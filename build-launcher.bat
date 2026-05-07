@echo off
echo ====================================
echo  Building PiDev Launcher
echo ====================================
echo.

cd /d "%~dp0"

:: Compile the Launcher
echo Compiling Launcher.java...
javac src\main\java\com\pidev\Launcher.java
if errorlevel 1 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

:: Create build directory
if not exist "build" mkdir build

:: Create JAR
echo Creating launcher.jar...
cd src\main\java
jar cfe ..\..\..\build\launcher.jar com.pidev.Launcher com\pidev\Launcher.class
cd ..\..\..

:: Create simple runner batch file
echo Creating PiDevApp.bat...
(
    echo @echo off
    echo java -jar "%%~dp0build\launcher.jar"
) > PiDevApp.bat

echo.
echo ====================================
echo SUCCESS!
echo ====================================
echo.
echo PiDevApp.bat has been created!
echo Double-click it to launch the application.
echo.
echo To convert to EXE:
echo 1. Download: https://www.f2ko.de/en/b2e.php
echo 2. Open Bat To Exe Converter
echo 3. Load PiDevApp.bat and click Compile
echo.

:: Cleanup
del src\main\java\com\pidev\Launcher.class 2>nul

pause
