# PiDev Application Launcher (PowerShell)
# This script launches the JavaFX application using Maven

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "PiDev Application Launcher"

Write-Host "====================================" -ForegroundColor Cyan
Write-Host "   Starting PiDev Application" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host ""

# Change to script directory
Set-Location $PSScriptRoot

# Function to find Maven
function Find-Maven {
    # Check if mvn is in PATH
    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        Write-Host "Found Maven in PATH: $($mvnCommand.Source)" -ForegroundColor Green
        return $mvnCommand.Source
    }
    
    Write-Host "Searching for IntelliJ bundled Maven..." -ForegroundColor Yellow
    
    # IntelliJ IDEA bundled Maven locations
    $intellijPaths = @(
        "$env:ProgramFiles\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:ProgramFiles(x86)\JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\IDEA-U\*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:LOCALAPPDATA\JetBrains\Toolbox\apps\IDEA-C\*\plugins\maven\lib\maven3\bin\mvn.cmd",
        "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-*\bin\mvn.cmd"
    )
    
    foreach ($pattern in $intellijPaths) {
        $found = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            Write-Host "Found IntelliJ Maven: $($found.FullName)" -ForegroundColor Green
            return $found.FullName
        }
    }
    
    Write-Host "Checking common Maven installations..." -ForegroundColor Yellow
    
    # Check common Maven installation paths
    $commonPaths = @(
        "C:\Program Files\Apache\Maven\bin\mvn.cmd",
        "C:\Program Files\apache-maven*\bin\mvn.cmd",
        "C:\apache-maven*\bin\mvn.cmd",
        "C:\Maven\bin\mvn.cmd",
        "$env:MAVEN_HOME\bin\mvn.cmd"
    )
    
    foreach ($pattern in $commonPaths) {
        $found = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            Write-Host "Found Maven: $($found.FullName)" -ForegroundColor Green
            return $found.FullName
        }
    }
    
    return $null
}

try {
    # Check for Maven wrapper first
    if (Test-Path "mvnw.cmd") {
        Write-Host "Using Maven Wrapper..." -ForegroundColor Green
        & .\mvnw.cmd clean javafx:run
    }
    else {
        # Find Maven
        $mavenPath = Find-Maven
        
        if ($null -eq $mavenPath) {
            Write-Host ""
            Write-Host "ERROR: Maven not found!" -ForegroundColor Red
            Write-Host "Please install Maven or add it to your PATH." -ForegroundColor Yellow
            Write-Host ""
            Read-Host "Press Enter to exit"
            exit 1
        }
        
        Write-Host "Using Maven from: $mavenPath" -ForegroundColor Green
        & $mavenPath clean javafx:run
    }
    
    if ($LASTEXITCODE -ne 0) {
        throw "Application failed to start"
    }
}
catch {
    Write-Host ""
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}
