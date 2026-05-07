@echo off
setlocal

echo [INFO] Loading Visual Studio Build Tools environment...
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\Tools\VsDevCmd.bat" -arch=x64
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to load VsDevCmd.bat
    exit /b 1
)

echo [INFO] Installing insightface with Python 3.11...
py -3.11 -m pip install --upgrade pip
py -3.11 -m pip install --upgrade insightface
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] insightface installation failed.
    exit /b 1
)

echo [INFO] Verifying insightface import...
py -3.11 -c "import insightface; print('INSIGHTFACE_OK')"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] insightface import check failed.
    exit /b 1
)

echo [OK] insightface installed and verified.
exit /b 0
