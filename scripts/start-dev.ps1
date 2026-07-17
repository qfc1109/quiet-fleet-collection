[CmdletBinding()]
param(
    [switch]$SkipLogOrganize
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot 'logs'
$repoPattern = [regex]::Escape($repoRoot)
$applicationConfig = Join-Path $repoRoot 'config\application.yml'
$mysqlEnvironment = Join-Path $repoRoot 'config\mysql.env'

. (Join-Path $PSScriptRoot 'mysql-env.ps1')

function Resolve-Tool {
    param([string]$Name)

    $command = Get-Command @("$Name.cmd", "$Name.exe", $Name) -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($null -eq $command) {
        $command = Get-Command $Name -ErrorAction Stop
    }

    return $command.Source
}

function New-DatedLogPath {
    param(
        [string]$BaseName,
        [string]$Suffix
    )

    if (-not (Test-Path -LiteralPath $logRoot)) {
        New-Item -ItemType Directory -Path $logRoot | Out-Null
    }

    $dateText = (Get-Date).ToString('yyyy-MM-dd')
    $index = 1
    do {
        $candidate = Join-Path $logRoot ("{0}-{1}-{2}{3}" -f $BaseName, $dateText, $index, $Suffix)
        $index++
    } while (Test-Path -LiteralPath $candidate)

    return $candidate
}

function Start-QfcProcess {
    param(
        [string]$Name,
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$WorkingDirectory,
        [string]$OutLog,
        [string]$ErrLog,
        [int]$Port
    )

    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $OutLog `
        -RedirectStandardError $ErrLog `
        -WindowStyle Hidden `
        -PassThru

    Write-Host ("Starting {0}: pid={1}" -f $Name, $process.Id)
    Write-Host ("  stdout: {0}" -f $OutLog)
    Write-Host ("  stderr: {0}" -f $ErrLog)

    return [PSCustomObject]@{
        Name = $Name
        Port = $Port
        Process = $process
        OutLog = $OutLog
        ErrLog = $ErrLog
    }
}

function Stop-QfcProcessOnPort {
    param(
        [int]$Port,
        [string]$Name,
        [string]$CommandPattern
    )

    Write-Host ("Checking existing {0} on port {1}..." -f $Name, $Port)
    $connections = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
    if ($connections.Count -eq 0) {
        Write-Host ("  {0} is not running." -f $Name)
        return
    }

    $processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $processIds) {
        $process = Get-CimInstance Win32_Process -Filter ("ProcessId={0}" -f $processId) -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }

        $commandLine = $process.CommandLine
        if ($commandLine -notmatch $repoPattern -or $commandLine -notmatch $CommandPattern) {
            throw ("Port {0} is used by pid={1}, but it is not this {2} service." -f $Port, $processId, $Name)
        }

        Write-Host ("  Stopping {0}: pid={1}" -f $Name, $processId)
        Stop-Process -Id $processId -Force
    }

    $deadline = (Get-Date).AddSeconds(10)
    do {
        Start-Sleep -Milliseconds 250
        $remaining = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    } while ($null -ne $remaining -and (Get-Date) -lt $deadline)

    if ($null -ne $remaining) {
        throw ("Timed out waiting for {0} on port {1} to stop." -f $Name, $Port)
    }

    Write-Host ("  {0} stopped." -f $Name)
}

function Wait-QfcProcessReady {
    param(
        [PSCustomObject]$Service,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if ($Service.Process.HasExited) {
            throw ("{0} exited before port {1} became ready. Check stderr: {2}" -f $Service.Name, $Service.Port, $Service.ErrLog)
        }

        $listener = Get-NetTCPConnection -LocalPort $Service.Port -State Listen -ErrorAction SilentlyContinue
        if ($null -ne $listener) {
            Write-Host ("[OK] {0} is running on http://127.0.0.1:{1}" -f $Service.Name, $Service.Port)
            return
        }

        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)

    throw ("Timed out waiting for {0} on port {1}. Check stdout: {2}; stderr: {3}" -f $Service.Name, $Service.Port, $Service.OutLog, $Service.ErrLog)
}

if (-not $SkipLogOrganize) {
    & (Join-Path $PSScriptRoot 'organize-logs.ps1') -LogRoot $logRoot
}

$mysqlSettings = Import-QfcMysqlEnvironment -Path $mysqlEnvironment
if (-not (Test-Path -LiteralPath $applicationConfig)) {
    throw ("Application configuration does not exist: {0}. Run scripts\setup-dev.bat first." -f $applicationConfig)
}
Write-Host ("[OK] Loaded MySQL configuration: {0}:{1}" -f $mysqlSettings.Host, $mysqlSettings.Port)

$mvn = Resolve-Tool 'mvn'
$npm = Resolve-Tool 'npm'

Stop-QfcProcessOnPort -Port 8081 -Name 'server' -CommandPattern 'com\.qfc\.QfcApplication'
Stop-QfcProcessOnPort -Port 5173 -Name 'web' -CommandPattern 'vite'
Stop-QfcProcessOnPort -Port 5174 -Name 'admin-web' -CommandPattern 'vite'

$processes = @()

$processes += Start-QfcProcess `
    -Name 'server' `
    -FilePath $mvn `
    -ArgumentList @(
        '-Dspring-boot.run.arguments=--spring.config.additional-location=file:../config/',
        '-Dspring-boot.run.jvmArguments=-DLOG_HOME=../logs',
        'spring-boot:run',
        '-ntp'
    ) `
    -WorkingDirectory (Join-Path $repoRoot 'server') `
    -OutLog (New-DatedLogPath -BaseName 'server-console-8081' -Suffix '.out.log') `
    -ErrLog (New-DatedLogPath -BaseName 'server-console-8081' -Suffix '.err.log') `
    -Port 8081

$processes += Start-QfcProcess `
    -Name 'web' `
    -FilePath $npm `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', '5173') `
    -WorkingDirectory (Join-Path $repoRoot 'web') `
    -OutLog (New-DatedLogPath -BaseName 'web-dev-5173' -Suffix '.out.log') `
    -ErrLog (New-DatedLogPath -BaseName 'web-dev-5173' -Suffix '.err.log') `
    -Port 5173

$processes += Start-QfcProcess `
    -Name 'admin-web' `
    -FilePath $npm `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', '5174') `
    -WorkingDirectory (Join-Path $repoRoot 'admin-web') `
    -OutLog (New-DatedLogPath -BaseName 'admin-web-dev-5174' -Suffix '.out.log') `
    -ErrLog (New-DatedLogPath -BaseName 'admin-web-dev-5174' -Suffix '.err.log') `
    -Port 5174

Write-Host ''
Write-Host 'Waiting for development services to become ready...'
foreach ($service in $processes) {
    Wait-QfcProcessReady -Service $service
}

try {
    $healthResponse = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8081/api/public/projects' -TimeoutSec 10
    if ($healthResponse.StatusCode -ne 200) {
        throw ("Unexpected HTTP status: {0}" -f $healthResponse.StatusCode)
    }
    Write-Host '[OK] Backend database API health check passed.'
}
catch {
    throw ("Backend database API health check failed: {0}" -f $_.Exception.Message)
}

Write-Host ''
Write-Host 'Development services restarted successfully. Useful URLs:'
Write-Host '  backend:   http://127.0.0.1:8081'
Write-Host '  web:       http://127.0.0.1:5173'
Write-Host '  admin-web: http://127.0.0.1:5174'
Write-Host ''
Write-Host 'Backend rolling log: logs/qfc-server.log'
Write-Host 'Archived backend logs: logs/qfc-server-yyyy-MM-dd-index.log.gz'
