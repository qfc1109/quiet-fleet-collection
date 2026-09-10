Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-QfcRepositoryRoot {
    $root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
    if (-not (Test-Path -LiteralPath (Join-Path $root 'server\pom.xml'))) {
        throw "QFC repository root could not be resolved from $PSScriptRoot."
    }
    return $root
}

function Assert-QfcCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    if ($null -eq (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command was not found: $Name"
    }
}

function Invoke-QfcCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory
    )

    Push-Location $WorkingDirectory
    try {
        & $FilePath @Arguments | Out-Host
        if ($LASTEXITCODE -ne 0) {
            throw "Command failed with exit code $($LASTEXITCODE): $FilePath $($Arguments -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

function Copy-QfcFrontendSource {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SourcePath,
        [Parameter(Mandatory = $true)]
        [string]$DestinationPath
    )

    New-Item -ItemType Directory -Path $DestinationPath -Force | Out-Null
    Get-ChildItem -LiteralPath $SourcePath -Force |
        Where-Object { $_.Name -notin @('node_modules', 'dist', 'dist-ssr', '.vite') } |
        ForEach-Object {
            Copy-Item -LiteralPath $_.FullName -Destination $DestinationPath -Recurse -Force
        }
}

function Get-QfcReleaseVersion {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositoryRoot,
        [Parameter(Mandatory = $true)]
        [bool]$HasDirtyWorktree
    )

    $shortSha = (& git -C $RepositoryRoot rev-parse --short HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($shortSha)) {
        throw 'Unable to determine the current Git revision.'
    }

    $timestamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
    if ($HasDirtyWorktree) {
        return "$timestamp-$shortSha-dirty"
    }
    return "$timestamp-$shortSha"
}

function New-QfcRelease {
    [CmdletBinding()]
    param(
        [string]$RepositoryRoot = (Get-QfcRepositoryRoot),
        [switch]$AllowDirtyWorktree,
        [string]$OutputDirectory
    )

    foreach ($command in @('git', 'mvn', 'npm', 'tar.exe')) {
        Assert-QfcCommand -Name $command
    }

    $statusLines = @(& git -C $RepositoryRoot status --porcelain)
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to read Git worktree status.'
    }
    $hasDirtyWorktree = $statusLines.Count -gt 0
    if ($hasDirtyWorktree -and -not $AllowDirtyWorktree) {
        $statusText = $statusLines -join [Environment]::NewLine
        throw ("The worktree contains uncommitted files. Re-run with -AllowDirtyWorktree to explicitly include them in the release." + [Environment]::NewLine + $statusText)
    }

    if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
        $OutputDirectory = Join-Path ([System.IO.Path]::GetTempPath()) 'qfc-deploy'
    }
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
    $OutputDirectory = (Resolve-Path $OutputDirectory).Path

    Write-Host 'Building QFC backend...'
    Invoke-QfcCommand -FilePath 'mvn' -Arguments @('-f', 'server\pom.xml', 'clean', 'package', '-B', '-ntp') -WorkingDirectory $RepositoryRoot

    $jar = Join-Path $RepositoryRoot 'server\target\quiet-fleet-collection-server-0.0.1-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Expected build output does not exist: $jar"
    }

    $version = Get-QfcReleaseVersion -RepositoryRoot $RepositoryRoot -HasDirtyWorktree $hasDirtyWorktree
    $stageRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("qfc-release-stage-" + [Guid]::NewGuid().ToString('N'))
    $releaseDirectoryName = "qfc-$version"
    $releaseDirectory = Join-Path $stageRoot $releaseDirectoryName
    $archive = Join-Path $OutputDirectory "qfc-release-$version.tar.gz"
    $archiveChecksum = "$archive.sha256"
    $frontendBuildRoot = Join-Path $stageRoot 'frontend-build'
    $mainFrontendBuild = Join-Path $frontendBuildRoot 'web'
    $adminFrontendBuild = Join-Path $frontendBuildRoot 'admin-web'

    try {
        Write-Host 'Installing and building QFC main web in an isolated temporary directory...'
        Copy-QfcFrontendSource -SourcePath (Join-Path $RepositoryRoot 'web') -DestinationPath $mainFrontendBuild
        Invoke-QfcCommand -FilePath 'npm' -Arguments @('--prefix', $mainFrontendBuild, 'ci') -WorkingDirectory $RepositoryRoot
        Invoke-QfcCommand -FilePath 'npm' -Arguments @('--prefix', $mainFrontendBuild, 'run', 'build') -WorkingDirectory $RepositoryRoot

        Write-Host 'Installing and building QFC admin web in an isolated temporary directory...'
        Copy-QfcFrontendSource -SourcePath (Join-Path $RepositoryRoot 'admin-web') -DestinationPath $adminFrontendBuild
        Invoke-QfcCommand -FilePath 'npm' -Arguments @('--prefix', $adminFrontendBuild, 'ci') -WorkingDirectory $RepositoryRoot
        Invoke-QfcCommand -FilePath 'npm' -Arguments @('--prefix', $adminFrontendBuild, 'run', 'build') -WorkingDirectory $RepositoryRoot

        $mainIndex = Join-Path $mainFrontendBuild 'dist\index.html'
        $adminIndex = Join-Path $adminFrontendBuild 'dist\index.html'
        foreach ($requiredPath in @($mainIndex, $adminIndex)) {
            if (-not (Test-Path -LiteralPath $requiredPath)) {
                throw "Expected build output does not exist: $requiredPath"
            }
        }

        New-Item -ItemType Directory -Path (Join-Path $releaseDirectory 'server') -Force | Out-Null
        New-Item -ItemType Directory -Path (Join-Path $releaseDirectory 'web') -Force | Out-Null
        New-Item -ItemType Directory -Path (Join-Path $releaseDirectory 'admin-web') -Force | Out-Null
        Copy-Item -LiteralPath $jar -Destination (Join-Path $releaseDirectory 'server\qfc-server.jar') -Force
        Copy-Item -Path (Join-Path $mainFrontendBuild 'dist\*') -Destination (Join-Path $releaseDirectory 'web') -Recurse -Force
        Copy-Item -Path (Join-Path $adminFrontendBuild 'dist\*') -Destination (Join-Path $releaseDirectory 'admin-web') -Recurse -Force
        Copy-Item -LiteralPath (Join-Path $RepositoryRoot 'server\src\main\resources\db') -Destination (Join-Path $releaseDirectory 'db') -Recurse -Force

        [System.IO.File]::WriteAllText(
            (Join-Path $releaseDirectory 'RELEASE_VERSION'),
            "$version$([Environment]::NewLine)",
            (New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false)
        )

        $manifestLines = Get-ChildItem -LiteralPath $releaseDirectory -Recurse -File |
            Sort-Object FullName |
            ForEach-Object {
                $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
                $relativePath = $_.FullName.Substring($releaseDirectory.Length + 1).Replace('\', '/')
                "$hash  $relativePath"
            }
        [System.IO.File]::WriteAllLines(
            (Join-Path $releaseDirectory 'MANIFEST.sha256'),
            [string[]]$manifestLines,
            (New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false)
        )

        & tar.exe -czf $archive -C $stageRoot $releaseDirectoryName
        if ($LASTEXITCODE -ne 0) {
            throw "tar.exe failed with exit code $LASTEXITCODE."
        }

        $archiveHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant()
        [System.IO.File]::WriteAllText(
            $archiveChecksum,
            "$archiveHash  $([System.IO.Path]::GetFileName($archive))$([Environment]::NewLine)",
            (New-Object -TypeName System.Text.UTF8Encoding -ArgumentList $false)
        )
    }
    finally {
        if (Test-Path -LiteralPath $stageRoot) {
            Remove-Item -LiteralPath $stageRoot -Recurse -Force
        }
    }

    return [PSCustomObject]@{
        Version = $version
        Archive = $archive
        ArchiveChecksum = $archiveChecksum
        HasDirtyWorktree = $hasDirtyWorktree
    }
}
