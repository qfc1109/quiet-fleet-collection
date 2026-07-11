[CmdletBinding()]
param(
    [switch]$SkipLogOrganize
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot 'logs'
$repoPattern = [regex]::Escape($repoRoot)

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
        [string]$ErrLog
    )

    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $OutLog `
        -RedirectStandardError $ErrLog `
        -WindowStyle Hidden `
        -PassThru

    Write-Host ("Started {0}: pid={1}" -f $Name, $process.Id)
    Write-Host ("  stdout: {0}" -f $OutLog)
    Write-Host ("  stderr: {0}" -f $ErrLog)

    return $process
}

function Stop-QfcProcessOnPort {
    param(
        [int]$Port,
        [string]$Name,
        [string]$CommandPattern
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter ("ProcessId={0}" -f $connection.OwningProcess) -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }

        $commandLine = $process.CommandLine
        if ($commandLine -notmatch $repoPattern -or $commandLine -notmatch $CommandPattern) {
            Write-Warning ("Port {0} is used by pid={1}, but it is not this {2} service. Skipping." -f $Port, $connection.OwningProcess, $Name)
            continue
        }

        Write-Host ("Stopping existing {0} on port {1}: pid={2}" -f $Name, $Port, $connection.OwningProcess)
        Stop-Process -Id $connection.OwningProcess -Force
        Start-Sleep -Seconds 1
    }
}

if (-not $SkipLogOrganize) {
    & (Join-Path $PSScriptRoot 'organize-logs.ps1') -LogRoot $logRoot
}

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
    -ErrLog (New-DatedLogPath -BaseName 'server-console-8081' -Suffix '.err.log')

$processes += Start-QfcProcess `
    -Name 'web' `
    -FilePath $npm `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', '5173') `
    -WorkingDirectory (Join-Path $repoRoot 'web') `
    -OutLog (New-DatedLogPath -BaseName 'web-dev-5173' -Suffix '.out.log') `
    -ErrLog (New-DatedLogPath -BaseName 'web-dev-5173' -Suffix '.err.log')

$processes += Start-QfcProcess `
    -Name 'admin-web' `
    -FilePath $npm `
    -ArgumentList @('run', 'dev', '--', '--host', '0.0.0.0', '--port', '5174') `
    -WorkingDirectory (Join-Path $repoRoot 'admin-web') `
    -OutLog (New-DatedLogPath -BaseName 'admin-web-dev-5174' -Suffix '.out.log') `
    -ErrLog (New-DatedLogPath -BaseName 'admin-web-dev-5174' -Suffix '.err.log')

Write-Host ''
Write-Host 'Development services are starting. Useful URLs:'
Write-Host '  backend:   http://127.0.0.1:8081'
Write-Host '  web:       http://127.0.0.1:5173'
Write-Host '  admin-web: http://127.0.0.1:5174'
Write-Host ''
Write-Host 'Backend rolling log: logs/qfc-server.log'
Write-Host 'Archived backend logs: logs/qfc-server-yyyy-MM-dd-index.log.gz'
