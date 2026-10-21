# Builds and runs S3 from the terminal.
#   .\run.ps1                      -> human vs AI
#   .\run.ps1 -AI                  -> AI vs AI (archers rush vs footmen rush)
#   .\run.ps1 -m maps/NWTR8.xml    -> any extra args are passed to s3.base.Main
param([switch]$AI, [Parameter(ValueFromRemainingArguments)] $Rest)

Set-Location $PSScriptRoot
$sources = "bin\sources.txt"
New-Item -ItemType Directory -Force bin | Out-Null
Get-ChildItem -Recurse src -Filter *.java | Resolve-Path -Relative |
    ForEach-Object { '"' + ($_ -replace '\\', '/') + '"' } | Set-Content $sources

javac -nowarn -encoding UTF-8 -d bin -cp "libs/*" "@$sources"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$gameArgs = @()
if ($AI) { $gameArgs += '-p', '1|player1|ai-archers-rush', '-p', '1|player2|ai-footmen-rush' }
if ($Rest) { $gameArgs += $Rest }
java -cp "bin;libs/*" s3.base.Main @gameArgs
