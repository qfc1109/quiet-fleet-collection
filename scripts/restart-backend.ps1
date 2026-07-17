[CmdletBinding()]
param(
    [switch]$SkipLogOrganize
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$logRoot = Join-Path $repoRoot 'logs'
$serverRoot = Join-Path $repoRoot 'server'
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

function Stop-QfcBackendOnPort {
    param([int]$Port)

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter ("ProcessId={0}" -f $connection.OwningProcess) -ErrorAction SilentlyContinue
        if ($null -eq $process) {
            continue
        }

        $commandLine = $process.CommandLine
        if ($commandLine -notmatch $repoPattern -or $commandLine -notmatch 'com\.qfc\.QfcApplication') {
            Write-Warning ("Port {0} is used by pid={1}, but it is not this QFC backend. Skipping." -f $Port, $connection.OwningProcess)
            continue
        }

        Write-Host ("Stopping existing QFC backend on port {0}: pid={1}" -f $Port, $connection.OwningProcess)
        Stop-Process -Id $connection.OwningProcess -Force
        Start-Sleep -Seconds 2
    }
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

Stop-QfcBackendOnPort -Port 8081

$outLog = New-DatedLogPath -BaseName 'server-console-8081' -Suffix '.out.log'
$errLog = New-DatedLogPath -BaseName 'server-console-8081' -Suffix '.err.log'

$process = Start-Process `
    -FilePath $mvn `
    -ArgumentList @(
        '-Dspring-boot.run.arguments=--spring.config.additional-location=file:../config/',
        '-Dspring-boot.run.jvmArguments=-DLOG_HOME=../logs',
        'spring-boot:run',
        '-ntp'
    ) `
    -WorkingDirectory $serverRoot `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden `
    -PassThru

Write-Host ("Started backend: pid={0}" -f $process.Id)
Write-Host ("  stdout: {0}" -f $outLog)
Write-Host ("  stderr: {0}" -f $errLog)
Write-Host 'Backend URL: http://127.0.0.1:8081'
