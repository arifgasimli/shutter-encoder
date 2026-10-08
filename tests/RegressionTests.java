import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import shutterencoder.functions.settings.InputAndOutput;
import shutterencoder.functions.settings.Timecode;
import shutterencoder.library.FFMPEG;
import shutterencoder.library.FFPROBE;
import shutterencoder.library.LibraryUtils;
import shutterencoder.ui.main.Shutter;
import shutterencoder.ui.videoplayer.VideoPlayerUI;

/** Runs without starting the application, using its bundled dependency JAR. */
public class RegressionTests extends Shutter {
    private static int failures;

    private static void check(boolean condition, String description) {
        System.out.println((condition ? "PASS: " : "FAIL: ") + description);
        if (!condition) failures++;
    }

    private static void timecode(String h, String m, String s, String f, String expected) {
        VideoPlayerUI.caseInH = new JTextField(h);
        VideoPlayerUI.caseInM = new JTextField(m);
        VideoPlayerUI.caseInS = new JTextField(s);
        VideoPlayerUI.caseInF = new JTextField(f);
        try {
            String actual = Timecode.setTimecode(null);
            check(actual.equals(" -timecode \"" + expected + "\""),
                    "trim timecode " + expected + " (actual: " + actual + ")");
        } catch (RuntimeException e) {
            check(false, "trim timecode " + expected + " threw " + e);
        }
    }

    public static void main(String[] args) {
        try {
            runTests(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void runTests(String[] args) throws Exception {
        caseConform = new JCheckBox();
        caseGenerateFromDate = new JCheckBox();
        caseSetTimecode = new JCheckBox();
        comboFonctions = new JComboBox(new String[] {"H.264"});
        FFPROBE.currentFPS = 25;
        FFPROBE.dropFrameTC = ":";
        FFPROBE.timecode1 = "00";
        FFPROBE.timecode2 = "00";
        FFPROBE.timecode3 = "00";
        FFPROBE.timecode4 = "00";
        VideoPlayerUI.inputFramerateMS = 40;
        InputAndOutput.inPoint = " -ss 1";
        timecode("00", "00", "45", "13", "00:00:45:13");
        timecode("00", "40", "00", "00", "00:40:00:00");
        timecode("01", "59", "59", "24", "01:59:59:24");

        // Empty strings loaded dynamically must behave like literal empty strings.
        FFPROBE.timecode1 = new String("");
        try {
            check(Timecode.setTimecode(null).isEmpty(), "no metadata timecode");
        } catch (RuntimeException e) {
            check(false, "no metadata timecode threw " + e);
        }
        FFPROBE.timecode1 = "00";
        InputAndOutput.inPoint = new String("");
        timecode("00", "00", "45", "13", "00:00:00:00");

        Path temp = Files.createTempDirectory("shutter regression ");
        try {
            Path media = temp.resolve("video $name & sample.wav");
            Process generate = new ProcessBuilder(args[0], "-nostdin", "-v", "error",
                    "-f", "lavfi", "-i", "sine=frequency=1000:duration=0.1",
                    media.toString()).inheritIO().start();
            if (generate.waitFor() != 0) throw new AssertionError("fixture generation failed");
            FFMPEG.PathToFFMPEG = args[0];
            check(LibraryUtils.isReadable(media.toFile()), "read valid media with spaces and special characters");
            check(!LibraryUtils.isReadable(temp.resolve("missing.wav").toFile()), "reject missing media");
            Path corrupt = temp.resolve("corrupt.wav");
            Files.writeString(corrupt, "not media");
            check(!LibraryUtils.isReadable(corrupt.toFile()), "reject invalid media");
            FFMPEG.PathToFFMPEG = temp.resolve("missing-ffmpeg.exe").toString();
            check(!LibraryUtils.isReadable(media.toFile()), "reject FFmpeg launch failure");
            boolean windows = System.getProperty("os.name").contains("Windows");
            Path failedProbe = temp.resolve(windows ? "failed-probe.cmd" : "failed-probe.sh");
            Files.writeString(failedProbe, windows ? "@exit /b 7\r\n" : "#!/bin/sh\nexit 7\n");
            if (!windows && !failedProbe.toFile().setExecutable(true)) {
                throw new AssertionError("Cannot make probe fixture executable");
            }
            FFMPEG.PathToFFMPEG = failedProbe.toString();
            check(!LibraryUtils.isReadable(media.toFile()), "reject nonzero exit without recognized error text");
        } finally {
            try (var files = Files.list(temp)) {
                for (Path path : files.toList()) Files.delete(path);
            }
            Files.delete(temp);
        }
        if (failures != 0) throw new AssertionError(failures + " regression check(s) failed");
    }
}
