# Shutter Encoder 20.4 — Windows portable fork, revision 2

Unofficial Windows x64 build based on upstream `ad0cae5`.

Download **ShutterEncoder.exe** and double-click it. Java, FFmpeg, and FFprobe
are embedded; no CMD script or neighboring application folder is required.
First launch extracts the runtime into the user's local application cache.

## Changes since revision 1

### Preview and memory
- Close the preview's audio lines and every process of a filter pipeline.
- Fix a black player after a few files: only invalidate a pending still preview
  when a seek really runs.
- Stop cancelled thumbnail jobs from overwriting the next file's filmstrip.
- Bound console logs, the FFmpeg error report and the seek frame buffer.
- Live picture while dragging the timeline (dedicated scrub decoder), falling
  back to the nearest keyframe when exact seeks are slow.
- The crop overlay no longer recalculates sizes on every painted frame, and the
  source probe runs off the UI thread (it could freeze the window).

### Crop
- Crop on the whole rotated source; the output resolution is applied to the
  cropped area when cropping last.
- Set the crop's output size with one scale factor, keeping the crop ratio and
  an even encoded size.
- Reset the crop selection when the orientation changes.

### Defaults
- Select the first working GPU encoder by default (NVENC, Quick Sync, AMF, then
  Vulkan) unless the user or a preset chose one.
- Output names: fixed `{fps}fps_se_` prefix and an `_H.264_{height}_{width}_{fps}`
  suffix enabled by default.

### Also included from revision 1
- Timecode rounding, empty metadata/trim handling, media readability probes,
  rotate/mirror consistency, and timeline range-editing fixes (see `FORK.md`).

## Validation

All application sources compile and all regression, orientation, range and crop
checks pass. Full GUI, codec, hardware and cross-platform validation has not
been performed.

## Included and limitations

- OpenJDK 25 and FFmpeg/FFprobe 9.0.2 (Gyan shared build).
- Windows x64; uses Windows .NET Framework 4.x.
- Additional external tools and AI models are not included.
- This is a focused bug-fix release, not a claim that all application bugs are fixed.

`SHA256SUMS.txt` contains the executable's SHA-256 digest. The source and build
instructions are included in this tag; see `FORK.md` and `LICENSE.txt`.
