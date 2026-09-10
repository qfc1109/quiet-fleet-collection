[CmdletBinding()]
param(
    [Alias('Host')]
    [string]$Server,
    [string]$User = 'root',
    [int]$SshPort = 22,
    [switch]$AllowDirtyWorktree,
    [string]$OutputDirectory,
    [switch]$Help
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($Help) {
    Write-Host 'Subsequent update:'
    Write-Host '  .\deploy\update.ps1 -Server 192.168.0.199 -User root -AllowDirtyWorktree'
    Write-Host ''
    Write-Host 'Builds the current worktree locally, uploads a new release, and invokes the server update script.'
    Write-Host 'The update never runs schema.sql or data.sql and never deletes database or storage files.'
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Server)) {
    throw '-Server is required unless -Help is used.'
}

. (Join-Path $PSScriptRoot 'lib\Build-QfcRelease.ps1')

foreach ($command in @('ssh.exe', 'scp.exe')) {
    Assert-QfcCommand -Name $command
}

$release = New-QfcRelease -AllowDirtyWorktree:$AllowDirtyWorktree -OutputDirectory $OutputDirectory
$identity = "$User@$Server"
$remoteDirectory = "/tmp/qfc-update-$($release.Version)"
$remoteArchive = "$remoteDirectory/release.tar.gz"

Write-Host "Uploading update for release $($release.Version)..."
& ssh.exe -p $SshPort $identity "mkdir -p $remoteDirectory"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to create remote directory $remoteDirectory."
}

& scp.exe -P $SshPort $release.Archive "$($identity):$remoteArchive"
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to upload the release archive.'
}

& scp.exe -P $SshPort (Join-Path $PSScriptRoot 'server\update.sh') "$($identity):$remoteDirectory/update.sh"
if ($LASTEXITCODE -ne 0) {
    throw 'Unable to upload the remote update script.'
}

Write-Host 'Running remote update. SSH may prompt for the account password again.'
& ssh.exe -p $SshPort $identity "install -m 0755 $remoteDirectory/update.sh /opt/qfc/bin/update.sh && bash /opt/qfc/bin/update.sh --release $remoteArchive"
if ($LASTEXITCODE -ne 0) {
    throw 'Remote update failed. The server-side script attempted to restore the previous release.'
}

Write-Host "Update succeeded. Main site: http://$Server/  Admin site: http://$($Server):8082/login"
