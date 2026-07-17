[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$repoPattern = [regex]::Escape($repoRoot)

$services = @(
    [PSCustomObject]@{ Name = 'server'; Port = 8081; Pattern = 'com\.qfc\.QfcApplication|spring-boot:run' },
    [PSCustomObject]@{ Name = 'web'; Port = 5173; Pattern = 'vite' },
    [PSCustomObject]@{ Name = 'admin-web'; Port = 5174; Pattern = 'vite' }
)

function Stop-QfcService {
    param([PSCustomObject]$Service)

    Write-Host ("Checking {0} on port {1}..." -f $Service.Name, $Service.Port)
    $connections = @(Get-NetTCPConnection -LocalPort $Service.Port -State Listen -ErrorAction SilentlyContinue)
    if ($connections.Count -eq 0) {
        Write-Host ("  {0} is not listening." -f $Service.Name)
        return
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        $process = Get-CimInstance Win32_Process -Filter ("ProcessId={0}" -f $processId) -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }

        if ($process.CommandLine -notmatch $repoPattern -or $process.CommandLine -notmatch $Service.Pattern) {
            throw ("Port {0} is used by pid={1}, but it is not this project's {2} service." -f $Service.Port, $processId, $Service.Name)
        }

        Write-Host ("  Stopping {0}: pid={1}" -f $Service.Name, $processId)
        Stop-Process -Id $processId -Force
    }

    $deadline = (Get-Date).AddSeconds(10)
    do {
        Start-Sleep -Milliseconds 250
        $remaining = Get-NetTCPConnection -LocalPort $Service.Port -State Listen -ErrorAction SilentlyContinue
    } while ($null -ne $remaining -and (Get-Date) -lt $deadline)

    if ($null -ne $remaining) {
        throw ("Timed out waiting for {0} on port {1} to stop." -f $Service.Name, $Service.Port)
    }

    Write-Host ("[OK] {0} stopped." -f $Service.Name)
}

foreach ($service in $services) {
    Stop-QfcService -Service $service
}

Start-Sleep -Seconds 1
$staleProcesses = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -match $repoPattern -and
    $_.CommandLine -match 'com\.qfc\.QfcApplication|spring-boot:run|node_modules[\\/]vite[\\/]|npm(?:\.cmd)?\s+run\s+dev'
})

foreach ($process in ($staleProcesses | Sort-Object ProcessId -Descending)) {
    if ($process.ProcessId -eq $PID) {
        continue
    }
    Write-Host ("Stopping remaining project process: {0} pid={1}" -f $process.Name, $process.ProcessId)
    Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
}

$remainingListeners = @(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue | Where-Object {
    $_.LocalPort -in @(8081, 5173, 5174)
})
if ($remainingListeners.Count -gt 0) {
    $ports = $remainingListeners | Select-Object -ExpandProperty LocalPort -Unique
    throw ("Some development ports are still listening: {0}" -f ($ports -join ', '))
}

Write-Host ''
Write-Host 'All Quiet Fleet Collection development services are stopped.'
