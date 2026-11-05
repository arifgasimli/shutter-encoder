import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import shutterencoder.functions.settings.Image;
import shutterencoder.functions.utils.Libplacebo;
import shutterencoder.library.FFPROBE;
import shutterencoder.ui.main.Shutter;

/** Crop, crop output size, rotation and output resolution: the encoded frame must have the expected size. */
public class CropTests extends Shutter {
    private static int checks;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("PASS: " + message);
    }

    /** Runs the filter on a 1920x1080 frame and returns the number of rgb24 bytes produced. */
    private static int outputBytes(String ffmpeg, String filter) throws Exception {
        List<String> command = new ArrayList<>(List.of(ffmpeg, "-nostdin", "-v", "error", "-f", "lavfi", "-i",
                "testsrc=size=1920x1080:rate=1", "-vf", filter, "-frames:v", "1", "-pix_fmt", "rgb24", "-f", "rawvideo", "-"));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        byte[] data;
        try (var input = process.getInputStream()) { data = input.readAllBytes(); }
        if (process.waitFor() != 0) throw new AssertionError("FFmpeg failed: " + command);
        return data.length;
    }

    private static void crop(int x, int y, int w, int h, double scale) {
        textCropPosX.setText(String.valueOf(x));
        textCropPosY.setText(String.valueOf(y));
        textCropWidth.setText(String.valueOf(w));
        textCropHeight.setText(String.valueOf(h));
        Image.cropOutputScale = scale;
    }

    /** Same order as the encoder: rotate, crop, then output resolution. */
    private static String chain() {
        String filter = Image.setRotate("", true);
        filter = Image.setCrop(filter, null);
        filter = Image.setScale(filter, false, true);
        return filter;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void runTests(String ffmpeg) throws Exception {
        language = new Properties();
        language.setProperty("source", "Source");
        grpResolution = new JPanel();
        grpResolution.setVisible(true);
        grpImageSequence = new JPanel();
        comboFonctions = new JComboBox<String[]>();
        ((javax.swing.DefaultComboBoxModel) comboFonctions.getModel()).addElement("H.264"); //declared as String[] upstream, holds Strings
        comboFilter = new JComboBox<>(new Object[] {".mp4"});
        comboResolution = new JComboBox<>(new String[] {"Source", "50%", "1280x720"});
        comboGPUFilter = new JComboBox<>(new String[] {"none"});
        btnNoUpscale = new JCheckBox();
        lblPad = new JLabel("stretch");
        lblPad.setVisible(false);
        caseAddTimecode = new JCheckBox();
        caseShowTimecode = new JCheckBox();
        caseAddText = new JCheckBox();
        caseShowFileName = new JCheckBox();
        caseAddWatermark = new JCheckBox();
        caseStabilisation = new JCheckBox();
        caseRotate = new JCheckBox();
        caseMiror = new JCheckBox();
        comboRotate = new JComboBox<>(new String[] {"0", "90", "-90", "180"});
        caseEnableCrop = new JCheckBox();
        caseEnableCrop.setSelected(true);
        comboPreset = new JComboBox<>(new String[] {"none", "auto"});
        textCropPosX = new JTextField();
        textCropPosY = new JTextField();
        textCropWidth = new JTextField();
        textCropHeight = new JTextField();
        Libplacebo.useLibplaceboFilters.set(false);
        FFPROBE.imageWidth = 1920;
        FFPROBE.imageHeight = 1080;
        FFPROBE.imageResolution = "1920x1080";

        try {
            //Crop output size: 1000x1000 shown at half size, ratio kept
            comboResolution.setSelectedItem("Source");
            crop(100, 40, 1000, 1000, 0.5);
            String filter = chain();
            check(filter.contains("crop=1000:1000:100:40") && filter.contains("scale@cropsize=500:500"), "crop output size filter: " + filter);
            check(outputBytes(ffmpeg, filter) == 500 * 500 * 3, "crop 1000x1000 at 50% encodes 500x500");

            //Odd results are rounded to even sizes
            crop(0, 0, 1001, 601, 0.5);
            check(Image.cropOutputWidth() == 500 && Image.cropOutputHeight() == 300, "crop output size is even");

            //Output resolution applies to the cropped area, not to the source
            comboResolution.setSelectedItem("50%");
            crop(100, 40, 1000, 1000, 1.0);
            filter = chain();
            check(outputBytes(ffmpeg, filter) == 500 * 500 * 3, "50% resolution of a 1000x1000 crop encodes 500x500: " + filter);

            //Both combined
            crop(100, 40, 1000, 600, 0.5);
            filter = chain();
            check(outputBytes(ffmpeg, filter) == 250 * 150 * 3, "crop 50% then resolution 50% encodes 250x150: " + filter);

            //Rotation: the crop is drawn on the rotated frame (1080x1920)
            comboResolution.setSelectedItem("Source");
            caseRotate.setSelected(true);
            comboRotate.setSelectedItem("90");
            check(Image.rotatedWidth() == 1080 && Image.rotatedHeight() == 1920, "quarter turn swaps the crop frame size");
            crop(0, 900, 1080, 1000, 1.0);
            filter = chain();
            check(outputBytes(ffmpeg, filter) == 1080 * 1000 * 3, "crop below the unrotated height works after a quarter turn: " + filter);

            //Rotation without crop: percentage uses the rotated size
            caseEnableCrop.setSelected(false);
            caseStabilisation.setSelected(true); //forces rotate before scale, like the encoder does
            comboResolution.setSelectedItem("50%");
            filter = chain();
            check(outputBytes(ffmpeg, filter) == 540 * 960 * 3, "50% after a quarter turn keeps the portrait ratio: " + filter);

            comboRotate.setSelectedItem("180");
            check(Image.rotatedWidth() == 1920 && Image.rotatedHeight() == 1080, "half turn keeps the crop frame size");
        } finally {
            Image.cropOutputScale = 1.0;
            caseRotate.setSelected(false);
            caseStabilisation.setSelected(false);
        }
        System.out.println("Crop checks passed: " + checks);
    }
}
