@echo off
echo Starting Syndicati Desktop Application with Maven...
echo.

REM Clean and compile
echo Cleaning and compiling...
mvn clean compile

if %ERRORLEVEL% neq 0 (
    echo Maven compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.

REM Run the application
echo Starting application...
mvn exec:java -Dexec.mainClass="com.syndicati.Launcher"

pause

