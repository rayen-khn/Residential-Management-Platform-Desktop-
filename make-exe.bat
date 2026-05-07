@echo off
title Creating PiDev Installer Builder EXE
cd /d "%~dp0"

echo ================================================================
echo   Creating PiDev-InstallerBuilder.exe
echo ================================================================
echo.

REM Compile the PowerShell script to EXE using built-in Windows tools
echo Creating executable wrapper...

REM Create a VBScript that launches the PowerShell with GUI
(
echo Set objShell = CreateObject^("WScript.Shell"^)
echo strPath = CreateObject^("Scripting.FileSystemObject"^).GetParentFolderName^(WScript.ScriptFullName^)
echo objShell.CurrentDirectory = strPath
echo objShell.Run "powershell.exe -ExecutionPolicy Bypass -WindowStyle Hidden -File """ ^& strPath ^& "\BuildInstaller.ps1""", 0, False
) > "%~dp0BuildInstaller.vbs"

REM Use IExpress to create EXE from VBScript
echo Creating self-extracting executable...

(
echo [Version]
echo Class=IEXPRESS
echo SEDVersion=3
echo [Options]
echo PackagePurpose=InstallApp
echo ShowInstallProgramWindow=1
echo HideExtractAnimation=1
echo UseLongFileName=1
echo InsideCompressed=0
echo CAB_FixedSize=0
echo CAB_ResvCodeSigning=0
echo RebootMode=N
echo InstallPrompt=
echo DisplayLicense=
echo FinishMessage=
echo TargetName=%~dp0PiDev-InstallerBuilder.exe
echo FriendlyName=PiDev Installer Builder
echo AppLaunched=wscript.exe BuildInstaller.vbs
echo PostInstallCmd=^<None^>
echo AdminQuietInstCmd=
echo UserQuietInstCmd=
echo SourceFiles=SourceFiles
echo [Strings]
echo [SourceFiles]
echo SourceFiles0=%~dp0
echo [SourceFiles0]
echo %%FILE0%%=BuildInstaller.vbs
echo %%FILE1%%=BuildInstaller.ps1
echo %%FILE2%%=create-installer.bat
echo %%FILE3%%=pom.xml
) > "%~dp0temp-sed.sed"

REM Run IExpress
iexpress /N /Q "%~dp0temp-sed.sed"

if exist "%~dp0PiDev-InstallerBuilder.exe" (
    echo.
    echo ================================================================
    echo   SUCCESS!
    echo ================================================================
    echo.
    echo Created: PiDev-InstallerBuilder.exe
    echo.
    echo This EXE will:
    echo   - Show a nice GUI with progress bar
    echo   - Auto-download Java, Maven, Inno Setup
    echo   - Build your app into a professional installer
    echo.
    del "%~dp0temp-sed.sed" 2>nul
) else (
    echo.
    echo IExpress method didn't work. Trying alternative...
    
    REM Create a simple batch-to-exe using PowerShell
    powershell -Command "$code = @'
using System;
using System.Diagnostics;
using System.IO;
class Program {
    static void Main() {
        string dir = Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location);
        ProcessStartInfo psi = new ProcessStartInfo();
        psi.FileName = \"powershell.exe\";
        psi.Arguments = \"-ExecutionPolicy Bypass -WindowStyle Hidden -File \\\"\" + Path.Combine(dir, \"BuildInstaller.ps1\") + \"\\\"\";
        psi.WorkingDirectory = dir;
        psi.UseShellExecute = false;
        psi.CreateNoWindow = true;
        Process.Start(psi);
    }
}
'@; Add-Type -TypeDefinition $code -OutputAssembly '%~dp0PiDev-InstallerBuilder.exe' -OutputType ConsoleApplication"
    
    del "%~dp0temp-sed.sed" 2>nul
)

pause
