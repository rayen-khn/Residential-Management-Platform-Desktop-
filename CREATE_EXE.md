# Creating an EXE Launcher for PiDev Application

## Quick Start: Create EXE Now! (Recommended)

### Method 1: Using PowerShell (Easiest)
1. Right-click `create-exe.ps1`
2. Select "Run with PowerShell"
3. Wait a few seconds
4. `PiDevApp.exe` will be created!
5. Double-click `PiDevApp.exe` to launch the app via IntelliJ

### Method 2: Using Batch File
1. Double-click `create-exe.bat`
2. Wait for completion
3. `PiDevApp.exe` will be created!

### What the EXE Does:
- Automatically finds IntelliJ IDEA on your system
- Opens your project in IntelliJ
- Shows a message to click Run button

## Option 2: Manual Conversion (If above doesn't work)

### Using Bat To Exe Converter (Free)
1. Download from: https://www.f2ko.de/en/b2e.php
2. Install and open the application
3. Click "Open" and select `launch-app.bat`
4. Set icon (optional - can use a .ico file)
5. Click "Compile" to create `launch-app.exe`

### Using IExpress (Built into Windows)
1. Press Win+R and type: `iexpress`
2. Select "Create new Self Extraction Directive file"
3. Choose "Extract files and run an installation command"
4. Set package title: "PiDev Application"
5. No confirmation prompt
6. Do not display license
7. Package files: Add `launch-app.bat`
8. Install Program: `launch-app.bat`
9. Set window style: Default
10. Set finish message (optional)
11. Save SED file and create the EXE

## Option 3: Using PS2EXE (For PowerShell Script)

### Install PS2EXE
```powershell
Install-Module -Name ps2exe -Scope CurrentUser
```

### Convert to EXE
```powershell
Invoke-PS2EXE -inputFile ".\launch-app.ps1" -outputFile ".\PiDevApp.exe" -title "PiDev Application" -noConsole
```

## Option 4: Create a Proper Windows Installer with jpackage (Professional)

This creates a native Windows EXE with installer:

```batch
mvn clean package
jpackage ^
  --type exe ^
  --name "PiDev Application" ^
  --app-version "1.0.0" ^
  --vendor "PiDev Team" ^
  --description "PiDev JavaFX Application" ^
  --icon src/main/resources/icon.ico ^
  --input target ^
  --main-jar ami_pidev_java-1.0-SNAPSHOT.jar ^
  --main-class com.pidev.MainApplication ^
  --win-shortcut ^
  --win-menu
```

## Recommended: Quick Launch Shortcut

1. Right-click on `launch-app.bat`
2. Select "Create Shortcut"
3. Right-click the shortcut → Properties
4. Change icon if desired
5. Move shortcut to Desktop
6. Rename to "PiDev Application"

Now you can double-click the shortcut to launch the app!

## Notes
- The batch file will automatically find Maven if it's installed
- If Maven is not in PATH, it will check common installation locations
- The app will compile and run automatically
- First launch may take longer due to Maven dependencies

## Troubleshooting
If the launcher doesn't work:
1. Make sure Maven is installed and in PATH
2. Run `mvn -version` in Command Prompt to verify
3. Check that Java JDK is installed (Java 11 or higher)
4. Try running `launch-app.bat` from Command Prompt to see errors
