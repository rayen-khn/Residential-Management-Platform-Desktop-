@echo off
setlocal

title PiDev - Build Standalone EXE
color 0B

echo ================================================================
echo   PiDev Dynamic Island - Standalone EXE Builder
echo ================================================================
echo.
echo This will create a standalone .exe that works on any Windows PC
echo without requiring Java or Maven installation.
echo.
echo ================================================================
echo.

cd /d "%~dp0"

REM Create tools directory
if not exist "tools" mkdir "tools"

REM ============================================================
REM STEP 1: Check/Download Java
REM ============================================================
echo [1/5] Checking Java...

set "JAVA_DIR=%~dp0tools\jdk-21"
set "JAVA_EXE=%JAVA_DIR%\bin\java.exe"
set "JPACKAGE=%JAVA_DIR%\bin\jpackage.exe"

if exist "%JAVA_EXE%" (
    echo        [OK] Java found locally
) else (
    echo        [INFO] Downloading Java 21... This may take a few minutes.
    echo.
    
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.2%%2B13/OpenJDK21U-jdk_x64_windows_hotspot_21.0.2_13.zip' -OutFile '%~dp0tools\jdk.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\jdk.zip" (
        echo [ERROR] Failed to download Java!
        pause
        exit /b 1
    )
    
    echo        Extracting Java...
    powershell -Command "Expand-Archive -Path '%~dp0tools\jdk.zip' -DestinationPath '%~dp0tools\jdk-temp' -Force"
    
    for /d %%d in ("%~dp0tools\jdk-temp\jdk-*") do (
        if exist "%JAVA_DIR%" rmdir /s /q "%JAVA_DIR%"
        move "%%d" "%JAVA_DIR%" >nul
    )
    
    if exist "%~dp0tools\jdk-temp" rmdir /s /q "%~dp0tools\jdk-temp"
    if exist "%~dp0tools\jdk.zip" del "%~dp0tools\jdk.zip"
    
    if exist "%JAVA_EXE%" (
        echo        [OK] Java installed!
    ) else (
        echo [ERROR] Java installation failed!
        pause
        exit /b 1
    )
)

set "JAVA_HOME=%JAVA_DIR%"
set "PATH=%JAVA_DIR%\bin;%PATH%"
echo.

REM ============================================================
REM STEP 2: Check/Download Maven
REM ============================================================
echo [2/5] Checking Maven...

set "MAVEN_DIR=%~dp0tools\maven"
set "MVN=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MVN%" (
    echo        [OK] Maven found locally
) else (
    echo        [INFO] Downloading Maven...
    
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0tools\maven.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\maven.zip" (
        echo [ERROR] Failed to download Maven!
        pause
        exit /b 1
    )
    
    echo        Extracting Maven...
    powershell -Command "Expand-Archive -Path '%~dp0tools\maven.zip' -DestinationPath '%~dp0tools\maven-temp' -Force"
    
    for /d %%d in ("%~dp0tools\maven-temp\apache-maven-*") do (
        if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
        move "%%d" "%MAVEN_DIR%" >nul
    )
    
    if exist "%~dp0tools\maven-temp" rmdir /s /q "%~dp0tools\maven-temp"
    if exist "%~dp0tools\maven.zip" del "%~dp0tools\maven.zip"
    
    if exist "%MVN%" (
        echo        [OK] Maven installed!
    ) else (
        echo [ERROR] Maven installation failed!
        pause
        exit /b 1
    )
)
echo.

REM ============================================================
REM STEP 3: Build the project with Maven
REM ============================================================
echo [3/5] Building project with Maven...
echo.

call "%MVN%" clean package -DskipTests -q

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Maven build failed!
    pause
    exit /b 1
)

echo        Build successful!
echo.

REM ============================================================
REM STEP 4: Prepare for jpackage
REM ============================================================
echo [4/5] Preparing application package...

set "APP_NAME=PiDev-DynamicIsland"
set "APP_VERSION=1.0.0"
set "MAIN_JAR=wolfs_pidev_3a6-1.0-SNAPSHOT.jar"
set "MAIN_CLASS=com.pidev.Launcher"

REM Create output directory
if exist "dist" rmdir /s /q "dist"
mkdir "dist"

REM Copy the shaded JAR
copy "target\%MAIN_JAR%" "dist\" >nul

echo        Prepared!
echo.

REM ============================================================
REM STEP 5: Create native EXE with jpackage
REM ============================================================
echo [5/5] Creating standalone EXE (this may take a few minutes)...
echo.

REM Clean previous build
if exist "output" rmdir /s /q "output"

"%JPACKAGE%" ^
    --type app-image ^
    --name "%APP_NAME%" ^
    --input "dist" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --dest "output" ^
    --app-version "%APP_VERSION%" ^
    --vendor "PiDev Team" ^
    --description "PiDev Dynamic Island Desktop Application" ^
    --java-options "--enable-preview" ^
    --java-options "-Dprism.dirtyopts=false" ^
    --win-console

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] jpackage failed!
    echo.
    echo Trying alternative method...
    
    REM Try without win-console
    "%JPACKAGE%" ^
        --type app-image ^
        --name "%APP_NAME%" ^
        --input "dist" ^
        --main-jar "%MAIN_JAR%" ^
        --main-class "%MAIN_CLASS%" ^
        --dest "output" ^
        --app-version "%APP_VERSION%"
    
    if %ERRORLEVEL% neq 0 (
        echo.
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
)

echo.
echo ================================================================
echo   BUILD SUCCESSFUL!
echo ================================================================
echo.
echo Your standalone application is ready at:
echo   %CD%\output\%APP_NAME%\
echo.
echo The EXE file is:
echo   %CD%\output\%APP_NAME%\%APP_NAME%.exe
echo.
echo You can copy the entire "%APP_NAME%" folder to any Windows PC
echo and it will run without needing Java or Maven installed!
echo.
echo ================================================================
echo.

REM Open the output folder
explorer "output\%APP_NAME%"

pause
