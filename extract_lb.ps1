$jar = "$env:TEMP\lbnext.jar"
$dst = "$env:TEMP\lbnext"
if (Test-Path $dst) { Remove-Item -Recurse -Force $dst }
New-Item -ItemType Directory -Path $dst -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
$count = 0
foreach ($entry in $zip.Entries) {
    if ($entry.FullName -match 'scaffold' -or $entry.FullName -match 'Scaffold') {
        $outPath = Join-Path $dst $entry.FullName
        $dir = Split-Path $outPath -Parent
        if (!(Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $outPath, $true)
        $count++
    }
}
$zip.Dispose()
Write-Output ("EXTRACTED " + $count + " scaffold files")
# Теперь покажем структуру
Get-ChildItem -Path $dst -Recurse -Filter "*.class" | ForEach-Object { Write-Output $_.FullName.Substring($dst.Length) }
