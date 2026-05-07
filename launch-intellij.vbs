Set objShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

' Get the script's directory
strScriptPath = objFSO.GetParentFolderName(WScript.ScriptFullName)

' IntelliJ IDEA executable paths to check
Dim intellijPaths(10)
intellijPaths(0) = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.3\bin\idea64.exe"
intellijPaths(1) = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.2\bin\idea64.exe"
intellijPaths(2) = "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2024.1\bin\idea64.exe"
intellijPaths(3) = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.3\bin\idea64.exe"
intellijPaths(4) = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.2\bin\idea64.exe"
intellijPaths(5) = "C:\Program Files\JetBrains\IntelliJ IDEA 2024.1\bin\idea64.exe"
intellijPaths(6) = objShell.ExpandEnvironmentStrings("%LOCALAPPDATA%") & "\JetBrains\Toolbox\apps\IDEA-U\ch-0\*\bin\idea64.exe"
intellijPaths(7) = objShell.ExpandEnvironmentStrings("%LOCALAPPDATA%") & "\JetBrains\Toolbox\apps\IDEA-C\ch-0\*\bin\idea64.exe"
intellijPaths(8) = "C:\Program Files (x86)\JetBrains\IntelliJ IDEA Community Edition\bin\idea64.exe"
intellijPaths(9) = "C:\Program Files (x86)\JetBrains\IntelliJ IDEA\bin\idea64.exe"

' Find IntelliJ IDEA
ideaPath = ""
For Each path In intellijPaths
    If InStr(path, "*") > 0 Then
        ' Handle wildcard paths (Toolbox)
        basePath = Left(path, InStrRev(path, "\*\") - 1)
        If objFSO.FolderExists(basePath) Then
            Set folder = objFSO.GetFolder(basePath)
            Set subfolders = folder.SubFolders
            For Each subfolder In subfolders
                testPath = subfolder.Path & "\bin\idea64.exe"
                If objFSO.FileExists(testPath) Then
                    ideaPath = testPath
                    Exit For
                End If
            Next
        End If
    Else
        If objFSO.FileExists(path) Then
            ideaPath = path
            Exit For
        End If
    End If
    If ideaPath <> "" Then Exit For
Next

If ideaPath = "" Then
    MsgBox "IntelliJ IDEA not found!" & vbCrLf & vbCrLf & _
           "Please install IntelliJ IDEA or update the paths in the script.", _
           vbCritical, "PiDev Launcher"
    WScript.Quit
End If

' Open IntelliJ with the project
objShell.Run """" & ideaPath & """ """ & strScriptPath & """", 1, False

' Wait a bit for IntelliJ to start
WScript.Sleep 3000

' Try to trigger the run configuration (this may not work reliably)
' Alternative: Just open IntelliJ and let user click Run
MsgBox "IntelliJ IDEA is opening..." & vbCrLf & vbCrLf & _
       "Please click the Run button (green play icon) or press Shift+F10 to start the application.", _
       vbInformation, "PiDev Launcher"
