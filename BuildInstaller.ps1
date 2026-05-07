Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

# Hide the console host so this script behaves like a GUI app.
Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class Win32Console {
    [DllImport("kernel32.dll")]
    public static extern IntPtr GetConsoleWindow();

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
}
"@

$consoleHandle = [Win32Console]::GetConsoleWindow()
if ($consoleHandle -ne [IntPtr]::Zero) {
    [Win32Console]::ShowWindow($consoleHandle, 0) | Out-Null
}

# Set security protocol for downloads
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ProgressPreference = 'SilentlyContinue'

$scriptPath = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptPath

$global:buildStopwatch = New-Object System.Diagnostics.Stopwatch
$global:logLineCount = 0
$global:lastInstallerPath = ""

# Create main form
$form = New-Object System.Windows.Forms.Form
$form.Text = "Syndicati - Installer Builder"
$form.Size = New-Object System.Drawing.Size(920, 680)
$form.StartPosition = "CenterScreen"
$form.FormBorderStyle = "FixedDialog"
$form.MaximizeBox = $false
$form.BackColor = [System.Drawing.Color]::FromArgb(30, 30, 35)

# Title label
$titleLabel = New-Object System.Windows.Forms.Label
$titleLabel.Text = "Syndicati"
$titleLabel.Font = New-Object System.Drawing.Font("Segoe UI", 22, [System.Drawing.FontStyle]::Bold)
$titleLabel.ForeColor = [System.Drawing.Color]::White
$titleLabel.AutoSize = $true
$titleLabel.Location = New-Object System.Drawing.Point(30, 18)
$form.Controls.Add($titleLabel)

# Subtitle
$subtitleLabel = New-Object System.Windows.Forms.Label
$subtitleLabel.Text = "Professional Installer Builder and Diagnostic Console"
$subtitleLabel.Font = New-Object System.Drawing.Font("Segoe UI", 10)
$subtitleLabel.ForeColor = [System.Drawing.Color]::FromArgb(150, 150, 150)
$subtitleLabel.AutoSize = $true
$subtitleLabel.Location = New-Object System.Drawing.Point(34, 58)
$form.Controls.Add($subtitleLabel)

# Status label
$statusLabel = New-Object System.Windows.Forms.Label
$statusLabel.Text = "Ready to build installer"
$statusLabel.Font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
$statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(100, 200, 100)
$statusLabel.AutoSize = $true
$statusLabel.Location = New-Object System.Drawing.Point(30, 95)
$form.Controls.Add($statusLabel)

# Step label
$stepLabel = New-Object System.Windows.Forms.Label
$stepLabel.Text = "Step: Idle"
$stepLabel.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$stepLabel.ForeColor = [System.Drawing.Color]::FromArgb(190, 190, 190)
$stepLabel.AutoSize = $true
$stepLabel.Location = New-Object System.Drawing.Point(30, 120)
$form.Controls.Add($stepLabel)

# Elapsed label
$elapsedLabel = New-Object System.Windows.Forms.Label
$elapsedLabel.Text = "Elapsed: 00:00"
$elapsedLabel.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$elapsedLabel.ForeColor = [System.Drawing.Color]::FromArgb(170, 170, 170)
$elapsedLabel.AutoSize = $true
$elapsedLabel.Location = New-Object System.Drawing.Point(230, 120)
$form.Controls.Add($elapsedLabel)

# Log count label
$logCountLabel = New-Object System.Windows.Forms.Label
$logCountLabel.Text = "Log lines: 0"
$logCountLabel.Font = New-Object System.Drawing.Font("Segoe UI", 9)
$logCountLabel.ForeColor = [System.Drawing.Color]::FromArgb(170, 170, 170)
$logCountLabel.AutoSize = $true
$logCountLabel.Location = New-Object System.Drawing.Point(360, 120)
$form.Controls.Add($logCountLabel)

# Progress bar
$progressBar = New-Object System.Windows.Forms.ProgressBar
$progressBar.Location = New-Object System.Drawing.Point(30, 145)
$progressBar.Size = New-Object System.Drawing.Size(850, 24)
$progressBar.Style = "Continuous"
$form.Controls.Add($progressBar)

# Log box
$detailsBox = New-Object System.Windows.Forms.RichTextBox
$detailsBox.Location = New-Object System.Drawing.Point(30, 182)
$detailsBox.Size = New-Object System.Drawing.Size(850, 410)
$detailsBox.Multiline = $true
$detailsBox.ScrollBars = "Vertical"
$detailsBox.ReadOnly = $true
$detailsBox.BackColor = [System.Drawing.Color]::FromArgb(20, 20, 25)
$detailsBox.ForeColor = [System.Drawing.Color]::FromArgb(200, 200, 200)
$detailsBox.Font = New-Object System.Drawing.Font("Consolas", 9)
$detailsBox.DetectUrls = $false
$form.Controls.Add($detailsBox)

# Build button
$buildButton = New-Object System.Windows.Forms.Button
$buildButton.Text = "Build Installer"
$buildButton.Location = New-Object System.Drawing.Point(30, 605)
$buildButton.Size = New-Object System.Drawing.Size(210, 36)
$buildButton.BackColor = [System.Drawing.Color]::FromArgb(220, 50, 50)
$buildButton.ForeColor = [System.Drawing.Color]::White
$buildButton.FlatStyle = "Flat"
$buildButton.Font = New-Object System.Drawing.Font("Segoe UI", 10, [System.Drawing.FontStyle]::Bold)
$form.Controls.Add($buildButton)

# Copy logs button
$copyLogsButton = New-Object System.Windows.Forms.Button
$copyLogsButton.Text = "Copy Logs"
$copyLogsButton.Location = New-Object System.Drawing.Point(255, 605)
$copyLogsButton.Size = New-Object System.Drawing.Size(140, 36)
$copyLogsButton.BackColor = [System.Drawing.Color]::FromArgb(60, 90, 160)
$copyLogsButton.ForeColor = [System.Drawing.Color]::White
$copyLogsButton.FlatStyle = "Flat"
$copyLogsButton.Font = New-Object System.Drawing.Font("Segoe UI", 9, [System.Drawing.FontStyle]::Bold)
$form.Controls.Add($copyLogsButton)

# Save logs button
$saveLogsButton = New-Object System.Windows.Forms.Button
$saveLogsButton.Text = "Save Logs"
$saveLogsButton.Location = New-Object System.Drawing.Point(410, 605)
$saveLogsButton.Size = New-Object System.Drawing.Size(140, 36)
$saveLogsButton.BackColor = [System.Drawing.Color]::FromArgb(60, 130, 95)
$saveLogsButton.ForeColor = [System.Drawing.Color]::White
$saveLogsButton.FlatStyle = "Flat"
$saveLogsButton.Font = New-Object System.Drawing.Font("Segoe UI", 9, [System.Drawing.FontStyle]::Bold)
$form.Controls.Add($saveLogsButton)

# Open output button
$openOutputButton = New-Object System.Windows.Forms.Button
$openOutputButton.Text = "Open Output"
$openOutputButton.Location = New-Object System.Drawing.Point(565, 605)
$openOutputButton.Size = New-Object System.Drawing.Size(140, 36)
$openOutputButton.BackColor = [System.Drawing.Color]::FromArgb(95, 95, 95)
$openOutputButton.ForeColor = [System.Drawing.Color]::White
$openOutputButton.FlatStyle = "Flat"
$openOutputButton.Font = New-Object System.Drawing.Font("Segoe UI", 9, [System.Drawing.FontStyle]::Bold)
$openOutputButton.Enabled = $false
$form.Controls.Add($openOutputButton)

function Format-Duration([TimeSpan]$span) {
    return ("{0:D2}:{1:D2}:{2:D2}" -f [int]$span.TotalHours, $span.Minutes, $span.Seconds)
}

function Log($message, [string]$level = "INFO") {
    $timestamp = Get-Date -Format "HH:mm:ss"
    $line = "[$timestamp] [$level] $message"
    $detailsBox.AppendText("$line`r`n")
    $detailsBox.SelectionStart = $detailsBox.Text.Length
    $detailsBox.ScrollToCaret()
    $global:logLineCount++
    $logCountLabel.Text = "Log lines: $global:logLineCount"
    if ($global:buildStopwatch.IsRunning) {
        $elapsedLabel.Text = "Elapsed: $(Format-Duration $global:buildStopwatch.Elapsed)"
    }
    [System.Windows.Forms.Application]::DoEvents()
}

function Log-Tail($title, $content) {
    if (-not $content) { return }
    $allLines = $content -split "`r?`n" | Where-Object { $_.Trim() -ne "" }
    if ($allLines.Count -eq 0) { return }
    $tailLines = $allLines | Select-Object -Last 12
    Log "$title (last $($tailLines.Count) lines):"
    foreach ($line in $tailLines) {
        Log "  $line"
    }
}

function UpdateStatus($message, $progress, [string]$step = "") {
    $statusLabel.Text = $message
    if ($step -and $step.Trim().Length -gt 0) {
        $stepLabel.Text = "Step: $step"
    }
    $clamped = [Math]::Max(0, [Math]::Min(100, [int]$progress))
    $progressBar.Value = $clamped
    if ($global:buildStopwatch.IsRunning) {
        $elapsedLabel.Text = "Elapsed: $(Format-Duration $global:buildStopwatch.Elapsed)"
    }
    [System.Windows.Forms.Application]::DoEvents()
}

function Build-Installer {
    $buildButton.Enabled = $false
    $openOutputButton.Enabled = $false
    $detailsBox.Clear()
    $global:logLineCount = 0
    $logCountLabel.Text = "Log lines: 0"
    $global:lastInstallerPath = ""
    $global:buildStopwatch.Reset()
    $global:buildStopwatch.Start()
    $statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(100, 200, 100)
    UpdateStatus "Initializing build..." 1 "Initialize"
    Log "Build session started from $scriptPath"
    Log "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
    
    try {
        # Create directories
        if (!(Test-Path "tools")) { New-Item -ItemType Directory -Path "tools" -Force | Out-Null }
        if (!(Test-Path "installer")) { New-Item -ItemType Directory -Path "installer" -Force | Out-Null }
        if (!(Test-Path "dist\app")) { New-Item -ItemType Directory -Path "dist\app" -Force | Out-Null }
        if (!(Test-Path "installer-output")) { New-Item -ItemType Directory -Path "installer-output" -Force | Out-Null }
        
        # Step 1: Java
        UpdateStatus "Checking Java..." 5 "Java"
        $javaDir = "$scriptPath\tools\jdk-25"
        $javaExe = "$javaDir\bin\java.exe"
        
        if (!(Test-Path $javaExe)) {
            Log "Downloading Java 25... (this may take a few minutes)"
            $javaUrl = "https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse"
            Invoke-WebRequest -Uri $javaUrl -OutFile "$scriptPath\tools\jdk.zip" -UseBasicParsing
            
            Log "Extracting Java..."
            Expand-Archive -Path "$scriptPath\tools\jdk.zip" -DestinationPath "$scriptPath\tools\jdk-temp" -Force
            
            Get-ChildItem "$scriptPath\tools\jdk-temp" -Directory | ForEach-Object {
                if (Test-Path $javaDir) { Remove-Item $javaDir -Recurse -Force }
                Move-Item $_.FullName $javaDir
            }
            
            Remove-Item "$scriptPath\tools\jdk-temp" -Recurse -Force -ErrorAction SilentlyContinue
            Remove-Item "$scriptPath\tools\jdk.zip" -Force -ErrorAction SilentlyContinue
        }
        Log "[OK] Java ready"
        UpdateStatus "Java ready" 15 "Java"
        
        $env:JAVA_HOME = $javaDir
        $env:PATH = "$javaDir\bin;$env:PATH"
        
        # Step 2: Maven
        UpdateStatus "Checking Maven..." 20 "Maven"
        $mavenDir = "$scriptPath\tools\maven"
        $mvn = "$mavenDir\bin\mvn.cmd"
        
        if (!(Test-Path $mvn)) {
            Log "Downloading Maven..."
            $mavenUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
            Invoke-WebRequest -Uri $mavenUrl -OutFile "$scriptPath\tools\maven.zip" -UseBasicParsing
            
            Log "Extracting Maven..."
            Expand-Archive -Path "$scriptPath\tools\maven.zip" -DestinationPath "$scriptPath\tools\maven-temp" -Force
            
            Get-ChildItem "$scriptPath\tools\maven-temp" -Directory | ForEach-Object {
                if (Test-Path $mavenDir) { Remove-Item $mavenDir -Recurse -Force }
                Move-Item $_.FullName $mavenDir
            }
            
            Remove-Item "$scriptPath\tools\maven-temp" -Recurse -Force -ErrorAction SilentlyContinue
            Remove-Item "$scriptPath\tools\maven.zip" -Force -ErrorAction SilentlyContinue
        }
        Log "[OK] Maven ready"
        UpdateStatus "Maven ready" 30 "Maven"
        
        # Step 3: Build JAR
        UpdateStatus "Building application..." 35 "Maven Package"
        Log "Building application with Maven..."

        $mvnOut = "$scriptPath\maven-build-out.txt"
        $mvnErr = "$scriptPath\maven-build-err.txt"
        Remove-Item $mvnOut -Force -ErrorAction SilentlyContinue
        Remove-Item $mvnErr -Force -ErrorAction SilentlyContinue

        $process = Start-Process -FilePath $mvn -ArgumentList "clean", "package", "-DskipTests", "-q" -Wait -NoNewWindow -PassThru -RedirectStandardOutput $mvnOut -RedirectStandardError $mvnErr
        $mvnOutContent = if (Test-Path $mvnOut) { Get-Content $mvnOut -Raw } else { "" }
        $mvnErrContent = if (Test-Path $mvnErr) { Get-Content $mvnErr -Raw } else { "" }

        if ($process.ExitCode -ne 0) {
            Log-Tail "Maven stdout" $mvnOutContent
            Log-Tail "Maven stderr" $mvnErrContent
            throw "Maven build failed!"
        }
        Log-Tail "Maven output" $mvnOutContent
        Remove-Item $mvnOut -Force -ErrorAction SilentlyContinue
        Remove-Item $mvnErr -Force -ErrorAction SilentlyContinue
        Log "[OK] Application built"
        UpdateStatus "Application built" 50 "Maven Package"
        
        # Step 4: Prepare distribution
        UpdateStatus "Preparing distribution..." 55 "Package App Image"
        Log "Preparing native app image for distribution..."
        
        if (Test-Path "dist") { Remove-Item "dist" -Recurse -Force }
        New-Item -ItemType Directory -Path "dist\input" -Force | Out-Null
        
        # Copy JAR (prefer shaded final artifact, fallback to any non-original jar)
        $jarCandidates = @(
            @(
                "target\syndicati.jar",
                "target\syndicati-1.0-SNAPSHOT.jar"
            ) | Where-Object { Test-Path $_ }
        )

        if ($jarCandidates.Count -eq 0) {
            $jarCandidates = @(
                Get-ChildItem "target\*.jar" -File -ErrorAction SilentlyContinue |
                    Where-Object { $_.Name -notlike "original-*" } |
                    Sort-Object LastWriteTime -Descending |
                    Select-Object -ExpandProperty FullName
            )
        }

        if ($jarCandidates.Count -eq 0) {
            throw "No build JAR found in target. Expected target\\syndicati.jar or another packaged JAR."
        }

        $selectedJar = $jarCandidates | Select-Object -First 1
        Copy-Item $selectedJar "dist\input\Syndicati.jar" -Force
        Log "[OK] JAR copied from $selectedJar"

        $jpackage = "$javaDir\bin\jpackage.exe"
        if (!(Test-Path $jpackage)) {
            throw "jpackage.exe not found in bundled JDK."
        }

        Log "Creating native launcher (Syndicati.exe)..."
        UpdateStatus "Creating native launcher..." 60 "jpackage"

        $appTempRoot = "$scriptPath\dist\app-temp"
        $appImageDir = "$appTempRoot\Syndicati"
        if (Test-Path $appTempRoot) {
            Remove-Item $appTempRoot -Recurse -Force -ErrorAction SilentlyContinue
        }

        $jpackageArgs = @(
            "--type", "app-image",
            "--name", "Syndicati",
            "--input", "$scriptPath\dist\input",
            "--main-jar", "Syndicati.jar",
            "--main-class", "com.syndicati.Launcher",
            "--dest", $appTempRoot,
            "--app-version", "1.0.0",
            "--vendor", "Syndicati",
            "--description", "Syndicati"
        )

        $appIcon = "$scriptPath\installer\app-icon.ico"
        if (Test-Path $appIcon) {
            $jpackageArgs += @("--icon", $appIcon)
        }

        $jpOut = "$scriptPath\jpackage-out.txt"
        $jpErr = "$scriptPath\jpackage-err.txt"
        Remove-Item $jpOut -Force -ErrorAction SilentlyContinue
        Remove-Item $jpErr -Force -ErrorAction SilentlyContinue

        # Use direct invocation with argument splatting so values containing spaces
        # (for example --description) are passed as a single argument.
        $jpRawOutput = & $jpackage @jpackageArgs 2>&1
        $jpExitCode = $LASTEXITCODE
        if ($jpRawOutput) {
            $jpText = ($jpRawOutput | Out-String).Trim()
            if ($jpText) {
                Set-Content -Path $jpOut -Value $jpText -Encoding ASCII
            }
        }

        if ($jpExitCode -ne 0) {
            if (Test-Path $jpOut) {
                $jpOutContent = Get-Content $jpOut -Raw
                if ($jpOutContent) { Log "jpackage error: $jpOutContent" }
            }
            throw "jpackage failed with exit code $jpExitCode."
        }

        if (Test-Path $jpOut) {
            $jpOutContent = Get-Content $jpOut -Raw
            Log-Tail "jpackage output" $jpOutContent
        }

        if (!(Test-Path $appImageDir)) {
            throw "jpackage output not found at $appImageDir"
        }

        Remove-Item $jpOut -Force -ErrorAction SilentlyContinue
        Remove-Item $jpErr -Force -ErrorAction SilentlyContinue

        New-Item -ItemType Directory -Path "dist\app" -Force | Out-Null
        Copy-Item "$appImageDir\*" "dist\app\" -Recurse -Force
        Remove-Item "$scriptPath\dist\app-temp" -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item "$scriptPath\dist\input" -Recurse -Force -ErrorAction SilentlyContinue
        
        Log "[OK] Distribution prepared"
        UpdateStatus "Distribution prepared" 70 "Package App Image"
        
        # Step 5: Inno Setup
        UpdateStatus "Checking Inno Setup..." 75 "Inno Setup"
        $innoDir = "$scriptPath\tools\innosetup"
        $iscc = "$innoDir\ISCC.exe"
        
        if (!(Test-Path $iscc)) {
            Log "Downloading Inno Setup..."
            $innoInstaller = "$scriptPath\tools\innosetup-installer.exe"
            Invoke-WebRequest -Uri "https://files.jrsoftware.org/is/6/innosetup-6.2.2.exe" -OutFile $innoInstaller -UseBasicParsing
            
            Log "Installing Inno Setup (please wait)..."
            # Run installer with explicit path
            $innoArgs = "/VERYSILENT /SUPPRESSMSGBOXES /NORESTART /DIR=`"$innoDir`""
            $process = Start-Process -FilePath $innoInstaller -ArgumentList $innoArgs -Wait -PassThru
            
            # Wait for installation to complete
            Start-Sleep -Seconds 10
            
            # Check if installation succeeded
            if (!(Test-Path $iscc)) {
                # Try system-wide location
                $systemInno = "${env:ProgramFiles(x86)}\Inno Setup 6\ISCC.exe"
                if (Test-Path $systemInno) {
                    $iscc = $systemInno
                    Log "Using system Inno Setup installation"
                } else {
                    throw "Inno Setup installation failed. Please install manually from https://jrsoftware.org/isdl.php"
                }
            }
            
            Remove-Item $innoInstaller -Force -ErrorAction SilentlyContinue
        }
        Log "[OK] Inno Setup ready"
        UpdateStatus "Inno Setup ready" 85 "Inno Setup"
        
        # Step 6: Create icon if needed
        if (!(Test-Path "installer\app-icon.ico")) {
            Log "Note: No custom icon found. Using default."
        }
        
        # Step 7: Build installer
        UpdateStatus "Building installer..." 90 "Compile Installer"
        Log "Creating installer..."
        
        # Build full path to ISS file
        $issFile = "$scriptPath\installer\syndicati-installer.iss"
        Log "Using: $iscc"
        Log "Script: $issFile"
        
        $process = Start-Process -FilePath $iscc -ArgumentList "`"$issFile`"" -Wait -NoNewWindow -PassThru -RedirectStandardOutput "$scriptPath\inno-out.txt" -RedirectStandardError "$scriptPath\inno-err.txt"
        
        if (Test-Path "$scriptPath\inno-err.txt") {
            $errContent = Get-Content "$scriptPath\inno-err.txt" -Raw
            if ($errContent) { Log "Inno Error: $errContent" }
        }

        if (Test-Path "$scriptPath\inno-out.txt") {
            $innoOutContent = Get-Content "$scriptPath\inno-out.txt" -Raw
            Log-Tail "Inno output" $innoOutContent
        }
        
        if ($process.ExitCode -ne 0) {
            if (Test-Path "$scriptPath\inno-out.txt") {
                $outContent = Get-Content "$scriptPath\inno-out.txt" -Raw
                if ($outContent) { Log $outContent }
            }
            throw "Installer creation failed with exit code $($process.ExitCode)!"
        }
        
        Remove-Item "$scriptPath\inno-out.txt" -Force -ErrorAction SilentlyContinue
        Remove-Item "$scriptPath\inno-err.txt" -Force -ErrorAction SilentlyContinue
        
        UpdateStatus "Installer created successfully!" 100 "Completed"
        $global:buildStopwatch.Stop()
        $elapsedLabel.Text = "Elapsed: $(Format-Duration $global:buildStopwatch.Elapsed)"
        Log ""
        Log "========================================="
        Log "SUCCESS! Installer created!"
        Log "========================================="
        $global:lastInstallerPath = "$scriptPath\installer-output\Syndicati-Setup-1.0.0.exe"
        Log "Location: $global:lastInstallerPath"
        $openOutputButton.Enabled = $true
        
        $statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(100, 255, 100)
        
        # Open output folder
        Start-Process "explorer.exe" -ArgumentList "installer-output"
        
    } catch {
        $global:buildStopwatch.Stop()
        $elapsedLabel.Text = "Elapsed: $(Format-Duration $global:buildStopwatch.Elapsed)"
        $statusLabel.Text = "Error: $($_.Exception.Message)"
        $statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(255, 100, 100)
        $stepLabel.Text = "Step: Failed"
        Log "ERROR: $($_.Exception.Message)" "ERROR"
        Log "Tip: Use Save Logs to attach a full log when reporting issues." "WARN"
    }
    
    $buildButton.Enabled = $true
    $buildButton.Text = "Build Again"
}

$buildButton.Add_Click({ Build-Installer })
$copyLogsButton.Add_Click({
    if ($detailsBox.TextLength -gt 0) {
        [System.Windows.Forms.Clipboard]::SetText($detailsBox.Text)
        [System.Windows.Forms.MessageBox]::Show("Logs copied to clipboard.", "Syndicati", [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
    }
})

$saveLogsButton.Add_Click({
    $logDir = "$scriptPath\installer-output"
    if (!(Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }
    $logFile = "$logDir\builder-log-$(Get-Date -Format 'yyyyMMdd-HHmmss').txt"
    Set-Content -Path $logFile -Value $detailsBox.Text -Encoding ASCII
    [System.Windows.Forms.MessageBox]::Show("Log saved to:`r`n$logFile", "Syndicati", [System.Windows.Forms.MessageBoxButtons]::OK, [System.Windows.Forms.MessageBoxIcon]::Information) | Out-Null
})

$openOutputButton.Add_Click({
    if (Test-Path "$scriptPath\installer-output") {
        Start-Process "explorer.exe" -ArgumentList "installer-output"
    }
})

$form.ShowDialog()
