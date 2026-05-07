@echo off
setlocal enabledelayedexpansion

title Syndicati - Development Runner
color 0A

echo ============================================
echo   Syndicati - Dev Runner
echo ============================================
echo.

REM Check for Maven in common locations
set "MAVEN_CMD="

REM Check if mvn is in PATH
where mvn >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "MAVEN_CMD=mvn"
    goto :found_maven
)

REM Check common Maven installation paths
set "MAVEN_PATHS=C:\Program Files\Apache\maven\bin\mvn.cmd;C:\Program Files\Maven\bin\mvn.cmd;C:\maven\bin\mvn.cmd;C:\apache-maven\bin\mvn.cmd;%USERPROFILE%\apache-maven\bin\mvn.cmd;%USERPROFILE%\.m2\wrapper\dists\apache-maven-*\*\apache-maven-*\bin\mvn.cmd"

for %%p in ("%MAVEN_PATHS:;=" "%") do (
    if exist "%%~p" (
        set "MAVEN_CMD=%%~p"
        goto :found_maven
    )
)

REM Check for Maven in Program Files
for /d %%d in ("C:\Program Files\apache-maven-*") do (
    if exist "%%d\bin\mvn.cmd" (
        set "MAVEN_CMD=%%d\bin\mvn.cmd"
        goto :found_maven
    )
)

for /d %%d in ("C:\Program Files (x86)\apache-maven-*") do (
    if exist "%%d\bin\mvn.cmd" (
        set "MAVEN_CMD=%%d\bin\mvn.cmd"
        goto :found_maven
    )
)

REM Check MAVEN_HOME environment variable
if defined MAVEN_HOME (
    if exist "%MAVEN_HOME%\bin\mvn.cmd" (
        set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
        goto :found_maven
    )
)

REM Check M2_HOME environment variable
if defined M2_HOME (
    if exist "%M2_HOME%\bin\mvn.cmd" (
        set "MAVEN_CMD=%M2_HOME%\bin\mvn.cmd"
        goto :found_maven
    )
)

echo [ERROR] Maven not found!
echo.
echo Please install Maven and add it to your PATH, or set MAVEN_HOME.
echo Download from: https://maven.apache.org/download.cgi
echo.
pause
exit /b 1

:found_maven
echo [OK] Maven found: %MAVEN_CMD%
echo.

REM Navigate to project directory
cd /d "%~dp0"

:main_loop
cls
echo ============================================
echo   Syndicati - Dev Runner
echo ============================================
echo.
echo [1] Run application (compile + run)
echo [2] Clean and run (full rebuild)
echo [3] Watch mode (auto-restart on changes)
echo [4] Just compile (no run)
echo [5] Exit
echo.
set /p choice="Select option: "

if "%choice%"=="1" goto :run_app
if "%choice%"=="2" goto :clean_run
if "%choice%"=="3" goto :watch_mode
if "%choice%"=="4" goto :compile_only
if "%choice%"=="5" exit /b 0
goto :main_loop

:compile_only
echo.
echo [INFO] Compiling project...
call "%MAVEN_CMD%" compile -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    goto :main_loop
)
echo [OK] Compilation successful!
pause
goto :main_loop

:run_app
echo.
echo [INFO] Compiling and running application...
echo.
call "%MAVEN_CMD%" compile exec:java -Dexec.mainClass="com.syndicati.Launcher" -q
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Application exited with error.
)
echo.
echo [INFO] Application closed.
pause
goto :main_loop

:clean_run
echo.
echo [INFO] Cleaning and rebuilding...
echo.
call "%MAVEN_CMD%" clean compile exec:java -Dexec.mainClass="com.syndicati.Launcher"
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Application exited with error.
)
echo.
echo [INFO] Application closed.
pause
goto :main_loop

:watch_mode
echo.
echo ============================================
echo   WATCH MODE - Auto-restart on changes
echo ============================================
echo.
echo Press Ctrl+C to stop watching.
echo.

:watch_loop
echo [%TIME%] Compiling and starting application...
echo.

REM Compile
call "%MAVEN_CMD%" compile -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Compilation failed! Waiting for changes...
    timeout /t 5 /nobreak >nul
    goto :watch_loop
)

REM Run the application
start /wait cmd /c ""%MAVEN_CMD%" exec:java -Dexec.mainClass="com.syndicati.Launcher" -q"

echo.
echo [%TIME%] Application closed. Restarting in 2 seconds...
echo          (Press Ctrl+C to stop)
timeout /t 2 /nobreak >nul
goto :watch_loop

