$filePath = 'c:\Users\amine\OneDrive\Desktop\ESPRIT-PIDEV-JAVA-3A53-2526-Syndicati\src\main\java\com\syndicati\views\backend\dashboard\DashboardView.java'
$bytes = [System.IO.File]::ReadAllBytes($filePath)
if ($bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    $newBytes = $bytes[3..($bytes.Length-1)]
    [System.IO.File]::WriteAllBytes($filePath, $newBytes)
    'BOM removed'
} else {
    'No BOM found'
}
