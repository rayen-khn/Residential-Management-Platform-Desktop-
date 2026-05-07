# PowerShell script to create EXE from VBScript using IExpress

$projectDir = $PSScriptRoot
$sedFile = "$env:TEMP\pidev_iexpress.sed"
$outputExe = "$projectDir\PiDevApp.exe"
$vbsScript = "$projectDir\launch-intellij.vbs"

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Creating PiDev Application EXE" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Check if VBS script exists
if (-not (Test-Path $vbsScript)) {
    Write-Host "ERROR: launch-intellij.vbs not found!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Creating IExpress configuration..." -ForegroundColor Yellow

# Create IExpress SED file
$sedContent = @"
[Version]
Class=IEXPRESS
SEDVersion=3

[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=0
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=%TargetName%
FriendlyName=%FriendlyName%
AppLaunched=%AppLaunched%
PostInstallCmd=%PostInstallCmd%
AdminQuietInstCmd=%AdminQuietInstCmd%
UserQuietInstCmd=%UserQuietInstCmd%
SourceFiles=SourceFiles

[Strings]
InstallPrompt=
DisplayLicense=
FinishMessage=
TargetName=$outputExe
FriendlyName=PiDev Application
AppLaunched=wscript.exe launch-intellij.vbs
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
FILE0="launch-intellij.vbs"
SourceFiles=SourceFiles

[SourceFiles]
SourceFiles0=$projectDir\

[SourceFiles0]
%FILE0%=
"@

$sedContent | Out-File -FilePath $sedFile -Encoding ASCII

Write-Host "Building EXE with IExpress..." -ForegroundColor Yellow

# Run IExpress
$process = Start-Process -FilePath "iexpress.exe" -ArgumentList "/N /Q `"$sedFile`"" -Wait -PassThru

if (Test-Path $outputExe) {
    Write-Host ""
    Write-Host "====================================" -ForegroundColor Green
    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host "====================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "PiDevApp.exe has been created!" -ForegroundColor Green
    Write-Host "Location: $outputExe" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "You can now double-click PiDevApp.exe to launch the application." -ForegroundColor Yellow
    Write-Host ""
}
else {
    Write-Host ""
    Write-Host "ERROR: Failed to create EXE" -ForegroundColor Red
    Write-Host "Please try running this script as Administrator" -ForegroundColor Yellow
    Write-Host ""
}

# Cleanup
Remove-Item $sedFile -ErrorAction SilentlyContinue

Read-Host "Press Enter to exit"
