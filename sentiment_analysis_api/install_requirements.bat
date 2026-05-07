@echo off
REM Install Python Requirements for Sentiment Analysis App

echo.
echo Installing Python dependencies...
echo.

REM Check if venv exists, if not create it
if not exist venv (
    echo Creating virtual environment...
    python -m venv venv
)

REM Activate virtual environment
call venv\Scripts\activate.bat

REM Upgrade pip
echo Upgrading pip...
python -m pip install --upgrade pip

REM Install requirements
echo Installing requirements from requirements.txt...
pip install -r requirements.txt

REM Check if successful
if %errorlevel% equ 0 (
    echo.
    echo Success! All requirements installed.
    echo You can now run: .\start_server.ps1
    echo.
) else (
    echo.
    echo Error installing requirements. Please check the output above.
    echo.
)

pause
