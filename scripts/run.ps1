param([switch]$BuildOnly)
$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $taskFFmpeg = (Get-Command ffmpeg -ErrorAction Stop).Source
    $taskFFprobe = (Get-Command ffprobe -ErrorAction Stop).Source
    New-Item -ItemType Directory -Force .codex-build/app-classes | Out-Null
    $taskSources = @(Get-ChildItem src -Recurse -Filter *.java |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines((Join-Path (Get-Location) '.codex-build/app-sources.txt'),
        [string[]]$taskSources, [Text.UTF8Encoding]::new($false))
    javac -encoding UTF-8 -cp 'Shutter Encoder.jar' -d .codex-build/app-classes '@.codex-build/app-sources.txt'
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed" }
    Copy-Item -LiteralPath 'Shutter Encoder.jar' -Destination 'Shutter Encoder-fixed.jar' -Force
    jar --update --file 'Shutter Encoder-fixed.jar' -C .codex-build/app-classes shutterencoder
    if ($LASTEXITCODE -ne 0) { throw "JAR packaging failed" }

    # The application looks for native tools next to its JAR, in Library.
    # Include DLLs because the installed FFmpeg may be a shared build.
    foreach ($taskTool in @($taskFFmpeg, $taskFFprobe)) {
        Copy-Item -LiteralPath $taskTool -Destination Library -Force
        Get-ChildItem -LiteralPath (Split-Path $taskTool) -Filter *.dll |
            Copy-Item -Destination Library -Force
    }
    if (!$BuildOnly) {
        java --enable-native-access=ALL-UNNAMED -cp 'Shutter Encoder-fixed.jar' shutterencoder.ui.main.Shutter
        if ($LASTEXITCODE -ne 0) { throw "Shutter Encoder exited with an error" }
    }
} finally {
    Pop-Location
}
