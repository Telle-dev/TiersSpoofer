$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
Set-Location $PSScriptRoot

function Has-Java21($dir) {
    if (-not $dir -or -not (Test-Path "$dir\bin\javac.exe")) { return $false }
    $v = cmd /c "call `"$dir\bin\java.exe`" -version 2>&1"
    return ($v -join ' ') -match 'version "(2[1-9]|[3-9]\d)'
}

$jdk = $null
$local = Get-ChildItem .jdk -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($local -and (Has-Java21 $local.FullName)) { $jdk = $local.FullName }
elseif (Has-Java21 $env:JAVA_HOME) { $jdk = $env:JAVA_HOME }

if (-not $jdk) {
    Write-Host "Java 21 not found, downloading it (~190 MB)..."
    $zip = "$env:TEMP\jdk21.zip"
    Invoke-WebRequest "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile $zip -UseBasicParsing
    Expand-Archive $zip .jdk -Force
    Remove-Item $zip
    $jdk = (Get-ChildItem .jdk -Directory | Select-Object -First 1).FullName
}

$env:JAVA_HOME = $jdk
Write-Host "Building... (first time takes a few minutes)"
& .\gradlew.bat build --no-daemon
if ($LASTEXITCODE -ne 0) { exit 1 }

New-Item output -ItemType Directory -Force | Out-Null
Get-ChildItem build\libs\*.jar | Where-Object { $_.Name -notlike '*-sources.jar' } | Copy-Item -Destination output
explorer.exe output
