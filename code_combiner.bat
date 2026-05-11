@echo off
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path 'src\main\java' -Recurse -Filter *.java | ForEach-Object { write-output ' ' ; write-output '// =========================================='; write-output '// FILE: $($_.FullName)'; write-output '// =========================================='; write-output ' ' ; Get-Content $_.FullName } > tmes_full_code.txt"
echo Done! Created tmes_full_code.txt
pause