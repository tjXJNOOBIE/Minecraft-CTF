[CmdletBinding()]
param(
  [ValidatePattern('^[a-z0-9-]+$')]
  [string]$Mode = "headless",

  [Alias('Host')]
  [string]$ServerHost,

  [Alias('Port')]
  [int]$ServerPort = 0,

  [Alias('Bots')]
  [int]$BotCount = 0,

  [ValidateSet(50, 75, 100)]
  [int]$ArenaSize = 75,

  [string]$Seed = "",

  [switch]$Viewer
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ServerHost)) {
  $ServerHost = if ($env:CTF_SIM_HOST) { $env:CTF_SIM_HOST } else { "127.0.0.1" }
}

if ($ServerPort -le 0) {
  $ServerPort = if ($env:CTF_SIM_PORT) { [int]$env:CTF_SIM_PORT } else { 25565 }
}

if ($BotCount -le 0) {
  $BotCount = if ($env:CTF_SIM_BOTS) { [int]$env:CTF_SIM_BOTS } else { 12 }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$simDir = (Resolve-Path (Join-Path $scriptDir "..")).Path
$repoRoot = (Resolve-Path (Join-Path $simDir "..\\..")).Path
$modeEntry = Join-Path $repoRoot "tools\\sim\\src\\modes\\$Mode.ts"
Set-Location $simDir

if (-not (Test-Path $modeEntry)) {
  throw "Unknown simulation mode: $Mode"
}

if (-not (Test-Path (Join-Path $repoRoot "node_modules"))) {
  Push-Location $repoRoot
  try {
    npm ci
  } finally {
    Pop-Location
  }
}

$exitCode = 0
Push-Location $repoRoot
try {
  $env:CTF_HOST = $ServerHost
  $env:CTF_PORT = [string]$ServerPort
  if (-not $env:CTF_VIEW_HOST) {
    $env:CTF_VIEW_HOST = $ServerHost
  }
  if (-not $env:CTF_BOT_REGISTRY_PATH) {
    $env:CTF_BOT_REGISTRY_PATH = (Join-Path $repoRoot "bot-registry.json")
  }
  if (-not $env:CTF_DISABLE_VIEWERS) {
    $env:CTF_DISABLE_VIEWERS = if ($Viewer.IsPresent) { "0" } else { "1" }
  }
  if (-not $env:CTF_VIEW_COUNT -and $env:CTF_DISABLE_VIEWERS -eq "1") {
    $env:CTF_VIEW_COUNT = "0"
  }
  $args = @(
    "tsx",
    "tools/sim/src/modes/$Mode.ts",
    "--host", $ServerHost,
    "--port", $ServerPort,
    "--bots", $BotCount,
    "--arenaSize", $ArenaSize
  )
  if (-not [string]::IsNullOrWhiteSpace($Seed)) {
    $args += @("--seed", $Seed)
  }
  if ($Viewer.IsPresent) {
    $args += "--viewer"
  }
  & npx @args
  $exitCode = $LASTEXITCODE
} finally {
  Pop-Location
}
exit $exitCode
