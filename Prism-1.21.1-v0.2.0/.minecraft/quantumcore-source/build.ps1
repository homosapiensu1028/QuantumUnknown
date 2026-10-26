param(
    [string]$MinecraftDir = $env:INST_MC_DIR
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $MinecraftDir) { $MinecraftDir = Join-Path $ProjectRoot '..' }
$MinecraftDir = [System.IO.Path]::GetFullPath($MinecraftDir)
$ToolsDir = Join-Path $ProjectRoot '.tools'
New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null

Write-Host ''
Write-Host '=== QUANTUM UNKNOWN: first-run builder ===' -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot"
Write-Host "Minecraft folder: $MinecraftDir"

function Find-Jdk21 {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $javac = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javac) { $candidates += (Split-Path -Parent (Split-Path -Parent $javac.Source)) }
    $candidates = $candidates | Where-Object { $_ } | Select-Object -Unique
    foreach ($candidate in $candidates) {
        $compiler = Join-Path $candidate 'bin\javac.exe'
        $runtime = Join-Path $candidate 'bin\java.exe'
        if ((Test-Path $compiler) -and (Test-Path $runtime)) {
            $version = (& $compiler -version 2>&1 | Out-String)
            if ($version -match 'javac 21\.') { return $candidate }
        }
    }
    return $null
}

$JdkHome = Find-Jdk21
if (-not $JdkHome) {
    $JdkZip = Join-Path $ToolsDir 'temurin-jdk21-windows-x64.zip'
    $JdkRoot = Join-Path $ToolsDir 'jdk21'
    if (-not (Test-Path $JdkZip)) {
        Write-Host 'Downloading JDK 21 (first use only)...' -ForegroundColor Yellow
        Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile $JdkZip
    }
    New-Item -ItemType Directory -Force -Path $JdkRoot | Out-Null
    if (-not (Get-ChildItem $JdkRoot -Filter javac.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1)) {
        Expand-Archive -Path $JdkZip -DestinationPath $JdkRoot -Force
    }
    $JavacPath = Get-ChildItem $JdkRoot -Filter javac.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $JavacPath) { throw 'Could not find javac.exe in the downloaded JDK archive.' }
    $JdkHome = Split-Path -Parent (Split-Path -Parent $JavacPath.FullName)
}
$env:JAVA_HOME = $JdkHome
$env:PATH = (Join-Path $JdkHome 'bin') + ';' + $env:PATH
Write-Host "Using JDK: $JdkHome"

$GradleVersion = '8.14.3'
$GradleHome = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleExe = Join-Path $GradleHome 'bin\gradle.bat'
if (-not (Test-Path $GradleExe)) {
    $GradleZip = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
    if (-not (Test-Path $GradleZip)) {
        Write-Host "Downloading Gradle $GradleVersion (first use only)..." -ForegroundColor Yellow
        Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip" -OutFile $GradleZip
    }
    Expand-Archive -Path $GradleZip -DestinationPath $ToolsDir -Force
}
if (-not (Test-Path $GradleExe)) { throw "Gradle executable was not found: $GradleExe" }

Push-Location $ProjectRoot
try {
    Write-Host 'Building and running tests. The initial build downloads Minecraft/NeoForge dependencies...' -ForegroundColor Yellow
    & $GradleExe --no-daemon clean test build
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}

$JarPath = Join-Path $ProjectRoot 'build\libs\quantumcore-0.2.0.jar'
if (-not (Test-Path $JarPath)) {
    $candidate = Get-ChildItem (Join-Path $ProjectRoot 'build\libs') -Filter 'quantumcore-*.jar' -File | Where-Object { $_.Name -notmatch '-sources|-javadoc' } | Select-Object -First 1
    if (-not $candidate) { throw 'Build succeeded but no quantumcore JAR was created.' }
    $JarPath = $candidate.FullName
}
$ModsDir = Join-Path $MinecraftDir 'mods'
New-Item -ItemType Directory -Force -Path $ModsDir | Out-Null
Copy-Item -Path $JarPath -Destination (Join-Path $ModsDir 'quantumcore-0.2.0.jar') -Force
Write-Host 'Installed QuantumCore into the Prism instance mods folder.' -ForegroundColor Green

# JEI is included so the discovery-based JEI filter can be exercised by default.
$JeiVersion = '19.21.1.307'
$JeiJar = Join-Path $ModsDir "jei-1.21.1-neoforge-$JeiVersion.jar"
if (-not (Test-Path $JeiJar)) {
    try {
        Write-Host 'Downloading JEI for Minecraft 1.21.1 / NeoForge...' -ForegroundColor Yellow
        $JeiUrl = "https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge/$JeiVersion/jei-1.21.1-neoforge-$JeiVersion.jar"
        Invoke-WebRequest -Uri $JeiUrl -OutFile $JeiJar
        if ((Get-Item $JeiJar).Length -lt 100000) { Remove-Item $JeiJar -Force; throw 'Downloaded JEI file was unexpectedly small.' }
        Write-Host 'JEI installed.' -ForegroundColor Green
    } catch {
        Write-Warning "JEI could not be downloaded automatically. Core mod is installed, but JEI-specific filtering will not be visible until JEI is installed manually. Details: $_"
        if (Test-Path $JeiJar) { Remove-Item $JeiJar -Force }
    }
}
Write-Host 'First-run build complete. Minecraft will now start.' -ForegroundColor Green
Write-Host ''
exit 0
