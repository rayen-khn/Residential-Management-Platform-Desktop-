Option Explicit

Dim shell, fso, scriptDir, ps1Path, command
Set shell = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

scriptDir = fso.GetParentFolderName(WScript.ScriptFullName)
ps1Path = scriptDir & "\BuildInstaller.ps1"

If Not fso.FileExists(ps1Path) Then
    MsgBox "BuildInstaller.ps1 was not found next to this launcher.", vbCritical, "Syndicati"
    WScript.Quit 1
End If

command = "powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File """ & ps1Path & """"

' 0 = hidden window, False = do not wait
shell.Run command, 0, False
