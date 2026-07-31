function Extract-Strings {
    param([string]$Path)
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $sb = New-Object System.Text.StringBuilder
    $current = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) {
        if ($b -ge 32 -and $b -lt 127) {
            [void]$current.Append([char]$b)
        } else {
            if ($current.Length -ge 3) {
                [void]$sb.AppendLine($current.ToString())
            }
            [void]$current.Clear()
        }
    }
    if ($current.Length -ge 3) { [void]$sb.AppendLine($current.ToString()) }
    return $sb.ToString()
}

$dir = "E:\Мои Сурсы\harmony\lb_classes"
$out = "E:\Мои Сурсы\harmony\lb_classes\strings_out"
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Path $out -Force | Out-Null

Get-ChildItem -Path $dir -Filter "*.class" -Recurse | ForEach-Object {
    $rel = $_.FullName.Substring($dir.Length + 1)
    $strings = Extract-Strings $_.FullName
    $name = $rel -replace '[\\]', '_' -replace '\.class$', '.txt'
    $destFile = Join-Path $out $name
    $dir2 = Split-Path $destFile -Parent
    if (!(Test-Path $dir2)) { New-Item -ItemType Directory -Path $dir2 -Force | Out-Null }
    $strings | Out-File -FilePath $destFile -Encoding UTF8
}
Write-Output ("Extracted strings to " + $out)
