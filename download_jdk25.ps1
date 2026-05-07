$jdkUrl = 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk'
$outputPath = 'c:\Users\amine\OneDrive\Desktop\Syndicati_Java\tools\jdk-25.zip'
$extractPath = 'c:\Users\amine\OneDrive\Desktop\Syndicati_Java\tools'

Write-Host "Downloading JDK 25 from Adoptium..."
try {
    Invoke-WebRequest -Uri $jdkUrl -OutFile $outputPath -ErrorAction Stop
    Write-Host "Download complete. Extracting..."
    
    Expand-Archive -Path $outputPath -DestinationPath $extractPath -Force
    Write-Host "Archive extracted"
    
    $extracted = Get-ChildItem $extractPath -Directory | Where-Object { $_.Name -like 'jdk-25*' } | Select-Object -First 1
    if ($extracted) {
        Rename-Item $extracted.FullName -NewName 'jdk-25' -Force
        Remove-Item $outputPath
        Write-Host "[OK] JDK 25 installed successfully at $extractPath\jdk-25"
    } else {
        Write-Host "[ERROR] Could not find extracted JDK folder"
    }
} catch {
    Write-Host "[ERROR] Error: $_"
    exit 1
}
