param([string]$ToolsDir=(Join-Path $PSScriptRoot 'tools'))
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
$ToolsDir=(Resolve-Path -LiteralPath $ToolsDir).Path
function Download([string]$Url,[string]$Name,[string]$Hash){$file=Join-Path $ToolsDir $Name;if(!(Test-Path -LiteralPath $file)){Invoke-WebRequest -Uri $Url -OutFile $file};if((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash -ne $Hash){throw "SHA-256 mismatch: $Name"};return $file}
$jdk=Download 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.20.1%2B1/OpenJDK17U-jdk_x64_windows_hotspot_17.0.20.1_1.zip' 'jdk.zip' 'e53a79c3c3d86865bd7e787903884331068e71321714ffd44f145785affc7cb0'
$platform=Download 'https://dl.google.com/android/repository/platform-35_r02.zip' 'platform.zip' '0988cacad01b38a18a47bac14a0695f246bc76c1b06c0eeb8eb0dc825ab0c8e0'
$buildTools=Download 'https://dl.google.com/android/repository/build-tools_r35_windows.zip' 'build-tools.zip' '5753c679a1b90bcf6fbc9945a2ce39dfb9e74f1df0831a1e1866ae5b594326f0'
Expand-Archive -LiteralPath $jdk -DestinationPath (Join-Path $ToolsDir 'jdk') -Force
Expand-Archive -LiteralPath $platform -DestinationPath (Join-Path $ToolsDir 'platform') -Force
Expand-Archive -LiteralPath $buildTools -DestinationPath (Join-Path $ToolsDir 'build-tools') -Force
Write-Output "Portable tools ready: $ToolsDir"
