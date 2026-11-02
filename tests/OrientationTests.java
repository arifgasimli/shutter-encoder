import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import shutterencoder.functions.settings.Image;
import shutterencoder.functions.utils.Libplacebo;
import shutterencoder.library.FFPROBE;
import shutterencoder.ui.main.Shutter;
import shutterencoder.ui.videoplayer.VideoPlayerUtils;

/** Pixel-level checks: asymmetric input catches upside-down and wrong mirror axes. */
public class OrientationTests extends Shutter {
    private static int checks;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }

    private static byte[] run(String ffmpeg, String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-v", "error", "-y"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] data;
        try (var input = process.getInputStream()) { data = input.readAllBytes(); }
        if (process.waitFor() != 0) throw new AssertionError("FFmpeg failed: " + command);
        return data;
    }

    private static byte[] transformed(byte[] source, int angle, boolean mirror) {
        int width = 4, height = 2;
        int ow = Math.abs(angle) == 90 ? height : width;
        int oh = Math.abs(angle) == 90 ? width : height;
        byte[] output = new byte[source.length];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int ox = x, oy = y;
            if (angle == 90) { ox = height - 1 - y; oy = x; }
            if (angle == -90) { ox = y; oy = width - 1 - x; }
            if (angle == 180) { ox = width - 1 - x; oy = height - 1 - y; }
            if (mirror) ox = ow - 1 - ox;
            System.arraycopy(source, (y * width + x) * 3, output, (oy * ow + ox) * 3, 3);
        }
        return output;
    }

    public static void runTests(String ffmpeg) throws Exception {
        grpResolution = new JPanel();
        grpResolution.setVisible(true);
        grpImageSequence = new JPanel();
        caseRotate = new JCheckBox();
        caseMiror = new JCheckBox();
        comboRotate = new JComboBox<>(new String[] {"0", "90", "-90", "180"});
        Path temp = Files.createTempDirectory("shutter orientation ");
        try {
            byte[] source = new byte[] { (byte)255,0,0, 0,(byte)255,0, 0,0,(byte)255, (byte)255,(byte)255,0,
                    0,(byte)255,(byte)255, (byte)255,0,(byte)255, (byte)255,(byte)255,(byte)255, 0,0,0 };
            Path fixture = temp.resolve("corners.rgb");
            Files.write(fixture, source);
            for (String angle : new String[] {"0", "90", "-90", "180"}) for (boolean mirror : new boolean[] {false, true}) {
                caseRotate.setSelected(true);
                caseMiror.setSelected(mirror);
                comboRotate.setSelectedItem(angle);
                Libplacebo.useLibplaceboFilters.set(false);
                String previewFilter = Image.setRotate("", true);
                Libplacebo.useLibplaceboFilters.set(true);
                String exportFilter = Image.setRotate("", false);
                check(previewFilter.equals(exportFilter), "backend orientation parity " + angle + "/" + mirror);
                String filter = previewFilter.isEmpty() ? "null" : previewFilter;
                byte[] preview = run(ffmpeg, "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "4x2", "-i",
                        fixture.toString(), "-vf", filter, "-frames:v", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "-");
                check(Arrays.equals(preview, transformed(source, Integer.parseInt(angle), mirror)), "pixel orientation " + angle + "/" + mirror);
                Path encoded = temp.resolve("output.mkv");
                run(ffmpeg, "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "4x2", "-i", fixture.toString(),
                        "-vf", exportFilter.isEmpty() ? "null" : exportFilter, "-frames:v", "1", "-c:v", "ffv1", encoded.toString());
                byte[] decoded = run(ffmpeg, "-i", encoded.toString(), "-frames:v", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "-");
                check(Arrays.equals(preview, decoded), "preview matches decoded export " + angle + "/" + mirror);
            }
            caseRotate.setSelected(false);
            caseMiror.setSelected(true);
            check(Image.setRotate("", true).equals("hflip"), "mirror works with rotation disabled");
            BufferedImage frame = VideoPlayerUtils.decodePreviewFrame(new byte[] {0, (byte)0xf8, (byte)0xe0, 7}, 2, 1, false);
            check((frame.getRGB(0, 0) & 0xffffff) == 0xff0000 && (frame.getRGB(1, 0) & 0xffffff) == 0x00ff00,
                    "paused frame reads exact dimensions and little-endian pixels");
            BufferedImage alpha = VideoPlayerUtils.decodePreviewFrame(new byte[] {(byte)255, 0, 0, (byte)255}, 1, 1, true);
            check(alpha.getRGB(0, 0) == 0xffff0000, "paused frame preserves alpha channel order");
            boolean rejected = false;
            try { VideoPlayerUtils.decodePreviewFrame(new byte[1], 2, 1, false); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "reject partial preview frames");
            for (String angle : new String[] {"90", "-90", "270", "-270", "450", "90.000000"}) {
                check(FFPROBE.rotationSwapsDimensions(angle), "detect source quarter turn " + angle);
            }
            check(!FFPROBE.rotationSwapsDimensions("180") && !FFPROBE.rotationSwapsDimensions("0")
                    && !FFPROBE.rotationSwapsDimensions("invalid"), "source rotation does not swap dimensions for 0/180/invalid");
            Libplacebo.useLibplaceboFilters.set(true);
            Thread previewBuilder = new Thread(() -> Libplacebo.useLibplaceboFilters.set(false));
            previewBuilder.start();
            previewBuilder.join();
            check(Libplacebo.useLibplaceboFilters.get(), "preview backend choice cannot change export backend choice");

            Path base = temp.resolve("base.mp4");
            run(ffmpeg, "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "4x2", "-i", fixture.toString(),
                    "-frames:v", "1", "-c:v", "libx264rgb", "-crf", "0", base.toString());
            for (String metadataAngle : new String[] {"90", "180", "270"}) {
                Path phone = temp.resolve("phone.mp4");
                run(ffmpeg, "-display_rotation", metadataAngle, "-i", base.toString(), "-c", "copy", phone.toString());
                byte[] visible = run(ffmpeg, "-i", phone.toString(), "-frames:v", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "-");
                int expectedAngle = metadataAngle.equals("90") ? -90 : metadataAngle.equals("270") ? 90 : 180;
                check(Arrays.equals(visible, transformed(source, expectedAngle, false)), "source display matrix " + metadataAngle);
                Path output = temp.resolve("normalized.mp4");
                run(ffmpeg, "-i", phone.toString(), "-vf", "hflip", "-frames:v", "1", "-c:v", "libx264rgb", "-crf", "0",
                        "-map_metadata", "0", "-map_metadata:s:v", "0:s:v", output.toString());
                byte[] rendered = run(ffmpeg, "-i", output.toString(), "-frames:v", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "-");
                check(Arrays.equals(rendered, transformed(source, expectedAngle, true)), "export does not apply display matrix twice " + metadataAngle);
            }
        } finally {
            Libplacebo.useLibplaceboFilters.set(false);
            try (var paths = Files.list(temp)) {
                for (Path path : paths.toList()) Files.delete(path);
            }
            Files.delete(temp);
        }
        System.out.println("Orientation checks passed: " + checks);
    }
}
