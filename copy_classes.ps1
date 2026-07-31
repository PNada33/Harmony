$src = "$env:TEMP\lbnext\net\ccbluex\liquidbounce\features\module\modules\world\scaffold"
$dst = "E:\Мои Сурсы\harmony\lb_classes"
if (Test-Path $dst) { Remove-Item -Recurse -Force $dst }
Copy-Item -Recurse -Force $src $dst
Write-Output ("Copied to " + $dst)
Get-ChildItem -Recurse $dst -Filter "*.class" | Measure-Object | ForEach-Object { Write-Output ("Files: " + $_.Count) }
