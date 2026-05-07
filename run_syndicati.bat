@echo off
REM Syndicati Application Launcher
REM Launches Syndicati with InsightFace face detection

echo.
echo ========================================
echo   Syndicati with InsightFace
echo ========================================
echo.

REM Verify Python is installed
where python >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Python not found!
    echo Please install Python 3.8+ and add to PATH
    echo Get it from: https://www.python.org/downloads/
    echo.
    pause
    exit /b 1
)

REM Verify InsightFace is installed
python -c "import insightface" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Installing InsightFace...
    pip install insightface onnxruntime opencv-python -q
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Failed to install InsightFace
        echo Run manually: pip install insightface onnxruntime opencv-python
        pause
        exit /b 1
    )
    echo.
    echo InsightFace installed successfully!
)

echo.

REM Set Java environment
if not defined JAVA_HOME set JAVA_HOME=c:\Users\amine\OneDrive\Desktop\Syndicati_Java\tools\jdk-25
set PATH=%JAVA_HOME%\bin;c:\Users\amine\OneDrive\Desktop\Syndicati_Java\tools\maven\bin;%PATH%

REM Launch app
echo Starting Syndicati...
echo.

cd /d c:\Users\amine\OneDrive\Desktop\Syndicati_Java

.\tools\maven\bin\mvn.cmd javafx:run

pause
