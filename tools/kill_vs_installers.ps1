Get-Process setup,vs_BuildTools,vs_setup_bootstrapper -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Write-Host "ELEVATED_KILL_DONE"