$ErrorActionPreference = "Stop"
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & ./scripts/run.ps1 -BuildOnly
    $taskBuild = Join-Path (Get-Location) ('.codex-build/package-' + [Guid]::NewGuid().ToString('N'))
    $taskPayload = Join-Path $taskBuild 'payload'
    New-Item -ItemType Directory -Force $taskPayload | Out-Null
    Copy-Item -LiteralPath 'Shutter Encoder-fixed.jar' -Destination (Join-Path $taskPayload 'Shutter Encoder.jar')
    Copy-Item -LiteralPath Languages,Library -Destination $taskPayload -Recurse
    Copy-Item -LiteralPath LICENSE.txt -Destination $taskPayload
    jlink --add-modules java.se,jdk.unsupported --strip-debug --no-header-files --no-man-pages --compress zip-6 --output (Join-Path $taskPayload 'runtime')
    if ($LASTEXITCODE -ne 0) { throw 'Runtime packaging failed' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $taskArchive = Join-Path $taskBuild 'payload.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory($taskPayload, $taskArchive,
        [IO.Compression.CompressionLevel]::Optimal, $false)
    $taskHash = (Get-FileHash -LiteralPath $taskArchive -Algorithm SHA256).Hash.ToLowerInvariant()
    $taskSource = (Get-Content -Raw scripts/PortableLauncher.cs).Replace('__PAYLOAD_SHA256__', $taskHash)
    $taskSourcePath = Join-Path $taskBuild 'PortableLauncher.cs'
    [IO.File]::WriteAllText($taskSourcePath, $taskSource, [Text.UTF8Encoding]::new($false))
    $taskCompiler = Join-Path $env:WINDIR 'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
    & $taskCompiler /nologo /target:winexe /platform:x64 /optimize+ /out:ShutterEncoder.exe `
        /reference:System.Windows.Forms.dll /reference:System.IO.Compression.dll `
        /reference:System.IO.Compression.FileSystem.dll "/resource:$taskArchive,payload.zip" $taskSourcePath
    if ($LASTEXITCODE -ne 0) { throw 'EXE packaging failed' }
    Get-Item ShutterEncoder.exe | Select-Object FullName,Length
} finally {
    Pop-Location
}
