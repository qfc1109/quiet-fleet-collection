function Import-QfcMysqlEnvironment {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw ("MySQL environment file does not exist: {0}" -f $Path)
    }

    foreach ($line in Get-Content -Encoding UTF8 -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) {
            continue
        }

        $separatorIndex = $trimmed.IndexOf('=')
        if ($separatorIndex -lt 1) {
            throw ("Invalid MySQL environment entry: {0}" -f $line)
        }

        $name = $trimmed.Substring(0, $separatorIndex).Trim()
        $value = $trimmed.Substring($separatorIndex + 1).Trim()
        if ($name -notmatch '^QFC_MYSQL_[A-Z_]+$') {
            throw ("Unsupported MySQL environment variable: {0}" -f $name)
        }

        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }

    $requiredVariables = @(
        'QFC_MYSQL_HOST',
        'QFC_MYSQL_PORT',
        'QFC_MYSQL_USERNAME',
        'QFC_MYSQL_PASSWORD'
    )
    foreach ($name in $requiredVariables) {
        $value = [Environment]::GetEnvironmentVariable($name, 'Process')
        if ([string]::IsNullOrWhiteSpace($value)) {
            throw ("Required MySQL environment variable is missing: {0}" -f $name)
        }
    }

    $port = 0
    if (-not [int]::TryParse($env:QFC_MYSQL_PORT, [ref]$port) -or $port -lt 1 -or $port -gt 65535) {
        throw ("Invalid QFC_MYSQL_PORT: {0}" -f $env:QFC_MYSQL_PORT)
    }

    return [PSCustomObject]@{
        Host = $env:QFC_MYSQL_HOST
        Port = $port
        Username = $env:QFC_MYSQL_USERNAME
        Password = $env:QFC_MYSQL_PASSWORD
    }
}
