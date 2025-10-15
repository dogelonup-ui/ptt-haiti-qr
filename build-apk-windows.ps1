Param(
  [string]$ProjectDir = "."
)
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$gradleProps = Join-Path $ProjectDir "gradle.properties"
if (Test-Path $gradleProps) {
  Add-Content -Path $gradleProps -Value "`norg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 -Djavax.net.ssl.trustStoreType=Windows-ROOT"
}

Push-Location $ProjectDir
if (-Not (Test-Path ".\gradlew.bat")) {
  if (Get-Command gradle -ErrorAction SilentlyContinue) {
    gradle wrapper --gradle-version 8.5
  } else {
    Write-Host "Gradle wrapper missing and Gradle not installed. Open the project once in Android Studio to generate the wrapper." -ForegroundColor Yellow
  }
}

$HomeSdk = Join-Path $env:USERPROFILE ".android-sdk-lite"
$SdkTools = Join-Path $HomeSdk "cmdline-tools"
$SdkBin = Join-Path $SdkTools "latest\bin"
if (-Not (Test-Path $SdkBin)) {
  New-Item -Force -ItemType Directory -Path $SdkTools | Out-Null
  $zip = Join-Path $env:TEMP "sdk-tools.zip"
  Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip" -OutFile $zip
  Expand-Archive -Path $zip -DestinationPath $SdkTools -Force
  Rename-Item -Path (Join-Path $SdkTools "cmdline-tools") -NewName "latest" -Force
}

$env:ANDROID_HOME = $HomeSdk
$env:ANDROID_SDK_ROOT = $HomeSdk
$env:PATH = "$SdkBin;$env:PATH"

& $SdkBin\sdkmanager.bat --sdk_root=$HomeSdk "platform-tools" "platforms;android-34" "build-tools;34.0.0" | Out-Null
yes | & $SdkBin\sdkmanager.bat --sdk_root=$HomeSdk --licenses | Out-Null

.\gradlew.bat --no-daemon assembleDebug

Write-Host "`n✅ APK should be here: $ProjectDir\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
Pop-Location
