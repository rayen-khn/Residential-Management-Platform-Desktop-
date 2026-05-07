@echo off
setlocal enabledelayedexpansion

title PiDev - Run from IntelliJ Tools
cd /d "%~dp0"

echo ================================================
echo   PiDev Dynamic Island - Quick Run
echo ================================================
echo.

REM Find IntelliJ's bundled Maven
set "MVN="

REM JetBrains Toolbox installations
for /d %%i in ("%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-U\ch-0\*") do (
    if exist "%%i\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set "MVN=%%i\plugins\maven\lib\maven3\bin\mvn.cmd"
        set "JAVA_HOME=%%i\jbr"
    )
)

if not defined MVN (
    for /d %%i in ("%LOCALAPPDATA%\JetBrains\Toolbox\apps\IDEA-C\ch-0\*") do (
        if exist "%%i\plugins\maven\lib\maven3\bin\mvn.cmd" (
            set "MVN=%%i\plugins\maven\lib\maven3\bin\mvn.cmd"
            set "JAVA_HOME=%%i\jbr"
        )
    )
)

REM Standard IntelliJ installations
if not defined MVN (
    for /d %%i in ("%ProgramFiles%\JetBrains\IntelliJ IDEA*") do (
        if exist "%%i\plugins\maven\lib\maven3\bin\mvn.cmd" (
            set "MVN=%%i\plugins\maven\lib\maven3\bin\mvn.cmd"
            set "JAVA_HOME=%%i\jbr"
        )
    )
)

if not defined MVN (
    for /d %%i in ("%ProgramFiles%\JetBrains\IntelliJ IDEA Community*") do (
        if exist "%%i\plugins\maven\lib\maven3\bin\mvn.cmd" (
            set "MVN=%%i\plugins\maven\lib\maven3\bin\mvn.cmd"
            set "JAVA_HOME=%%i\jbr"
        )
    )
)

REM Fallback to PATH
if not defined MVN (
    where mvn >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set "MVN=mvn"
    )
)

if not defined MVN (
    echo [ERROR] Could not find Maven!
    echo.
    echo Make sure IntelliJ IDEA is installed, or add Maven to PATH.
    echo.
    pause
    exit /b 1
)

echo Using Maven: %MVN%
echo Using Java:  %JAVA_HOME%
echo.
echo Compiling and running...
echo.

call "%MVN%" compile javafx:run -q

echo.
echo Application closed.
pause
