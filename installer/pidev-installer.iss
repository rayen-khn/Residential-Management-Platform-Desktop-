; Syndicati - Inno Setup Installer Script
; This creates a professional Windows installer with progress bar

#define MyAppName "Syndicati"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "Syndicati"
#define MyAppURL "https://github.com/syndicati"
#define MyAppExeName "Syndicati.bat"

[Setup]
; Basic installer settings
AppId={{A1B2C3D4-E5F6-7890-ABCD-EF1234567890}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}

; Installation directories
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes

; Output settings
OutputDir=..\installer-output
OutputBaseFilename=Syndicati-Setup-{#MyAppVersion}
; Commented out - create your own app-icon.ico or remove this line
; SetupIconFile=app-icon.ico
; UninstallDisplayIcon={app}\app-icon.ico

; Compression
Compression=lzma2/ultra64
SolidCompression=yes
LZMAUseSeparateProcess=yes

; Installer appearance
WizardStyle=modern
WizardSizePercent=120
WindowVisible=no
WindowShowCaption=yes
WindowResizable=no

; Privileges
PrivilegesRequired=lowest
PrivilegesRequiredOverridesAllowed=dialog

; Misc
DisableWelcomePage=no
DisableDirPage=no
DisableReadyPage=no
ShowLanguageDialog=auto

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel1=Welcome to {#MyAppName} Setup
WelcomeLabel2=This will install {#MyAppName} {#MyAppVersion} on your computer.%n%nThe application includes its own Java runtime, so no additional software is required.%n%nClick Next to continue.

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
; Main application files - JAR
Source: "..\dist\app\*"; DestDir: "{app}\app"; Flags: ignoreversion recursesubdirs createallsubdirs
; Bundled JRE
Source: "..\dist\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs
; Launcher
Source: "..\dist\Syndicati.bat"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}"

[Code]
// Custom progress during installation
var
  ProgressPage: TOutputProgressWizardPage;

procedure InitializeWizard;
begin
  // Create custom progress page
  ProgressPage := CreateOutputProgressPage('Installing', 'Please wait while {#MyAppName} is being installed...');
end;
