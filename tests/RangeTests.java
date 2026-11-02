import java.awt.Cursor;
import java.util.ArrayList;
import javax.swing.*;
import shutterencoder.library.FFPROBE;
import shutterencoder.ui.main.Shutter;
import shutterencoder.ui.videoplayer.VideoPlayerCore;
import shutterencoder.ui.videoplayer.VideoPlayerUI;
import shutterencoder.ui.videoplayer.VideoPlayerUtils;
import shutterencoder.ui.videoplayer.VideoPlayerMultiCuts;
import shutterencoder.ui.videoplayer.VideoPlayerMultiCuts.CutSegment;

public class RangeTests extends Shutter {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }
    private static CutSegment segment(int index, int start, int end) {
        return new CutSegment(index, start, end, 0, 0, start / 25, start % 25, 0, 0, end / 25, end % 25);
    }
    private static double start(CutSegment s) { return VideoPlayerMultiCuts.getSegmentTime(s.inH, s.inM, s.inS, s.inF); }
    private static double end(CutSegment s) { return VideoPlayerMultiCuts.getSegmentTime(s.outH, s.outM, s.outS, s.outF); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void runTests(String ffmpeg) throws Exception {
        Process process = new ProcessBuilder(ffmpeg, "-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        process.waitFor();
        VideoPlayerCore.playerVideo = process;
        try {
            SwingUtilities.invokeAndWait(() -> {
                FFPROBE.currentFPS = 25; FFPROBE.accurateFPS = 25; FFPROBE.dropFrameTC = ":";
                caseSetTimecode = new JCheckBox(); caseEnableSequence = new JCheckBox();
                comboFonctions = new JComboBox(new String[] {"H.264"});
                VideoPlayerUI.caseInH = new JTextField("00"); VideoPlayerUI.caseInM = new JTextField("00");
                VideoPlayerUI.caseInS = new JTextField("24"); VideoPlayerUI.caseInF = new JTextField("00");
                VideoPlayerUI.caseOutH = new JTextField("00"); VideoPlayerUI.caseOutM = new JTextField("00");
                VideoPlayerUI.caseOutS = new JTextField("40"); VideoPlayerUI.caseOutF = new JTextField("00");
                VideoPlayerUI.waveformContainer = new JLabel(); VideoPlayerUI.waveformContainer.setSize(1000, 30);
                VideoPlayerUI.caseInternalTc = new JCheckBox(); VideoPlayerUI.lblPosition = new JLabel();
                VideoPlayerUI.sliderChange = true;
                VideoPlayerUI.totalFrames = 1000; VideoPlayerUI.fileDuration = 40000;
                VideoPlayerCore.videoPath = "range-regression.mp4";
                VideoPlayerUtils.fileList.setLength(0);
                VideoPlayerMultiCuts.cutSegments = new ArrayList<>();
                VideoPlayerMultiCuts.cutSegments.add(segment(0, 0, 200));
                VideoPlayerMultiCuts.cutSegments.add(segment(1, 200, 500));
                VideoPlayerMultiCuts.cutSegments.add(segment(2, 600, 1000));
                VideoPlayerCore.activeSegmentIndex = 2; VideoPlayerCore.dragSegmentIndex = 2;
                VideoPlayerUI.boundaryDragInProgress = true; VideoPlayerUI.mouseIsPressed = true;
                VideoPlayerUI.waveformContainer.setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                CutSegment last = VideoPlayerMultiCuts.cutSegments.get(2);
                check(VideoPlayerUtils.setBoundary(2, false, 950), "move last boundary before preview finishes");
                VideoPlayerCore.playerCurrentFrame = 5; VideoPlayerCore.bufferCurrentFrame = 0;
                VideoPlayerMultiCuts.updateCurrentSegment();
                VideoPlayerUtils.getTimePoint(5);
                check(end(last) == 950 && VideoPlayerUI.caseOutS.getText().equals("38")
                        && VideoPlayerCore.activeSegmentIndex == 2, "late decoded frame cannot overwrite range or drag owner");
                VideoPlayerUtils.setBoundary(2, false, -100);
                check(end(last) == 601, "crossed boundary clamps to one source frame");
                check(VideoPlayerUtils.setBoundary(2, false, 980) && end(last) == 980,
                        "collapsed range can be expanded without resetting effects");
                VideoPlayerUI.waveformContainer.setSize(10, 30);
                VideoPlayerUtils.setMarkers();
                check(start(last) == 600 && end(last) == 980, "timeline zoom changes pixels without changing range times");
                VideoPlayerUtils.setBoundary(2, true, -100);
                check(start(last) == 500 && end(last) == 980, "range does not overlap previous segment");
                boolean valid = true;
                for (int i = 0; i < 1500; i++) {
                    VideoPlayerUtils.setBoundary(2, i % 2 == 0, (i * 173) % 1300 - 150);
                    VideoPlayerCore.playerCurrentFrame = i % 400;
                    VideoPlayerMultiCuts.updateCurrentSegment();
                    VideoPlayerUtils.getTimePoint(i % 400);
                    VideoPlayerUtils.setMarkers();
                    valid &= end(last) - start(last) >= 1 && start(last) >= 500 && end(last) <= 1000;
                }
                check(valid, "1500 boundary edits remain valid with lagging preview callbacks");
                double saved = end(last);
                check(!VideoPlayerUtils.setBoundary(99, false, 800) && !VideoPlayerUtils.setBoundary(2, false, Double.NaN)
                        && end(last) == saved, "invalid edits preserve the last valid range");
                check(VideoPlayerUI.boundaryCursorAt(599, 600, 600) == Cursor.W_RESIZE_CURSOR
                        && VideoPlayerUI.boundaryCursorAt(600, 600, 600) == Cursor.E_RESIZE_CURSOR,
                        "both handles remain selectable when a range occupies one pixel");
                VideoPlayerMultiCuts.saveCutState();
                VideoPlayerUtils.setBoundary(2, false, 1000);
                VideoPlayerUI.boundaryDragInProgress = false;
                VideoPlayerCore.playerCurrentFrame = 700;
                VideoPlayerMultiCuts.undoCut();
                check(end(VideoPlayerMultiCuts.cutSegments.get(2)) == saved, "undo restores range after dragging");
                VideoPlayerMultiCuts.redoCut();
                check(end(VideoPlayerMultiCuts.cutSegments.get(2)) == 1000, "redo restores changed boundary");
                VideoPlayerMultiCuts.cutSegments = new ArrayList<>();
                VideoPlayerMultiCuts.cutSegments.add(segment(0, 0, 200));
                VideoPlayerMultiCuts.cutSegments.add(segment(1, 200, 500));
                VideoPlayerMultiCuts.cutSegments.add(segment(2, 600, 1000));
                VideoPlayerCore.activeSegmentIndex = 2; VideoPlayerCore.playerCurrentFrame = 750;
                VideoPlayerUI.btnEdit = new JButton(); VideoPlayerUI.btnEdit.setName("cut");
                VideoPlayerUI.caseApplyCutToAll = new JCheckBox();
                VideoPlayerUI.waveformContainer.setSize(1000, 30);
                VideoPlayerUI.cursorCurrentFrame = new JPanel(); VideoPlayerUI.cursorCurrentFrame.setLocation(750, 0);
                VideoPlayerMultiCuts.setCurrentSegmentValues();
                VideoPlayerMultiCuts.editCurrentSegment();
                check(VideoPlayerMultiCuts.cutSegments.size() == 4
                        && end(VideoPlayerMultiCuts.cutSegments.get(2)) == 750
                        && start(VideoPlayerMultiCuts.cutSegments.get(3)) == 750
                        && end(VideoPlayerMultiCuts.cutSegments.get(3)) == 1000,
                        "newly split last range retains its original end and valid indices");
                VideoPlayerCore.dragSegmentIndex = 3; VideoPlayerCore.activeSegmentIndex = 3;
                VideoPlayerUI.boundaryDragInProgress = true;
                VideoPlayerCore.playerCurrentFrame = 10;
                VideoPlayerMultiCuts.updateCurrentSegment();
                check(VideoPlayerUtils.setBoundary(3, false, 990)
                        && end(VideoPlayerMultiCuts.cutSegments.get(3)) == 990,
                        "newest boundary remains editable while preview is on an older segment");
                VideoPlayerUI.boundaryDragInProgress = false;
                java.util.concurrent.CountDownLatch running = new java.util.concurrent.CountDownLatch(1);
                java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
                VideoPlayerCore.setTime = new Thread(() -> {
                    running.countDown();
                    try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
                VideoPlayerCore.setTime.start();
                try {
                    running.await();
                    VideoPlayerMultiCuts.updateCurrentSegment();
                    check(VideoPlayerCore.activeSegmentIndex == 3,
                            "mouse release keeps newest range selected until delayed seek completes");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); throw new AssertionError(e);
                } finally {
                    release.countDown();
                    try { VideoPlayerCore.setTime.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    VideoPlayerCore.setTime = new Thread();
                }
                VideoPlayerUI.boundaryDragInProgress = true;
                boolean fractionalValid = true;
                for (float fps : new float[] {23.98f, 29.97f, 59.94f}) {
                    FFPROBE.currentFPS = fps; FFPROBE.accurateFPS = fps;
                    CutSegment fractional = new CutSegment(0, 0, 1000, 0,0,0,0, 0,0,10,0);
                    VideoPlayerMultiCuts.cutSegments = new ArrayList<>(); VideoPlayerMultiCuts.cutSegments.add(fractional);
                    VideoPlayerCore.activeSegmentIndex = 0;
                    for (int i = 0; i < 200; i++) {
                        VideoPlayerUtils.setBoundary(0, i % 2 == 0, i * 13 % 800);
                        fractionalValid &= end(fractional) - start(fractional) >= 1;
                    }
                }
                check(fractionalValid, "fractional FPS edits never create empty or inverted ranges");
                VideoPlayerUI.boundaryDragInProgress = false; VideoPlayerUI.mouseIsPressed = false;
                VideoPlayerMultiCuts.cutSegments.clear();
                VideoPlayerMultiCuts.clearCutHistory();
            });
        } finally { VideoPlayerCore.playerVideo = null; }
        System.out.println("Range checks passed: " + checks);
    }
}
