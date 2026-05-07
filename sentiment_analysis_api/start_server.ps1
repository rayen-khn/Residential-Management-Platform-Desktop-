# Sentiment Analysis with Langchain & Groq
# Server Startup Script for PowerShell (Windows)
# More reliable than batch files!

Write-Host ""
Write-Host "======================================"
Write-Host "Sentiment Analysis Server"
Write-Host "Powered by Groq & Langchain"
Write-Host "======================================"
Write-Host ""

# Check if venv exists
if (-not (Test-Path "venv")) {
    Write-Host "Virtual environment not found!"
    Write-Host "Creating virtual environment..."
    python -m venv venv
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Error: Failed to create virtual environment"
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    Write-Host "[OK] Virtual environment created"
    Write-Host ""
    
    Write-Host "Activating virtual environment..."
    & ".\venv\Scripts\Activate.ps1"
    
    Write-Host "Installing packages (2-3 minutes)..."
    Write-Host "Please wait..."
    Write-Host ""
    
    pip install -r requirements.txt
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Error: Package installation failed"
        Read-Host "Press Enter to exit"
        exit 1
    }
    
    Write-Host ""
    Write-Host "[OK] All packages installed"
    Write-Host ""
} else {
    # Activate virtual environment
    Write-Host "Activating virtual environment..."
    & ".\venv\Scripts\Activate.ps1"
    
    # Check if Flask is installed
    Write-Host "Checking packages..."
    $flaskInstalled = python -c "import flask" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "Packages not installed or incomplete!"
        Write-Host "Installing packages (2-3 minutes)..."
        Write-Host "Please wait..."
        Write-Host ""
        
        pip install -r requirements.txt
        
        if ($LASTEXITCODE -ne 0) {
            Write-Host ""
            Write-Host "Error: Package installation failed"
            Read-Host "Press Enter to exit"
            exit 1
        }
        
        Write-Host ""
        Write-Host "[OK] All packages installed"
        Write-Host ""
    } else {
        Write-Host "[OK] Packages already installed"
        Write-Host ""
    }
}

# Check if .env exists
if (-not (Test-Path ".env")) {
    Write-Host ""
    Write-Host "Error: .env file not found"
    Write-Host "Please create .env with GROQ_API_KEY"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "======================================"
Write-Host "Starting Flask Server"
Write-Host "======================================"
Write-Host ""
Write-Host "Server URL: http://localhost:5000"
Write-Host ""
Write-Host "Press Ctrl+C to stop the server"
Write-Host ""
Write-Host "Tip: First analysis may take 5-10 seconds"
Write-Host "     Subsequent requests will be faster"
Write-Host ""

# Run Flask app
try {
    python app.py
} catch {
    Write-Host ""
    Write-Host "Error occurred: $_"
} finally {
    Write-Host ""
    Write-Host "Server stopped."
    Write-Host ""
    Read-Host "Press Enter to close this window"
}
