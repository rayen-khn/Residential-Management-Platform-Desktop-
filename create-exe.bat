@echo off
:: This script creates a PiDevApp.exe using IExpress (built into Windows)

echo ====================================
echo Creating PiDev Application EXE
echo ====================================
echo.

:: Create temporary SED file for IExpress
set "SED_FILE=%TEMP%\pidev_iexpress.sed"
set "PROJECT_DIR=%~dp0"
set "OUTPUT_EXE=%PROJECT_DIR%PiDevApp.exe"

echo Creating IExpress configuration...

(
echo [Version]
echo Class=IEXPRESS
echo SEDVersion=3
echo [Options]
echo PackagePurpose=InstallApp
echo ShowInstallProgramWindow=0
echo HideExtractAnimation=1
echo UseLongFileName=1
echo InsideCompressed=0
echo CAB_FixedSize=0
echo CAB_ResvCodeSigning=0
echo RebootMode=N
echo InstallPrompt=%%InstallPrompt%%
echo DisplayLicense=%%DisplayLicense%%
echo FinishMessage=%%FinishMessage%%
echo TargetName=%%TargetName%%
echo FriendlyName=%%FriendlyName%%
echo AppLaunched=%%AppLaunched%%
echo PostInstallCmd=%%PostInstallCmd%%
echo AdminQuietInstCmd=%%AdminQuietInstCmd%%
echo UserQuietInstCmd=%%UserQuietInstCmd%%
echo SourceFiles=%%SourceFiles%%
echo [Strings]
echo InstallPrompt=
echo DisplayLicense=
echo FinishMessage=
echo TargetName=%OUTPUT_EXE%
echo FriendlyName=PiDev Application
echo AppLaunched=cmd /c launch-intellij.vbs
echo PostInstallCmd=^<None^>
echo AdminQuietInstCmd=
echo UserQuietInstCmd=
echo FILE0="launch-intellij.vbs"
echo SourceFiles=SourceFiles
echo [SourceFiles]
echo SourceFiles0=%PROJECT_DIR%
echo [SourceFiles0]
echo %%FILE0%%=
) > "%SED_FILE%"

echo.
echo Building EXE with IExpress...
iexpress /N /Q "%SED_FILE%"

if exist "%OUTPUT_EXE%" (
    echo.
    echo ====================================
    echo SUCCESS!
    echo ====================================
    echo.
    echo PiDevApp.exe has been created!
    echo Location: %OUTPUT_EXE%
    echo.
    echo You can now double-click PiDevApp.exe to launch the application.
    echo.
) else (
    echo.
    echo ERROR: Failed to create EXE
    echo Please try running this script as Administrator
    echo.
)

del "%SED_FILE%"
pause
