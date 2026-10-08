# Shutter Encoder 20.4 — Windows portable fork, revision 1

Unofficial Windows x64 build based on upstream `ad0cae5`.

Download **ShutterEncoder.exe** and double-click it. Java, FFmpeg, and FFprobe
are embedded; no CMD script or neighboring application folder is required.
First launch extracts the runtime into the user's local application cache.

## Fixes

- Correct hour/minute rounding in trimmed output timecodes.
- Avoid crashes and incorrect offsets when metadata or trim strings are empty.
- Reject media when FFmpeg cannot launch or returns an unsuccessful exit code.
- Clean up readability probe streams and processes.
- Support Windows PowerShell 5.1 when building from source.

## Validation

All application sources compile and all ten regression checks pass. Seven of
those checks fail on the original upstream sources. The EXE was launched on
Windows and its main window was responsive.

## Included and limitations

- OpenJDK 25 and FFmpeg/FFprobe 9.0.2 (Gyan shared build).
- Windows x64; uses Windows .NET Framework 4.x.
- Additional external tools and AI models are not included.
- This is a focused bug-fix release, not a claim that all application bugs are fixed.

`SHA256SUMS.txt` contains the executable's SHA-256 digest. The source and build
instructions are included in this tag; see `FORK.md` and `LICENSE.txt`.
