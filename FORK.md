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
- Use one rotate-then-mirror sequence across preview and export, including
  mirroring with rotation enabled at 0 degrees.
- Recognize 270-degree source display matrices when calculating video dimensions.
- Decode ordinary paused previews directly from the source using a captured UI
  state, reject obsolete results, and avoid losing a queued refresh as a worker exits.
- Isolate libplacebo backend decisions between concurrent preview/export builders.
- Apply resolution settings during CPU preview and match export's scale/rotate order.
- Retain the latest seek request during rapid changes and use the rotated output
  aspect ratio when scaling precedes a quarter turn.
- Commit timeline boundaries from pointer coordinates on the Swing event thread,
  independently of delayed preview frames; keep the dragged segment selected.
- Clamp edited ranges to source-frame and neighboring-segment boundaries, and
  keep both handles selectable when their pixel positions overlap.
- Replace the timeline's polling/seek/join loop with a coalesced Swing timer;
  release commits the pointer position immediately and stops the drag.
- Keep marker layout updates separate from range edits and reindex split ranges
  by source time instead of timeline pixels.
- Default output names: fixed `{fps}fps_se_` prefix (not overridden by saved
  settings) and an `_H.264_{height}_{width}_{fps}` suffix enabled by default.
- Select the first working GPU encoder by default (NVENC, Quick Sync, AMF, then
  Vulkan) unless the user or a preset chose one.
- Close the preview's audio lines and every process of a filter pipeline, only
  invalidate a pending still preview when a seek really runs (fixes a black
  player after a few files), and stop stale thumbnail jobs overwriting new ones.
- Bound memory: console logs, the FFmpeg error report and the seek frame buffer.
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
