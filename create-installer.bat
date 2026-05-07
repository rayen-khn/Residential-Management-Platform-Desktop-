@echo off
setlocal

title PiDev - Installer Builder
color 0B

cd /d "%~dp0"

echo ================================================================
echo   PiDev Dynamic Island - Professional Installer Builder
echo ================================================================
echo.
echo This script will:
echo   1. Download Java 21 (bundled with your app)
echo   2. Download Maven and build your app
echo   3. Download Inno Setup (free installer creator)
echo   4. Create a professional Windows installer (.exe)
echo.
echo ================================================================
echo.

REM Create directories
if not exist "tools" mkdir "tools"
if not exist "installer" mkdir "installer"
if not exist "dist\app" mkdir "dist\app"
if not exist "installer-output" mkdir "installer-output"

REM ============================================================
REM STEP 1: Download/Check Java
REM ============================================================
echo [1/6] Checking Java JDK 21...

set "JAVA_DIR=%~dp0tools\jdk-21"
set "JAVA_EXE=%JAVA_DIR%\bin\java.exe"

if exist "%JAVA_EXE%" (
    echo        [OK] Java found
) else (
    echo        Downloading Java 21...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Write-Host '        Downloading... (this may take a few minutes)'; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.2%%2B13/OpenJDK21U-jdk_x64_windows_hotspot_21.0.2_13.zip' -OutFile '%~dp0tools\jdk.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\jdk.zip" (
        echo        [ERROR] Failed to download Java!
        pause
        exit /b 1
    )
    
    echo        Extracting...
    powershell -Command "Expand-Archive -Path '%~dp0tools\jdk.zip' -DestinationPath '%~dp0tools\jdk-temp' -Force"
    
    for /d %%d in ("%~dp0tools\jdk-temp\jdk-*") do (
        if exist "%JAVA_DIR%" rmdir /s /q "%JAVA_DIR%"
        move "%%d" "%JAVA_DIR%" >nul
    )
    
    if exist "%~dp0tools\jdk-temp" rmdir /s /q "%~dp0tools\jdk-temp"
    if exist "%~dp0tools\jdk.zip" del "%~dp0tools\jdk.zip"
    
    echo        [OK] Java installed
)

set "JAVA_HOME=%JAVA_DIR%"
set "PATH=%JAVA_DIR%\bin;%PATH%"

REM ============================================================
REM STEP 2: Download/Check Maven
REM ============================================================
echo [2/6] Checking Maven...

set "MAVEN_DIR=%~dp0tools\maven"
set "MVN=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MVN%" (
    echo        [OK] Maven found
) else (
    echo        Downloading Maven...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0tools\maven.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\maven.zip" (
        echo        [ERROR] Failed to download Maven!
        pause
        exit /b 1
    )
    
    echo        Extracting...
    powershell -Command "Expand-Archive -Path '%~dp0tools\maven.zip' -DestinationPath '%~dp0tools\maven-temp' -Force"
    
    for /d %%d in ("%~dp0tools\maven-temp\apache-maven-*") do (
        if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
        move "%%d" "%MAVEN_DIR%" >nul
    )
    
    if exist "%~dp0tools\maven-temp" rmdir /s /q "%~dp0tools\maven-temp"
    if exist "%~dp0tools\maven.zip" del "%~dp0tools\maven.zip"
    
    echo        [OK] Maven installed
)

REM ============================================================
REM STEP 3: Build the application JAR
REM ============================================================
echo [3/6] Building application...

call "%MVN%" clean package -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo        [ERROR] Build failed!
    pause
    exit /b 1
)
echo        [OK] Application built

REM ============================================================
REM STEP 4: Create app package with jpackage
REM ============================================================
echo [4/6] Creating application package...

set "JPACKAGE=%JAVA_DIR%\bin\jpackage.exe"

REM Clean previous build
if exist "dist\app" rmdir /s /q "dist\app"
mkdir "dist\app"

REM Copy the fat JAR
copy "target\wolfs_pidev_3a6-1.0-SNAPSHOT.jar" "dist\app.jar" >nul

REM Create native app image with bundled JRE
"%JPACKAGE%" ^
    --type app-image ^
    --name "PiDev" ^
    --input "dist" ^
    --main-jar "app.jar" ^
    --main-class "com.pidev.Launcher" ^
    --dest "dist\app-temp" ^
    --app-version "1.0.0" ^
    --vendor "PiDev Team" ^
    --description "PiDev Dynamic Island Desktop Application"

if %ERRORLEVEL% neq 0 (
    echo        [ERROR] jpackage failed!
    pause
    exit /b 1
)

REM Move to final location
xcopy "dist\app-temp\PiDev\*" "dist\app\" /E /I /Y >nul
rmdir /s /q "dist\app-temp"
del "dist\app.jar"

echo        [OK] Application packaged

REM ============================================================
REM STEP 5: Download/Check Inno Setup
REM ============================================================
echo [5/6] Checking Inno Setup...

set "INNO_DIR=%~dp0tools\innosetup"
set "ISCC=%INNO_DIR%\ISCC.exe"

if exist "%ISCC%" (
    echo        [OK] Inno Setup found
) else (
    echo        Downloading Inno Setup...
    
    REM Download Inno Setup portable/quick version
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://files.jrsoftware.org/is/6/innosetup-6.2.2.exe' -OutFile '%~dp0tools\innosetup-installer.exe' -UseBasicParsing"
    
    if not exist "%~dp0tools\innosetup-installer.exe" (
        echo        [ERROR] Failed to download Inno Setup!
        echo.
        echo        Please download manually from: https://jrsoftware.org/isdl.php
        pause
        exit /b 1
    )
    
    echo        Installing Inno Setup silently...
    "%~dp0tools\innosetup-installer.exe" /VERYSILENT /SUPPRESSMSGBOXES /NORESTART /DIR="%INNO_DIR%"
    
    REM Wait for installation
    timeout /t 10 /nobreak >nul
    
    if exist "%ISCC%" (
        echo        [OK] Inno Setup installed
        del "%~dp0tools\innosetup-installer.exe"
    ) else (
        echo        [WARNING] Inno Setup installation may still be in progress.
        echo        Please wait and run this script again, or install manually.
        pause
        exit /b 1
    )
)

REM ============================================================
REM STEP 6: Create app icon if not exists
REM ============================================================
if not exist "installer\app-icon.ico" (
    echo        Creating default icon...
    REM Create a simple icon using PowerShell
    powershell -Command "$icon = [System.Drawing.Icon]::ExtractAssociatedIcon([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName); $fs = [System.IO.File]::Create('%~dp0installer\app-icon.ico'); $icon.Save($fs); $fs.Close()" 2>nul
    
    if not exist "installer\app-icon.ico" (
        REM Download a generic app icon
        powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/AmineMr/icon/main/app.ico' -OutFile '%~dp0installer\app-icon.ico' -UseBasicParsing" 2>nul
    )
)

REM ============================================================
REM STEP 7: Build the installer
REM ============================================================
echo [6/6] Building installer...

"%ISCC%" /Q "installer\pidev-installer.iss"

if %ERRORLEVEL% neq 0 (
    echo        [ERROR] Installer creation failed!
    pause
    exit /b 1
)

echo.
echo ================================================================
echo   SUCCESS! Installer created!
echo ================================================================
echo.
echo Your installer is ready at:
echo   %~dp0installer-output\PiDev-Setup-1.0.0.exe
echo.
echo This installer:
echo   - Has a professional Windows installation wizard
echo   - Includes progress bar during installation
echo   - Bundles Java runtime (no Java needed on target PC)
echo   - Creates Start Menu and Desktop shortcuts
echo   - Supports proper Windows uninstallation
echo.
echo ================================================================

REM Open output folder
explorer "installer-output"

pause
