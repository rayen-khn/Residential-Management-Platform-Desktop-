@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "ROOT=%~dp0"
set "PF86=%ProgramFiles(x86)%"
set "PF64=%ProgramFiles%"
set "HAS_ERRORS=0"
set "HAS_WARNINGS=0"
set "HAS_MSVC=0"
set "PY_CMD=python"
set "PY_VER="
set "PY_MAJOR=0"
set "PY_MINOR=0"

echo.
echo ================================================================
echo   Syndicati - Prerequisites Verifier + Auto Installer
echo ================================================================
echo.

where winget >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "HAS_WINGET=1"
) else (
    set "HAS_WINGET=0"
)

call :check_java
call :check_maven
call :check_python
call :check_python_packages
call :check_insightface_cache
call :check_database_port
call :check_local_config
call :print_manual_notes

echo.
echo ================================================================
echo   Verification Summary
echo ================================================================
if %HAS_ERRORS% EQU 0 (
    echo [OK] No blocking errors detected.
) else (
    echo [ERROR] %HAS_ERRORS% blocking item^(s^) failed.
)
if %HAS_WARNINGS% GTR 0 (
    echo [WARN] %HAS_WARNINGS% warning item^(s^) need manual attention.
)
echo.
if %HAS_ERRORS% EQU 0 (
    echo You can run the app now with: run-app.bat
) else (
    echo Fix the blocking errors above, then re-run this script.
)
echo.
pause
exit /b %HAS_ERRORS%

:check_java
echo [1/7] Checking Java...
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        echo [OK] Using JAVA_HOME: %JAVA_HOME%
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        goto :eof
    )
)

set "JAVA_BIN=%ROOT%tools\jdk-25\bin\java.exe"
if exist "%JAVA_BIN%" (
    echo [OK] Using bundled Java: %JAVA_BIN%
    set "JAVA_HOME=%ROOT%tools\jdk-25"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    goto :eof
)

where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%j in ('where java 2^>nul') do (
        echo [OK] Java found on PATH: %%j
        goto :eof
    )
)

echo [INFO] Java not found. Attempting install...
if "%HAS_WINGET%"=="1" (
    winget install -e --id EclipseAdoptium.Temurin.25.JDK --accept-package-agreements --accept-source-agreements --silent
    where java >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Java installed via winget.
        goto :eof
    )
)

echo [ERROR] Java 25 missing and auto-install failed.
set /a HAS_ERRORS+=1
goto :eof

:check_maven
echo.
echo [2/7] Checking Maven...
set "MVN_BIN=%ROOT%tools\maven\bin\mvn.cmd"
if exist "%MVN_BIN%" (
    echo [OK] Using bundled Maven: %MVN_BIN%
    set "PATH=%ROOT%tools\maven\bin;%PATH%"
    goto :eof
)

where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%m in ('where mvn 2^>nul') do (
        echo [OK] Maven found on PATH: %%m
        goto :eof
    )
)

echo [INFO] Maven not found. Attempting install...
if "%HAS_WINGET%"=="1" (
    winget install -e --id Apache.Maven --accept-package-agreements --accept-source-agreements --silent
    where mvn >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Maven installed via winget.
        goto :eof
    )
)

echo [ERROR] Maven missing and auto-install failed.
set /a HAS_ERRORS+=1
goto :eof

:check_python
echo.
echo [3/7] Checking Python...
call :try_set_py311

%PY_CMD% --version >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :python_found

where python >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "PY_CMD=python"
    goto :python_found
)

echo [INFO] Python not found. Attempting install...
if "%HAS_WINGET%"=="1" (
    winget install -e --id Python.Python.3.11 --accept-package-agreements --accept-source-agreements --silent
)

call :try_set_py311

%PY_CMD% --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    where python >nul 2>&1
    if %ERRORLEVEL% EQU 0 set "PY_CMD=python"
)

%PY_CMD% --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Python 3.8+ missing and auto-install failed.
    set /a HAS_ERRORS+=1
    goto :eof
)

:python_found
for /f "delims=" %%p in ('%PY_CMD% --version 2^>^&1') do echo [OK] %%p
echo [OK] Using interpreter: %PY_CMD%

for /f "delims=" %%v in ('%PY_CMD% -c "import sys; print(str(sys.version_info[0])+'.'+str(sys.version_info[1]))" 2^>nul') do set "PY_VER=%%v"
for /f "tokens=1,2 delims=." %%a in ("%PY_VER%") do (
    set "PY_MAJOR=%%a"
    set "PY_MINOR=%%b"
)

if "%PY_MAJOR%"=="3" (
    if %PY_MINOR% GEQ 13 (
        echo [WARN] Python %PY_VER% detected. InsightFace wheels are more reliable on Python 3.11/3.12.
        if "%HAS_WINGET%"=="1" (
            echo [INFO] Attempting to install Python 3.11 for compatibility...
            winget install -e --id Python.Python.3.11 --accept-package-agreements --accept-source-agreements --silent
            call :try_set_py311
            if /I not "%PY_CMD%"=="python" (
                for /f "delims=" %%p in ('%PY_CMD% --version 2^>^&1') do echo [OK] Switched interpreter: %%p
                for /f "delims=" %%v in ('%PY_CMD% -c "import sys; print(str(sys.version_info[0])+'.'+str(sys.version_info[1]))" 2^>nul') do set "PY_VER=%%v"
            ) else (
                if "%PY_VER%"=="3.13" echo [WARN] Python 3.11 was not detected after install attempt.
                if "%PY_VER%"=="3.14" echo [WARN] Python 3.11 was not detected after install attempt.
                set /a HAS_WARNINGS+=1
            )
        )
        if "%PY_VER%"=="3.13" set /a HAS_WARNINGS+=1
        if "%PY_VER%"=="3.14" set /a HAS_WARNINGS+=1
    )
)

:python_pip
%PY_CMD% -m pip --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] pip is available.
    goto :eof
)

echo [INFO] pip missing. Attempting ensurepip...
%PY_CMD% -m ensurepip --upgrade >nul 2>&1
%PY_CMD% -m pip --version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] pip setup failed.
    set /a HAS_ERRORS+=1
    goto :eof
)
echo [OK] pip installed.
goto :eof

:try_set_py311
where py >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    py -3.11 --version >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set "PY_CMD=py -3.11"
        goto :eof
    )
)

if exist "%LocalAppData%\Programs\Python\Python311\python.exe" (
    set "PY_CMD="%LocalAppData%\Programs\Python\Python311\python.exe""
    goto :eof
)

if exist "!PF64!\Python311\python.exe" (
    set "PY_CMD="!PF64!\Python311\python.exe""
    goto :eof
)

if exist "!PF86!\Python311\python.exe" (
    set "PY_CMD="!PF86!\Python311\python.exe""
    goto :eof
)

goto :eof

:check_python_packages
echo.
echo [4/7] Checking Python biometric packages...

call :ensure_py_pkg "onnxruntime" "import onnxruntime"
call :ensure_py_pkg "opencv-python" "import cv2"
call :ensure_py_pkg "numpy" "import numpy"
call :check_optional_insightface

goto :eof

:ensure_py_pkg
set "PKG_NAME=%~1"
set "PKG_IMPORT=%~2"
%PY_CMD% -c "%PKG_IMPORT%" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] %PKG_NAME%
    goto :eof
)

if /I "%PKG_NAME%"=="insightface" (
    if "%PY_MAJOR%"=="3" if %PY_MINOR% GEQ 13 (
        call :try_set_py311
        for /f "delims=" %%v in ('%PY_CMD% -c "import sys; print(str(sys.version_info[0])+'.'+str(sys.version_info[1]))" 2^>nul') do set "PY_VER=%%v"
        for /f "tokens=1,2 delims=." %%a in ("%PY_VER%") do (
            set "PY_MAJOR=%%a"
            set "PY_MINOR=%%b"
        )
        if "%PY_MAJOR%"=="3" if %PY_MINOR% GEQ 13 (
            echo [ERROR] InsightFace is not reliable on Python %PY_VER% in this setup.
            echo [HINT] Install Python 3.11 and ensure either "py -3.11" or Python311\python.exe exists.
            echo [HINT] Then re-run this script.
            set /a HAS_ERRORS+=1
            goto :eof
        )
    )
)

echo [INFO] Installing %PKG_NAME% ...
%PY_CMD% -m pip install %PKG_NAME% --upgrade
%PY_CMD% -c "%PKG_IMPORT%" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] %PKG_NAME% installed.
) else (
    if /I "%PKG_NAME%"=="insightface" (
        call :ensure_msvc_build_tools
        if %HAS_MSVC% EQU 1 (
            echo [INFO] Retrying insightface install after C++ Build Tools check...
            %PY_CMD% -m pip install insightface --upgrade
            %PY_CMD% -c "import insightface" >nul 2>&1
            if %ERRORLEVEL% EQU 0 (
                echo [OK] insightface installed after Build Tools setup.
                goto :eof
            )
        )
        echo [HINT] If you're on Python 3.13/3.14, install/use Python 3.11 and re-run this script.
        echo [HINT] If build errors mention MSVC, install: Microsoft C++ Build Tools.
    )
    echo [ERROR] Failed to install %PKG_NAME%.
    set /a HAS_ERRORS+=1
)
goto :eof

:check_optional_insightface
%PY_CMD% -c "import insightface" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] insightface ^(optional backend^) is installed.
    goto :eof
)

echo [INFO] insightface not installed. Using no-build fallback backend ^(opencv + numpy^).
echo [INFO] If you want native InsightFace later, install manually when compiler toolchain is ready.
goto :eof

:ensure_msvc_build_tools
if %HAS_MSVC% EQU 1 goto :eof

where cl >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "HAS_MSVC=1"
    echo [OK] Microsoft C++ toolchain detected on PATH.
    goto :eof
)

if exist "!PF86!\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC" (
    set "HAS_MSVC=1"
    echo [OK] Microsoft C++ Build Tools detected.
    goto :eof
)

echo [WARN] Microsoft C++ Build Tools not detected.
set /a HAS_WARNINGS+=1

if not "%HAS_WINGET%"=="1" goto :msvc_install_failed

echo [INFO] Installing Microsoft C++ Build Tools (UAC prompt may appear)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$cmd='install -e --id Microsoft.VisualStudio.2022.BuildTools --accept-package-agreements --accept-source-agreements --override ""--wait --quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended --norestart""'; Start-Process -FilePath 'winget' -ArgumentList $cmd -Verb RunAs -Wait"

if exist "!PF86!\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC" (
    set "HAS_MSVC=1"
    echo [OK] Microsoft C++ Build Tools installed.
    goto :eof
)

where cl >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "HAS_MSVC=1"
    echo [OK] Microsoft C++ toolchain installed and available.
    goto :eof
)

:msvc_install_failed

echo [WARN] Could not auto-install Microsoft C++ Build Tools.
echo [HINT] Install manually: https://visualstudio.microsoft.com/visual-cpp-build-tools/
goto :eof

:check_insightface_cache
echo.
echo [5/7] Checking InsightFace model cache...
if exist "%USERPROFILE%\.insightface" (
    echo [OK] InsightFace cache exists: %USERPROFILE%\.insightface
) else (
    echo [WARN] InsightFace model cache not found yet.
    echo        First run will download models ^(~100-150MB^).
    set /a HAS_WARNINGS+=1
)
goto :eof

:check_database_port
echo.
echo [6/7] Checking MySQL availability on 127.0.0.1:3306...
powershell -NoProfile -Command "$ok=(Test-NetConnection -ComputerName 127.0.0.1 -Port 3306 -WarningAction SilentlyContinue).TcpTestSucceeded; if($ok){exit 0}else{exit 1}" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [OK] MySQL port 3306 is reachable.
) else (
    echo [WARN] MySQL port 3306 is not reachable.
    echo        Start your MySQL service ^(WAMP/XAMPP/MySQL80^) before running the app.
    set /a HAS_WARNINGS+=1
)
goto :eof

:check_local_config
echo.
echo [7/7] Checking local config...
if exist "%ROOT%config\application.local.properties" (
    echo [OK] Found config\application.local.properties
) else (
    echo [WARN] config\application.local.properties not found.
    echo        Database/mail/env values may be missing.
    set /a HAS_WARNINGS+=1
)
goto :eof

:print_manual_notes
echo.
echo Manual checks ^(not auto-installable by script^):
echo  - Camera permission/access in Windows Settings
echo  - Valid SMTP credentials if you use OTP/password reset email
echo  - Existing DB schema/data in your MySQL syndicati database
goto :eof
