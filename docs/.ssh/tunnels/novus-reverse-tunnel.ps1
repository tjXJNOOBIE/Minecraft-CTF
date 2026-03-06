Set-StrictMode -Version Latest

param(
    [string]$Key = "$env:USERPROFILE\Documents\.ssh\NovusPriKey",
    [string]$Host = "ubuntu@146.235.232.128",
    [int]$RemoteWebPort = 8700,
    [int]$RemoteViewBase = 8701,
    [int]$ViewCount = 4,
    [int]$WebLocalPort = 7000,
    [int]$ViewBaseLocalPort = 8600,
    [string]$SshExe = "ssh.exe"
)

if ($ViewCount -lt 1) {
    $ViewCount = 4
}

$sshArgs = @(
    "-i", $Key,
    "-o", "StrictHostKeyChecking=no",
    "-o", "ExitOnForwardFailure=yes",
    "-o", "ServerAliveInterval=15",
    "-o", "ServerAliveCountMax=3",
    "-R", "${RemoteWebPort}:127.0.0.1:${WebLocalPort}"
)

for ($i = 0; $i -lt $ViewCount; $i++) {
    $localPort = $ViewBaseLocalPort + $i
    $remotePort = $RemoteViewBase + $i
    $sshArgs += "-R" , "${remotePort}:127.0.0.1:${localPort}"
}

$sshArgs += $Host

Write-Host "Starting reverse SSH tunnel to $Host (web $WebLocalPort->$RemoteWebPort, viewer ports $ViewBaseLocalPort+$ViewCount -> $RemoteViewBase+$ViewCount)"

& $SshExe @sshArgs
if ($LASTEXITCODE -ne 0) {
    throw "SSH exited with code $LASTEXITCODE"
}
