[CmdletBinding()]
param(
    [switch]$SkipNpmInstall,
    [switch]$SkipDatabaseInit
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$configRoot = Join-Path $repoRoot 'config'
$applicationConfig = Join-Path $configRoot 'application.yml'
$applicationConfigExample = Join-Path $configRoot 'application.yml.example'
$mysqlEnvironment = Join-Path $configRoot 'mysql.env'
$schemaSql = Join-Path $repoRoot 'server\src\main\resources\db\schema.sql'
$dataSql = Join-Path $repoRoot 'server\src\main\resources\db\data.sql'

. (Join-Path $PSScriptRoot 'mysql-env.ps1')

function Resolve-Tool {
    param([string]$Name)

    $command = Get-Command @("$Name.cmd", "$Name.exe", $Name) -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1

    if ($null -eq $command) {
        throw ("Required tool is not available: {0}" -f $Name)
    }

    Write-Host ("[OK] {0}: {1}" -f $Name, $command.Source)
    return $command.Source
}

function Invoke-NativeCommand {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw ("{0} failed with exit code {1}." -f $Name, $LASTEXITCODE)
    }
}

function Read-DefaultValue {
    param(
        [string]$Prompt,
        [string]$DefaultValue
    )

    $value = Read-Host ("{0} [{1}]" -f $Prompt, $DefaultValue)
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value.Trim()
}

function ConvertFrom-SecureValue {
    param([Security.SecureString]$SecureValue)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function New-MysqlEnvironmentConfig {
    param(
        [string]$HostName,
        [int]$Port,
        [string]$UserName,
        [string]$Password
    )

    if ($HostName.Contains("`r") -or $HostName.Contains("`n") -or
        $UserName.Contains("`r") -or $UserName.Contains("`n") -or
        $Password.Contains("`r") -or $Password.Contains("`n")) {
        throw 'MySQL configuration values cannot contain line breaks.'
    }

    $content = @"
QFC_MYSQL_HOST=$HostName
QFC_MYSQL_PORT=$Port
QFC_MYSQL_USERNAME=$UserName
QFC_MYSQL_PASSWORD=$Password
"@
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($mysqlEnvironment, $content, $utf8WithoutBom)
    Write-Host ("[OK] Wrote MySQL environment configuration: {0}" -f $mysqlEnvironment)
}

function Read-MysqlEnvironmentConfig {
    $dbHost = Read-DefaultValue -Prompt 'MySQL host' -DefaultValue '127.0.0.1'
    $dbPortText = Read-DefaultValue -Prompt 'MySQL port' -DefaultValue '3306'
    $dbPort = 0
    if (-not [int]::TryParse($dbPortText, [ref]$dbPort) -or $dbPort -lt 1 -or $dbPort -gt 65535) {
        throw ("Invalid MySQL port: {0}" -f $dbPortText)
    }
    $dbUser = Read-DefaultValue -Prompt 'MySQL user' -DefaultValue 'root'
    $securePassword = Read-Host 'MySQL password' -AsSecureString
    $dbPassword = ConvertFrom-SecureValue $securePassword
    New-MysqlEnvironmentConfig -HostName $dbHost -Port $dbPort -UserName $dbUser -Password $dbPassword
}

function Invoke-MySqlQuery {
    param(
        [string]$MySql,
        [PSCustomObject]$Settings,
        [string]$Query
    )

    $previousMysqlPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $Settings.Password
        $output = @(& $MySql --no-defaults "--host=$($Settings.Host)" "--port=$($Settings.Port)" "--user=$($Settings.Username)" --connect-timeout=5 --batch --skip-column-names -e $Query)
        if ($LASTEXITCODE -ne 0) {
            throw ("MySQL query failed with exit code {0}." -f $LASTEXITCODE)
        }
        return $output
    }
    finally {
        if ($null -eq $previousMysqlPassword) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
        else {
            $env:MYSQL_PWD = $previousMysqlPassword
        }
    }
}

function Invoke-MySqlScript {
    param(
        [string]$MySql,
        [PSCustomObject]$Settings,
        [string]$ScriptPath
    )

    $previousMysqlPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $Settings.Password
        $process = Start-Process `
            -FilePath $MySql `
            -ArgumentList @(
                '--no-defaults',
                "--host=$($Settings.Host)",
                "--port=$($Settings.Port)",
                "--user=$($Settings.Username)",
                '--default-character-set=utf8mb4'
            ) `
            -RedirectStandardInput $ScriptPath `
            -NoNewWindow `
            -Wait `
            -PassThru
        if ($process.ExitCode -ne 0) {
            throw ("MySQL script failed: {0}" -f $ScriptPath)
        }
    }
    finally {
        if ($null -eq $previousMysqlPassword) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
        else {
            $env:MYSQL_PWD = $previousMysqlPassword
        }
    }
}

function Assert-SeedDataEncoding {
    param(
        [string]$MySql,
        [PSCustomObject]$Settings
    )

    $expectedSiteProfile = 'E8BDBBE5B886E7AEA1E79086E59198' + "`t" + 'E8BDBBE5B886E99B86E7ACACE4B880E78988E7BD91E7AB99E8B4A6E58FB7'
    $siteProfile = @(Invoke-MySqlQuery -MySql $MySql -Settings $Settings -Query "SELECT HEX(display_name), HEX(bio) FROM qfc_site.site_user WHERE username = 'admin';")
    if ($siteProfile.Count -ne 1 -or $siteProfile[0] -ne $expectedSiteProfile) {
        throw 'Seed data encoding verification failed for qfc_site.site_user admin profile.'
    }

    $expectedAdminProfile = 'E8BDBBE5B886E7AEA1E79086E59198' + "`t" + 'E8BDBBE5B886E99B86E7ACACE4B880E78988E7AEA1E79086E59198E8B4A6E58FB7'
    $adminProfile = @(Invoke-MySqlQuery -MySql $MySql -Settings $Settings -Query "SELECT HEX(display_name), HEX(bio) FROM qfc_admin.admin_user WHERE username = 'admin';")
    if ($adminProfile.Count -ne 1 -or $adminProfile[0] -ne $expectedAdminProfile) {
        throw 'Seed data encoding verification failed for qfc_admin.admin_user admin profile.'
    }

    $corruptedSeedCount = @(Invoke-MySqlQuery -MySql $MySql -Settings $Settings -Query "SELECT (SELECT COUNT(*) FROM qfc_admin.permission WHERE name LIKE '%?%' OR module LIKE '%?%' OR description LIKE '%?%') + (SELECT COUNT(*) FROM qfc_admin.admin_role WHERE name LIKE '%?%' OR description LIKE '%?%');")
    if ($corruptedSeedCount.Count -ne 1 -or $corruptedSeedCount[0] -ne '0') {
        throw 'Seed data encoding verification found corrupted permission or role text.'
    }

    Write-Host '[OK] Seed data UTF-8 encoding verified.'
}

Write-Host 'Checking required development tools...'
$java = Resolve-Tool 'java'
$mvn = Resolve-Tool 'mvn'
$node = Resolve-Tool 'node'
$npm = Resolve-Tool 'npm'
$mysql = Resolve-Tool 'mysql'

Write-Host ''
Write-Host 'Creating runtime directories...'
foreach ($directory in @('logs', 'storage')) {
    $path = Join-Path $repoRoot $directory
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
    }
    Write-Host ("[OK] {0}" -f $path)
}

if (-not $SkipNpmInstall) {
    Write-Host ''
    Write-Host 'Installing web dependencies...'
    Invoke-NativeCommand -Name 'web npm install' -Command {
        & $npm --prefix (Join-Path $repoRoot 'web') install
    }

    Write-Host ''
    Write-Host 'Installing admin-web dependencies...'
    Invoke-NativeCommand -Name 'admin-web npm install' -Command {
        & $npm --prefix (Join-Path $repoRoot 'admin-web') install
    }
}
else {
    Write-Host '[SKIP] npm dependency installation'
}

Write-Host ''
Write-Host 'Checking application configuration...'
if (-not (Test-Path -LiteralPath $applicationConfig)) {
    Copy-Item -LiteralPath $applicationConfigExample -Destination $applicationConfig
    Write-Host ("[OK] Created application configuration: {0}" -f $applicationConfig)
}
else {
    Write-Host ("[OK] {0}" -f $applicationConfig)
}

if (-not (Test-Path -LiteralPath $mysqlEnvironment)) {
    Write-Host 'MySQL configuration is missing. Enter the target MySQL credentials.'
    Read-MysqlEnvironmentConfig
}

$mysqlSettings = $null
while ($null -eq $mysqlSettings) {
    try {
        $candidateSettings = Import-QfcMysqlEnvironment -Path $mysqlEnvironment
        Invoke-MySqlQuery -MySql $mysql -Settings $candidateSettings -Query 'SELECT 1;' | Out-Null
        $mysqlSettings = $candidateSettings
        Write-Host ("[OK] MySQL connection succeeded: {0}:{1}" -f $mysqlSettings.Host, $mysqlSettings.Port)
    }
    catch {
        Write-Warning $_.Exception.Message
        $retry = Read-Host 'Re-enter the MySQL configuration? [Y/n]'
        if (-not [string]::IsNullOrWhiteSpace($retry) -and $retry -notmatch '^(?i)y(?:es)?$') {
            throw ("Unable to connect to MySQL. Check {0}." -f $mysqlEnvironment)
        }
        Read-MysqlEnvironmentConfig
    }
}

if (-not $SkipDatabaseInit) {
    $expectedSchemas = @('qfc_site', 'qfc_site_log', 'qfc_admin', 'qfc_admin_log')
    $expectedTables = @(
        'qfc_site.site_user',
        'qfc_site.project',
        'qfc_site.project_file',
        'qfc_site.project_issue',
        'qfc_site.site_feedback',
        'qfc_site_log.site_login_log',
        'qfc_site_log.site_operation_log',
        'qfc_admin.admin_user',
        'qfc_admin.admin_role',
        'qfc_admin.permission',
        'qfc_admin.admin_user_role',
        'qfc_admin.role_permission',
        'qfc_admin_log.admin_login_log',
        'qfc_admin_log.admin_operation_log',
        'qfc_admin_log.admin_console_log'
    )

    $schemaQuery = "SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME IN ('" + ($expectedSchemas -join "','") + "');"
    $existingSchemas = @(Invoke-MySqlQuery -MySql $mysql -Settings $mysqlSettings -Query $schemaQuery)

    $tableQuery = "SELECT CONCAT(TABLE_SCHEMA, '.', TABLE_NAME) FROM information_schema.TABLES WHERE CONCAT(TABLE_SCHEMA, '.', TABLE_NAME) IN ('" + ($expectedTables -join "','") + "');"
    $existingTables = @(Invoke-MySqlQuery -MySql $mysql -Settings $mysqlSettings -Query $tableQuery)

    $missingSchemas = @($expectedSchemas | Where-Object { $_ -notin $existingSchemas })
    $missingTables = @($expectedTables | Where-Object { $_ -notin $existingTables })

    if ($missingSchemas.Count -eq 0 -and $missingTables.Count -eq 0) {
        Write-Host '[OK] Project databases and required tables already exist. Database initialization skipped.'
    }
    else {
        if ($missingSchemas.Count -gt 0) {
            Write-Host ("Missing databases: {0}" -f ($missingSchemas -join ', '))
        }
        if ($missingTables.Count -gt 0) {
            Write-Host ("Missing tables: {0}" -f ($missingTables -join ', '))
        }
        Write-Host 'The schema and seed scripts will create missing objects and update built-in development accounts.'
        $confirmation = Read-Host 'Initialize the database now? [Y/n]'
        if ([string]::IsNullOrWhiteSpace($confirmation) -or $confirmation -match '^(?i)y(?:es)?$') {
            Invoke-MySqlScript -MySql $mysql -Settings $mysqlSettings -ScriptPath $schemaSql
            Invoke-MySqlScript -MySql $mysql -Settings $mysqlSettings -ScriptPath $dataSql
            Assert-SeedDataEncoding -MySql $mysql -Settings $mysqlSettings
            Write-Host '[OK] Database initialized.'
        }
        else {
            Write-Host '[SKIP] Database initialization declined.'
        }
    }
}
else {
    Write-Host '[SKIP] database initialization'
}

Write-Host ''
Write-Host 'Development environment setup completed.'
Write-Host 'Run scripts\start-dev.bat to restart all development services.'
