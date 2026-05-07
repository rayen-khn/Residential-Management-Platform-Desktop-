using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;

class Program {
    static void Main() {
        string exePath = Assembly.GetExecutingAssembly().Location;
        string dir = Path.GetDirectoryName(exePath);
        string ps1Path = Path.Combine(dir, "BuildInstaller.ps1");
        
        ProcessStartInfo psi = new ProcessStartInfo();
        psi.FileName = "powershell.exe";
        psi.Arguments = "-ExecutionPolicy Bypass -File \"" + ps1Path + "\"";
        psi.WorkingDirectory = dir;
        psi.UseShellExecute = false;
        Process.Start(psi);
    }
}
