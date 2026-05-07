@echo off
:: PiDev Application Launcher
title PiDev Application

echo ====================================
echo    Starting PiDev Application
echo ====================================
echo.

:: Change to project directory
cd /d "%~dp0"

:: Check if Maven wrapper exists
if exist "mvnw.cmd" (
    echo Using Maven Wrapper...
    call mvnw.cmd clean javafx:run
    goto :end
)

:: Try to find Maven in various locations
set "MAVEN_FOUND="

:: Check PATH first
where mvn.cmd >nul 2>&1
if %errorlevel% equ 0 (
    echo Using Maven from PATH...
    set "MAVEN_FOUND=1"
    call mvn.cmd clean javafx:run
    goto :end
)

:: IntelliJ IDEA bundled Maven locations
echo Searching for IntelliJ Maven...

:: IntelliJ Community/Ultimate common locations
for %%i in (
    "%ProgramFiles%\JetBrains\IntelliJ IDEA Community Edition*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%ProgramFiles%\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%ProgramFiles(x86)%\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U\*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\*\plugins\maven\lib\maven3\bin\mvn.cmd"
    "%USERPROFILE%\.m2\wrapper\dists\apache-maven-*\bin\mvn.cmd"
) do (
    for /f "delims=" %%f in ('dir /b /s "%%~i" 2^>nul') do (
        if exist "%%f" (
            echo Found Maven at: %%f
            set "MAVEN_FOUND=1"
            call "%%f" clean javafx:run
            goto :end
        )
    )
)

:: Common standalone Maven installations
echo Checking common Maven installations...
for %%i in (
    "C:\Program Files\Apache\Maven\bin\mvn.cmd"
    "C:\Program Files\apache-maven*\bin\mvn.cmd"
    "C:\apache-maven*\bin\mvn.cmd"
    "C:\Maven\bin\mvn.cmd"
    "%MAVEN_HOME%\bin\mvn.cmd"
) do (
    for /f "delims=" %%f in ('dir /b /s "%%~i" 2^>nul') do (
        if exist "%%f" (
            echo Found Maven at: %%f
            set "MAVEN_FOUND=1"
            call "%%f" clean javafx:run
            goto :end
        )
    )
)

:: If Maven still not found
if not defined MAVEN_FOUND (
    echo.
    echo ========================================
    echo ERROR: Maven not found!
    echo ========================================
    echo.
    echo Please either:
    echo 1. Install Maven and add it to PATH
    echo 2. Open IntelliJ IDEA and run the project
    echo 3. Check that IntelliJ IDEA is installed
    echo.
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

:end

if errorlevel 1 (
    echo.
    echo Application failed to start!
    echo Press any key to exit...
    pause >nul
    exit /b 1
)

exit /b 0
