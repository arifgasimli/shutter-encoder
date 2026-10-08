# Windows portable fork

This unofficial fork of [Shutter Encoder](https://github.com/paulpacifico/shutter-encoder)
20.4 includes focused bug fixes and a single-file Windows x64 launcher.
The upstream project and original copyright remain credited in the source and license.

## Download and run

Download `ShutterEncoder.exe` from [Releases](https://github.com/arifgasimli/shutter-encoder/releases/latest)
and double-click it. No CMD script, adjacent application files, or separate Java
installation is needed. The launcher uses the Windows .NET Framework 4.x runtime.

On first launch, the embedded files are extracted into
`%LOCALAPPDATA%\ShutterEncoderPortable\<build-id>`. Later launches reuse that cache.
Diagnostics are written to `launcher.log` inside the build's cache directory.
Application settings remain in the upstream application's user settings location.

The initial release bundles OpenJDK 25 and FFmpeg/FFprobe 9.0.2 from the installed
Gyan shared build. Additional external tools and AI models are not bundled;
features that require them need those dependencies installed separately.

## Changes

- Floor trimmed timecode hours and minutes instead of rounding them up.
- Handle empty metadata timecodes and trim strings by content, avoiding a parsing
  crash and accidental trim offsets.
- Report unreadable media when FFmpeg cannot start or exits unsuccessfully, even
  if its output does not contain one of the recognized error messages.
- Launch the readability probe with separate arguments, close its streams, and
  clean up the probe process on early failure.
- Provide a GUI-only portable launcher with an embedded Java runtime and native tools.
- Write Java source argument files as UTF-8 without a BOM for compatibility with
  both Windows PowerShell 5.1 and PowerShell 7.

## Build and test

On Windows, install a JDK with `javac` and `jar`, JDK 25 with `jlink` on PATH,
and FFmpeg/FFprobe on PATH. The EXE compiler comes from the Windows .NET Framework
64-bit installation (`Framework64\v4.0.30319\csc.exe`).

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/package.ps1
```

The second command creates `ShutterEncoder.exe` in the repository root. Close a
running copy of this EXE before rebuilding it. Build artifacts and native binaries
are ignored by Git; the upstream dependency JAR is preserved.

Validation for the initial release: all application sources compile, all ten
regression checks pass, and the packaged application opens a responsive window.
Seven checks fail on the unmodified upstream `ad0cae5` sources. Full GUI, codec,
hardware, and cross-platform validation has not been performed.

## Licensing and bundled components

Shutter Encoder and these modifications are distributed under the terms in
`LICENSE.txt`. The release's source is available from its Git tag, including the
launcher and packaging scripts. OpenJDK's notices are retained in the embedded
runtime's `legal` directory. FFmpeg is a separate upstream component:
[FFmpeg source](https://ffmpeg.org/download.html),
[Gyan Windows build information](https://www.gyan.dev/ffmpeg/builds/).
