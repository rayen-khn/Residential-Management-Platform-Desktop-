@echo off
REM Direct Installation - Install requirements directly to Python

echo.
echo Installing Python dependencies directly...
echo.

REM Install requirements directly
python -m pip install --upgrade pip
pip install -r requirements.txt

echo.
if %errorlevel% equ 0 (
    echo Success! All requirements installed.
    echo You can now run: python app.py
) else (
    echo Error installing requirements.
)
echo.

pause
