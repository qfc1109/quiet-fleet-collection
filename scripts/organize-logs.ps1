[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$LogRoot = (Join-Path (Split-Path -Parent $PSScriptRoot) 'logs')
)

$ErrorActionPreference = 'Stop'

function Test-DatedLogName {
    param([string]$FileName)

    return $FileName -match '-\d{4}-\d{2}-\d{2}(?:-\d+)?\.(?:out\.log|err\.log|log|log\.gz)$'
}

function Get-LogNameParts {
    param([string]$FileName)

    if ($FileName -match '^(?<Base>.+?)(?<Suffix>\.(?:out|err)\.log)$') {
        return @{
            Base = $Matches.Base
            Suffix = $Matches.Suffix
        }
    }

    if ($FileName -match '^(?<Base>.+?)(?<Suffix>\.log)$') {
        return @{
            Base = $Matches.Base
            Suffix = $Matches.Suffix
        }
    }

    return $null
}

function New-DatedLogPath {
    param(
        [string]$Directory,
        [string]$BaseName,
        [string]$DateText,
        [string]$Suffix
    )

    $index = 1
    do {
        $candidate = Join-Path $Directory ("{0}-{1}-{2}{3}" -f $BaseName, $DateText, $index, $Suffix)
        $index++
    } while (Test-Path -LiteralPath $candidate)

    return $candidate
}

if (-not (Test-Path -LiteralPath $LogRoot)) {
    New-Item -ItemType Directory -Path $LogRoot | Out-Null
    return
}

$logRootItem = Get-Item -LiteralPath $LogRoot
$activeLogNames = @(
    'qfc-server.log'
)

Get-ChildItem -LiteralPath $logRootItem.FullName -File |
    Where-Object { $_.Name -match '\.(?:out\.log|err\.log|log)$' } |
    Where-Object { $activeLogNames -notcontains $_.Name } |
    Where-Object { -not (Test-DatedLogName $_.Name) } |
    ForEach-Object {
        $parts = Get-LogNameParts $_.Name
        if ($null -eq $parts) {
            return
        }

        $dateText = $_.LastWriteTime.ToString('yyyy-MM-dd')
        $targetPath = New-DatedLogPath -Directory $logRootItem.FullName -BaseName $parts.Base -DateText $dateText -Suffix $parts.Suffix

        if ($PSCmdlet.ShouldProcess($_.FullName, "Move to $targetPath")) {
            try {
                Move-Item -LiteralPath $_.FullName -Destination $targetPath
                Write-Host ("Archived {0} -> {1}" -f $_.Name, (Split-Path -Leaf $targetPath))
            } catch {
                Write-Warning ("Could not archive {0}: {1}" -f $_.Name, $_.Exception.Message)
            }
        }
    }
