# builds and runs the mario framework from the terminal, so i dont need the ide
#   .\run.ps1          -> PlayLevel (bt agent on original level 10, with visuals)
#   .\run.ps1 -Debug   -> same but prints the bt branch every frame
param([switch]$Debug)

Set-Location $PSScriptRoot
$sources = "bin\sources.txt"
New-Item -ItemType Directory -Force bin | Out-Null
Get-ChildItem -Recurse src -Filter *.java | Resolve-Path -Relative |
    ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' } | Set-Content $sources

javac -nowarn -encoding UTF-8 -d bin "@$sources"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

# the tree xml is read at runtime, copy it next to the classes too
Copy-Item src\agents\firstLast\mario_bt.xml bin\agents\firstLast\ -Force

$opts = @()
if ($Debug) { $opts += '-Dbt.debug=true' }
java @opts -cp bin PlayLevel
