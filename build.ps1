param([Parameter(Mandatory=$true)][string]$ToolsDir)
$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
$ToolsDir=(Resolve-Path -LiteralPath $ToolsDir).Path
$env:JAVA_HOME=(Get-ChildItem -LiteralPath (Join-Path $ToolsDir 'jdk') -Directory | Select-Object -First 1).FullName
$bin=Join-Path $env:JAVA_HOME 'bin'
$bt=Join-Path $ToolsDir 'build-tools\android-15'
$android=Join-Path $ToolsDir 'platform\android-35\android.jar'
function Run([string]$Program,[string[]]$Arguments){& $Program @Arguments;if($LASTEXITCODE -ne 0){throw "Command failed: $Program"}}
$build=Join-Path $PSScriptRoot ('build\'+(Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
New-Item -ItemType Directory -Force -Path $build,"$build\classes","$build\dex","$build\tests","$build\storage-tests","$build\tmp",'artifacts','signing' | Out-Null
Run (Join-Path $bin 'javac.exe') @('--release','8','-d',"$build\tests",'app\src\main\java\com\kev\fortniteautobot\TriggerGate.java','app\src\main\java\com\kev\fortniteautobot\MatchMath.java','app\src\main\java\com\kev\fortniteautobot\BackupArchive.java','app\src\main\java\com\kev\fortniteautobot\PreviewGeometry.java','app\src\main\java\com\kev\fortniteautobot\VariantPolicy.java','app\src\main\java\com\kev\fortniteautobot\DebugGeometry.java','app\src\main\java\com\kev\fortniteautobot\TapCoordinates.java','tests\CoreTest.java','tests\BackupArchiveTest.java')
Run (Join-Path $bin 'java.exe') @('-cp',"$build\tests",'com.kev.fortniteautobot.CoreTest')
Run (Join-Path $bin 'java.exe') @("-Djava.io.tmpdir=$build\tmp",'-cp',"$build\tests",'com.kev.fortniteautobot.BackupArchiveTest')
$json=(Resolve-Path 'tests\lib\json-20240303.jar').Path
if((Get-FileHash -LiteralPath $json -Algorithm SHA256).Hash -ne '3CF6CD6892E32E2B4C1C39E0F52F5248A2F5B37646FDFBB79A66B46B618414ED'){throw 'Storage test dependency checksum mismatch'}
$testStubs=@(Get-ChildItem tests\storage-stubs -Recurse -Filter *.java | ForEach-Object FullName)
Run (Join-Path $bin 'javac.exe') (@('--release','8','-cp',$json,'-d',"$build\storage-tests",'app\src\main\java\com\kev\fortniteautobot\RuleStore.java','app\src\main\java\com\kev\fortniteautobot\MatchMath.java','tests\VariantStorageTest.java')+$testStubs)
Run (Join-Path $bin 'java.exe') @("-Djava.io.tmpdir=$build\tmp",'-cp',"$build\storage-tests;$json",'com.kev.fortniteautobot.VariantStorageTest')
Run (Join-Path $bt 'aapt2.exe') @('compile','--dir','app\src\main\res','-o',"$build\resources.zip")
$manifest=[xml](Get-Content -Raw app\src\main\AndroidManifest.xml)
$manifest.manifest.SetAttribute('package','com.kev.fortniteautobot')
$manifest.Save("$build\AndroidManifest.xml")
Run (Join-Path $bt 'aapt2.exe') @('link','-o',"$build\unsigned.apk",'--manifest',"$build\AndroidManifest.xml",'-I',$android,'--version-code','140','--version-name','1.4.0','--min-sdk-version','29','--target-sdk-version','35',"$build\resources.zip")
$sources=@(Get-ChildItem app\src\main\java -Filter *.java -Recurse | ForEach-Object FullName)
Run (Join-Path $bin 'javac.exe') (@('--release','8','-encoding','UTF-8','-cp',$android,'-d',"$build\classes")+$sources)
Run (Join-Path $bin 'jar.exe') @('cf',"$build\app.jar",'-C',"$build\classes",'.')
Run (Join-Path $bt 'd8.bat') @('--min-api','29','--lib',$android,'--output',"$build\dex","$build\app.jar")
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[IO.Compression.ZipFile]::Open("$build\unsigned.apk",[IO.Compression.ZipArchiveMode]::Update)
try{[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build\dex\classes.dex",'classes.dex',[IO.Compression.CompressionLevel]::NoCompression) | Out-Null}finally{$zip.Dispose()}
Run (Join-Path $bt 'zipalign.exe') @('-f','4',"$build\unsigned.apk","$build\aligned.apk")
$key=Join-Path $PSScriptRoot 'signing\auto-bot.jks'
if(!(Test-Path -LiteralPath $key)){Run (Join-Path $bin 'keytool.exe') @('-genkeypair','-keystore',$key,'-storepass','android','-keypass','android','-alias','autobot','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Auto Bot Personal Build')}
$out=Join-Path $PSScriptRoot 'artifacts\Auto-Bot-1.4.0.apk'
Run (Join-Path $bt 'apksigner.bat') @('sign','--ks',$key,'--ks-key-alias','autobot','--ks-pass','pass:android','--key-pass','pass:android','--out',$out,"$build\aligned.apk")
Run (Join-Path $bt 'apksigner.bat') @('verify','--verbose','--print-certs',$out)
Run (Join-Path $bt 'zipalign.exe') @('-c','4',$out)
Run (Join-Path $bt 'aapt.exe') @('dump','badging',$out)
Write-Output "Built from clean source: $out"
