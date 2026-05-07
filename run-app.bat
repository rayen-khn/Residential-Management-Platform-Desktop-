@echo off
setlocal

title Syndicati - Smart Runner
color 0A

cd /d "%~dp0"

echo ================================================================
echo   Syndicati - Smart Runner
echo ================================================================
echo.

REM Create tools directory
if not exist "tools" mkdir "tools"

REM ============================================================
REM STEP 1: Check/Download Java
REM ============================================================
if defined JAVA_HOME (
    set "JAVA_DIR=%JAVA_HOME%"
) else (
    set "JAVA_DIR=%~dp0tools\jdk-25"
)
set "JAVA_EXE=%JAVA_DIR%\bin\java.exe"

if exist "%JAVA_EXE%" (
    echo [OK] Java found locally
) else (
    echo [INFO] Downloading Java 25... This may take a few minutes.
    echo.
    
    set "JAVA_ZIP=%~dp0tools\jdk.zip"
    
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '%~dp0tools\jdk.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\jdk.zip" (
        echo [ERROR] Failed to download Java!
        echo Please check your internet connection.
        pause
        exit /b 1
    )
    
    echo Extracting Java...
    powershell -Command "Expand-Archive -Path '%~dp0tools\jdk.zip' -DestinationPath '%~dp0tools\jdk-temp' -Force"
    
    REM Move to correct location
    for /d %%d in ("%~dp0tools\jdk-temp\jdk-*") do (
        if exist "%JAVA_DIR%" rmdir /s /q "%JAVA_DIR%"
        move "%%d" "%JAVA_DIR%" >nul
    )
    
    REM Cleanup
    if exist "%~dp0tools\jdk-temp" rmdir /s /q "%~dp0tools\jdk-temp"
    if exist "%~dp0tools\jdk.zip" del "%~dp0tools\jdk.zip"
    
    if exist "%JAVA_EXE%" (
        echo [OK] Java installed successfully!
    ) else (
        echo [ERROR] Java installation failed!
        pause
        exit /b 1
    )
)

set "JAVA_HOME=%JAVA_DIR%"
set "PATH=%JAVA_DIR%\bin;%PATH%"

REM ============================================================
REM STEP 2: Check/Download Maven
REM ============================================================
set "MAVEN_DIR=%~dp0tools\maven"
set "MVN=%MAVEN_DIR%\bin\mvn.cmd"

if exist "%MVN%" (
    echo [OK] Maven found locally
) else (
    echo [INFO] Downloading Maven... This may take a minute.
    echo.
    
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip' -OutFile '%~dp0tools\maven.zip' -UseBasicParsing"
    
    if not exist "%~dp0tools\maven.zip" (
        echo [ERROR] Failed to download Maven!
        echo Please check your internet connection.
        pause
        exit /b 1
    )
    
    echo Extracting Maven...
    powershell -Command "Expand-Archive -Path '%~dp0tools\maven.zip' -DestinationPath '%~dp0tools\maven-temp' -Force"
    
    REM Move to correct location
    for /d %%d in ("%~dp0tools\maven-temp\apache-maven-*") do (
        if exist "%MAVEN_DIR%" rmdir /s /q "%MAVEN_DIR%"
        move "%%d" "%MAVEN_DIR%" >nul
    )
    
    REM Cleanup
    if exist "%~dp0tools\maven-temp" rmdir /s /q "%~dp0tools\maven-temp"
    if exist "%~dp0tools\maven.zip" del "%~dp0tools\maven.zip"
    
    if exist "%MVN%" (
        echo [OK] Maven installed successfully!
    ) else (
        echo [ERROR] Maven installation failed!
        pause
        exit /b 1
    )
)

echo.
echo ================================================================
echo   Starting Application...
echo ================================================================
echo.

REM Run with JavaFX Maven plugin
call "%MVN%" compile javafx:run

echo.
echo Application closed.
pause
