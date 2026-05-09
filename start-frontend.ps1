$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$clientDir = Join-Path $root "client"
$port = 5172

if (-not (Test-Path (Join-Path $clientDir "package.json"))) {
    Write-Error "client/package.json was not found. Run this script from the project root."
    exit 1
}

$listener = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "Frontend is already running: http://localhost:$port/" -ForegroundColor Green
    exit 0
}

if (-not (Test-Path (Join-Path $clientDir "node_modules"))) {
    Write-Host "Frontend dependencies not found. Installing..." -ForegroundColor Cyan
    & npm.cmd install --prefix $clientDir
}

Write-Host "Starting frontend: http://localhost:$port/" -ForegroundColor Green
& npm.cmd run dev --prefix $clientDir
