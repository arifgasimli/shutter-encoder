# Regression checks

From the repository root on Windows, run:

```powershell
./scripts/test.ps1
```

Requires a JDK (tested with Java 21) and FFmpeg on PATH. A custom executable
can be supplied with `./scripts/test.ps1 -FFmpeg 'C:\path\ffmpeg.exe'`.
The script compiles all application sources against the dependencies bundled in
the upstream `Shutter Encoder.jar`, then runs the checks without opening the UI.
Generated files stay in `.codex-build/`; the upstream JAR is unchanged.

The checks cover trimmed timecode minute/hour boundaries, dynamically allocated
empty metadata and trim strings, valid media paths containing spaces and special
characters, missing/corrupt media, a missing FFmpeg executable, and an unsuccessful
process exit without a recognized error message. Fixtures are generated locally
and removed after the run.

On upstream commit `ad0cae5`, seven of the ten checks fail. With these fixes,
all ten pass. This is a focused regression suite, not full GUI, codec, hardware,
or cross-platform validation.
