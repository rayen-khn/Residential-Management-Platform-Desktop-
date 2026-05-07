@echo off
echo Starting Syndicati Desktop Application...
echo.

cd /d "%~dp0"

REM Use the bundled toolchain when available
if exist "tools\jdk-25\bin\java.exe" set "JAVA_HOME=%~dp0tools\jdk-25"
if exist "tools\maven\bin\mvn.cmd" set "MVN=%~dp0tools\maven\bin\mvn.cmd"
if not defined MVN set "MVN=mvn"

echo Compiling and running application...
call "%MVN%" compile javafx:run

if %ERRORLEVEL% neq 0 (
    echo Application failed to start!
    pause
    exit /b 1
)

pause
