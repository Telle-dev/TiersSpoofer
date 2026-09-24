# Builds the mod: gets Java 21 if needed, runs gradle, copies the jar to output\.

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # makes Invoke-WebRequest much faster on PowerShell 5
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = $PSScriptRoot
Set-Location $Root

function Step($msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "    $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "    $msg" -ForegroundColor Yellow }
function Fail($msg) { Write-Host ""; Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

# Returns the major version of the JDK at $javaHome, or 0 if it isn't a usable JDK.
function Get-JdkMajor($javaHome) {
    if (-not $javaHome) { return 0 }
    $java  = Join-Path $javaHome 'bin\java.exe'
    $javac = Join-Path $javaHome 'bin\javac.exe'
    if (-not (Test-Path $java) -or -not (Test-Path $javac)) { return 0 }
    # "java -version" prints to stderr, which PowerShell 5 turns into errors when
    # redirected - so don't stop on them for this one call.
    $old = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { $out = (& $java -version 2>&1 | ForEach-Object { "$_" }) -join "`n" }
    catch { $out = '' }
    finally { $ErrorActionPreference = $old }
    if ($out -match 'version "(\d+)(\.(\d+))?') {
        $major = [int]$Matches[1]
        if ($major -eq 1 -and $Matches[3]) { $major = [int]$Matches[3] }  # old "1.8" style
        return $major
    }
    return 0
}

function Find-Jdk {
    $candidates = @()
    # 1) portable JDK downloaded by an earlier run
    $local = Join-Path $Root '.jdk'
    if (Test-Path $local) {
        $candidates += Get-ChildItem $local -Directory | ForEach-Object { $_.FullName }
    }
    # 2) JAVA_HOME
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    # 3) java on PATH
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath) { $candidates += (Split-Path (Split-Path $onPath.Source -Parent) -Parent) }
    # 4) common install locations
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java",
                        "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu",
                        "$env:ProgramFiles\BellSoft", "$env:ProgramFiles\Amazon Corretto")) {
        if (Test-Path $base) {
            $candidates += Get-ChildItem $base -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
        }
    }
    foreach ($c in $candidates) {
        if ((Get-JdkMajor $c) -ge 21) { return $c }
    }
    return $null
}

function Install-PortableJdk {
    $dest = Join-Path $Root '.jdk'
    $zip  = Join-Path $env:TEMP 'tierspoofer-jdk21.zip'
    $arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
    $url  = "https://api.adoptium.net/v3/binary/latest/21/ga/windows/$arch/jdk/hotspot/normal/eclipse?project=jdk"

    Write-Host "    Downloading Temurin JDK 21 ($arch, ~190 MB)..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    } catch {
        Warn "Download failed: $($_.Exception.Message)"
        return $null
    }
    Write-Host "    Extracting..."
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    New-Item -ItemType Directory -Path $dest | Out-Null
    Expand-Archive -Path $zip -DestinationPath $dest -Force
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    $jdk = Get-ChildItem $dest -Directory | Select-Object -First 1
    if ($jdk -and (Get-JdkMajor $jdk.FullName) -ge 21) { return $jdk.FullName }
    return $null
}

function Install-JdkWithWinget {
    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if (-not $winget) { return $null }
    Write-Host "    Trying winget (EclipseAdoptium.Temurin.21.JDK)..."
    & winget install --id EclipseAdoptium.Temurin.21.JDK -e --silent --accept-package-agreements --accept-source-agreements | Out-Host
    return Find-Jdk
}

Write-Host "TierSpoofer build" -ForegroundColor Cyan

# ---------------------------------------------------------------- Java
Step "Looking for Java 21..."
$jdk = Find-Jdk
if ($jdk) {
    Ok "Using JDK: $jdk"
} else {
    Warn "No Java 21 JDK found - getting one."
    $jdk = Install-PortableJdk
    if (-not $jdk) { $jdk = Install-JdkWithWinget }
    if (-not $jdk) {
        Fail "Could not get Java 21. Install 'Temurin 21 JDK' from https://adoptium.net and run Build.bat again."
    }
    Ok "Using JDK: $jdk"
}
$env:JAVA_HOME = $jdk
$env:Path = (Join-Path $jdk 'bin') + ';' + $env:Path

# ---------------------------------------------------------------- Build
Step "Building (the first build downloads Minecraft + Fabric, ~5-10 minutes)..."
$gradlew = Join-Path $Root 'gradlew.bat'
if (-not (Test-Path $gradlew)) { Fail "gradlew.bat is missing - re-extract the project zip." }

& $gradlew build --no-daemon --console=plain
if ($LASTEXITCODE -ne 0) {
    Fail "Gradle build failed (exit code $LASTEXITCODE). Scroll up for the first error, or send it over."
}

# ---------------------------------------------------------------- Output
Step "Collecting the jar..."
$jar = Get-ChildItem (Join-Path $Root 'build\libs') -Filter '*.jar' |
       Where-Object { $_.Name -notmatch '-(sources|dev)\.jar$' } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { Fail "Build finished but no jar was found in build\libs." }

$outDir = Join-Path $Root 'output'
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
Copy-Item $jar.FullName $outDir -Force
$outJar = Join-Path $outDir $jar.Name
Ok "Built: $outJar"

# ---------------------------------------------------------------- Mods folder
$mods = Join-Path $env:APPDATA '.minecraft\mods'
if (Test-Path (Join-Path $env:APPDATA '.minecraft')) {
    $answer = Read-Host "`n    Copy it into $mods ? (Y/n)"
    if ($answer -eq '' -or $answer -match '^[yYjJ]') {
        New-Item -ItemType Directory -Path $mods -Force | Out-Null
        # Remove older TierSpoofer jars so two versions don't load at once.
        Get-ChildItem $mods -Filter 'tierspoofer-*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -ne $jar.Name } | Remove-Item -Force
        Copy-Item $outJar $mods -Force
        Ok "Copied to $mods"
        if (-not (Get-ChildItem $mods -Filter 'fabric-api*.jar' -ErrorAction SilentlyContinue)) {
            Warn "Fabric API is not in your mods folder - TierSpoofer needs it:"
            Warn "https://modrinth.com/mod/fabric-api (pick the 1.21.11 version)"
        }
    }
} else {
    Warn "No .minecraft folder found - put the jar into your mods folder yourself."
}

Write-Host ""
Write-Host "Done! Needs Fabric Loader 0.18.1+ and Fabric API for Minecraft 1.21.11." -ForegroundColor Green
Start-Process explorer.exe $outDir
exit 0
