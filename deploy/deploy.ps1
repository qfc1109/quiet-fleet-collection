[CmdletBinding()]
param(
    [Alias('Host')]
    [string]$Server,
    [string]$User = 'root',
    [int]$SshPort = 22,
    [switch]$AllowDirtyWorktree,
    [switch]$ResumeAfterRuntimeInstall,
    [switch]$ResumeAfterImagePull,
    [switch]$ResumeAfterContainerStart,
    [string]$OutputDirectory,
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Help) {
    Write-Host 'Initial deployment:'
    Write-Host '  .\deploy\deploy.ps1 -Server 192.168.0.199 -User root -AllowDirtyWorktree'
    Write-Host ''
    Write-Host 'Builds the current worktree locally, uploads the release and server assets, then initializes Docker, MySQL 8, Redis, systemd, Nginx, and a fresh database.'
    Write-Host 'SSH prompts interactively for the account password.'
    Write-Host 'Use -ResumeAfterRuntimeInstall only to recover a package-install-only failure from this same bootstrap; it still refuses existing QFC state.'
    Write-Host 'Use -ResumeAfterImagePull only to recover an image-pull-only failure from this same bootstrap; it still refuses existing containers, volumes, and databases.'
    Write-Host 'Use -ResumeAfterContainerStart only to recover a post-container-start, pre-database-init failure from this same bootstrap.'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Server)) {
    throw '-Server is required unless -Help is used.'
}
if ((@($ResumeAfterRuntimeInstall, $ResumeAfterImagePull, $ResumeAfterContainerStart) | Where-Object { $_ }).Count -gt 1) {
    throw 'Choose only one recovery option.'
}

. (Join-Path $PSScriptRoot 'lib\Build-QfcRelease.ps1')

foreach ($command in @('ssh.exe', 'scp.exe')) {
    Assert-QfcCommand -Name $command
}

$release = New-QfcRelease -AllowDirtyWorktree:$AllowDirtyWorktree -OutputDirectory $OutputDirectory
$identity = "$User@$Server"
$remoteDirectory = "/tmp/qfc-deploy-$($release.Version)"
$remoteArchive = "$remoteDirectory/release.tar.gz"

Write-Host "Uploading first-deployment assets for release $($release.Version)..."
& ssh.exe -p $SshPort $identity "mkdir -p $remoteDirectory"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to create remote directory $remoteDirectory."
}

& scp.exe -P $SshPort $release.Archive "$($identity):$remoteArchive"
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to upload the release archive.'
}

& scp.exe -P $SshPort -r (Join-Path $PSScriptRoot 'server') "$($identity):$remoteDirectory/"
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to upload server deployment assets.'
}

Write-Host 'Running remote bootstrap. SSH may prompt for the account password again.'
$bootstrapCommand = "bash $remoteDirectory/server/bootstrap.sh --release $remoteArchive"
if ($ResumeAfterRuntimeInstall) {
    $bootstrapCommand += ' --resume-after-runtime-install'
}
if ($ResumeAfterImagePull) {
    $bootstrapCommand += ' --resume-after-image-pull'
}
if ($ResumeAfterContainerStart) {
    $bootstrapCommand += ' --resume-after-container-start'
}
& ssh.exe -p $SshPort $identity $bootstrapCommand
if ($LASTEXITCODE -ne 0) {
    throw 'Remote bootstrap failed. Existing user files were not removed automatically.'
}

Write-Host "Initial deployment succeeded. Main site: http://$Server/  Admin site: http://$($Server):8082/login"
