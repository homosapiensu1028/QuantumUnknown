$ErrorActionPreference = 'Stop'
$McDir = $env:INST_MC_DIR
if (-not $McDir) { $McDir = $PSScriptRoot }
$McDir = [System.IO.Path]::GetFullPath($McDir)
$ProjectRoot = Join-Path $McDir 'quantumcore-source'
$ModJar = Join-Path $McDir 'mods\quantumcore-0.2.0.jar'
$JeiJar = Join-Path $McDir 'mods\jei-1.21.1-neoforge-19.21.1.307.jar'

if ((Test-Path $ModJar) -and (Test-Path $JeiJar)) {
    Write-Host 'Quantum Unknown and JEI are already installed; skipping first-run build.'
    exit 0
}
if (-not (Test-Path (Join-Path $ProjectRoot 'build.ps1'))) {
    Write-Error "Project source was not found: $ProjectRoot"
    exit 2
}
& (Join-Path $ProjectRoot 'build.ps1') -MinecraftDir $McDir
if ($LASTEXITCODE -and $LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (-not (Test-Path $ModJar)) {
    Write-Error 'QuantumCore JAR was not installed; see the build output above.'
    exit 3
}
exit 0
