# Builds btkeys-v1.0.0.apk without Gradle.
# Pipeline: aapt2 -> javac -> d8 -> zipalign -> apksigner
#
# Requires a JDK 17 and Android build-tools (aapt2/d8/zipalign/apksigner) + an
# android.jar. Locate them with, in order of precedence:
#   $env:BTKEYS_TOOLCHAIN, $env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, ..\toolchain
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$out  = Join-Path $root 'build'

function Find-One($path, $filter) {
    $f = Get-ChildItem $path -Recurse -Filter $filter -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $f) { throw "not found: $filter under $path" }
    return $f.FullName
}

# ---------------- toolchain discovery ----------------
$candidates = @()
foreach ($c in @($env:BTKEYS_TOOLCHAIN, (Join-Path $root '..\toolchain'))) {
    if ($c) { $candidates += (Resolve-Path $c -ErrorAction SilentlyContinue) }
}
foreach ($c in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT,
                 "$env:LOCALAPPDATA\Android\Sdk", "$env:USERPROFILE\AppData\Local\Android\Sdk")) {
    if ($c -and (Test-Path $c)) { $candidates += (Resolve-Path $c) }
}

$jdkHome = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $jdkHome = $env:JAVA_HOME
}
foreach ($c in $candidates) {
    if ($jdkHome) { break }
    $javacExe = Get-ChildItem $c -Recurse -Filter 'javac.exe' -ErrorAction SilentlyContinue |
                Where-Object { $_.Directory.Name -eq 'bin' } | Select-Object -First 1
    if ($javacExe) { $jdkHome = $javacExe.Directory.Parent.FullName }
}
if (-not $jdkHome) { throw 'JDK not found. Install JDK 17 or set JAVA_HOME / BTKEYS_TOOLCHAIN.' }

$androidJar = $null; $bt = $null
foreach ($c in $candidates) {
    if (-not $androidJar) {
        $androidJar = (Get-ChildItem $c -Recurse -Filter 'android.jar' -ErrorAction SilentlyContinue |
                       Sort-Object Length -Descending | Select-Object -First 1).FullName
    }
    if (-not $bt) {
        $bt = (Get-ChildItem $c -Recurse -Filter 'aapt2.exe' -ErrorAction SilentlyContinue |
               Select-Object -First 1).DirectoryName
    }
}
if (-not $androidJar) { throw 'android.jar not found. Install an SDK platform or set ANDROID_HOME.' }
if (-not $bt)         { throw 'aapt2 not found. Install Android build-tools or set ANDROID_HOME.' }

$env:JAVA_HOME = $jdkHome
$javac      = Join-Path $jdkHome 'bin\javac.exe'
$jar        = Join-Path $jdkHome 'bin\jar.exe'
$keytool    = Join-Path $jdkHome 'bin\keytool.exe'
$aapt2      = Join-Path $bt 'aapt2.exe'
$d8         = Join-Path $bt 'd8.bat'
$zipalign   = Join-Path $bt 'zipalign.exe'
$apksigner  = Join-Path $bt 'apksigner.bat'

Write-Host "jdk      : $jdkHome"
Write-Host "build-tools: $bt"
Write-Host "android.jar: $androidJar"

if (Test-Path $out) { Remove-Item $out -Recurse -Force }
New-Item -ItemType Directory -Force -Path $out, "$out\gen", "$out\obj", "$out\dex" | Out-Null

# ---------- 1. resources ----------
Write-Host '== aapt2 compile'
& $aapt2 compile --dir (Join-Path $root 'res') -o "$out\res.zip"
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

Write-Host '== aapt2 link'
& $aapt2 link -o "$out\base.apk" -I $androidJar --manifest (Join-Path $root 'AndroidManifest.xml') `
    --java "$out\gen" -R "$out\res.zip" --auto-add-overlay
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

# ---------- 2. java ----------
Write-Host '== javac'
$sources  = @(Get-ChildItem (Join-Path $root 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$sources += @(Get-ChildItem "$out\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
# javac argument files treat '\' as an escape, so normalise to forward slashes.
$argfile  = "$out\javac.args"
($sources | ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' }) | Set-Content -Path $argfile -Encoding ASCII
& $javac -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath $androidJar -d "$out\obj" "@$argfile"
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

# ---------- 3. dex ----------
Write-Host '== jar + d8'
& $jar --create --file "$out\classes.jar" -C "$out\obj" .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
& $d8 --release --min-api 28 --lib $androidJar --output "$out\dex" "$out\classes.jar"
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::Open("$out\base.apk", 'Update')
$existing = $zip.GetEntry('classes.dex')
if ($existing) { $existing.Delete() }
[IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "$out\dex\classes.dex", 'classes.dex', 'Optimal') | Out-Null
$zip.Dispose()

# ---------- 4. align + sign ----------
Write-Host '== zipalign'
& $zipalign -f 4 "$out\base.apk" "$out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

# The debug keystore is generated on first run and is NOT committed: every clone
# signs with its own key, which is fine for sideloading but not for store updates.
$ks = Join-Path $root 'debug.keystore'
if (-not (Test-Path $ks)) {
    Write-Host '== keytool (debug keystore)'
    & $keytool -genkeypair -keystore $ks -storepass android -keypass android -alias androiddebugkey `
        -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}

Write-Host '== apksigner'
& $apksigner sign --ks $ks --ks-pass pass:android --key-pass pass:android `
    --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true `
    --out "$out\btkeys-signed.apk" "$out\aligned.apk"
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

& $apksigner verify --print-certs "$out\btkeys-signed.apk"
$final = Join-Path $root 'btkeys-v1.0.0.apk'
Copy-Item "$out\btkeys-signed.apk" $final -Force
Write-Host "== done: $final  ($([math]::Round((Get-Item $final).Length/1KB,1)) KB)"
