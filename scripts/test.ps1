param([string]$FFmpeg = "ffmpeg")
$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $taskFFmpeg = (Get-Command $FFmpeg -ErrorAction Stop).Source
    New-Item -ItemType Directory -Force .codex-build/classes | Out-Null
    $taskSources = @(Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
    $taskSources += (Resolve-Path tests/RegressionTests.java).Path
    $taskSourceLines = @($taskSources | ForEach-Object { '"' + $_.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines((Join-Path (Get-Location) '.codex-build/sources.txt'),
        [string[]]$taskSourceLines, [Text.UTF8Encoding]::new($false))
    javac -encoding UTF-8 -cp 'Shutter Encoder.jar' -d .codex-build/classes '@.codex-build/sources.txt'
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed" }
    java -cp '.codex-build/classes;Shutter Encoder.jar' RegressionTests $taskFFmpeg
    if ($LASTEXITCODE -ne 0) { throw "Regression tests failed" }
} finally {
    Pop-Location
}
