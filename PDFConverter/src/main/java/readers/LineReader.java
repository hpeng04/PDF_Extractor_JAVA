package readers;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import imgprocessor.*;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;

import javax.imageio.ImageIO;

public class LineReader extends Reader{
    private final int TEXT_LINE = 2;
    private final int WORD = 3;

    private List<Word> words;
    private BufferedImage curr_img;

    public LineReader() {
        super();
    }

    public void setWords(BufferedImage img) {
        this.curr_img = img;
        this.words = tessReader.getWords(img, TEXT_LINE);
    }


    public String findTextLine(String keyword) {
        StringBuilder text = new StringBuilder();
        for (Word word : this.words) {
            String t = word.getText();
            if (t.contains(keyword)) {
                text.append(t);
            }
        }

        return text.toString();
    }

    public String findWordSequence(String keyword) {
        List<String> keyword_list = new ArrayList<>(Arrays.asList(keyword.split("\\s+")));
        return "";
    }

    public String findFileName() {
        String keyword = "File:";
        double height_multiplier = 2.5;
        return findTypical(keyword, height_multiplier);
    }



    public String findFanPower() {
        String keyword = "Fan and Preheater Power";
        double height_multiplier = 1.5;
        return findTypical(keyword, height_multiplier);
    }

    public String findSensibleHeat() {
        String keyword = "Sensible Heat Recovery Efficiency";
        double height_multiplier = 2.5;
        return findTypical(keyword, height_multiplier);
    }

    public String findVentEload() {
        String keyword = "Estimated Ventilation Electrical Load";
        double height_multiplier = 3.1;
        return findTypical(keyword, height_multiplier);
    }

    public String findWaterHeatingEquipment() {
        String keyword = "Water Heating";
        double height_multiplier = 2.5;
        return findTypical(keyword, height_multiplier);
    }

    private String findTypical(String keyword, double height_multiplier) {
        List<Rectangle> rect_list = new ArrayList<>();
        StringBuilder texts = new StringBuilder();
        for (Word word : this.words) {
            if (word.getText().contains(keyword)) {rect_list.add(word.getBoundingBox());}
        }

        for (Rectangle rect : rect_list) {
            int x = 0;
            int y = rect.y - 6;
            int width = this.curr_img.getWidth();
            int height = (int) (rect.height * height_multiplier);
            BufferedImage cropped_img = this.curr_img.getSubimage(x, y, width, height);

//            saveImage(cropped_img);

            try {
                texts.append(tessReader.doOCR(cropped_img));
            } catch (TesseractException e) {
                throw new RuntimeException(e);
            }
        }
        return texts.toString();
    }

    private void saveImage(BufferedImage img) {
        // Specify the file path and the image format
        String outputPath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\java_outs\\outputImage.jpg";
        String format = "JPEG"; // Format name can be "PNG", "JPEG", "GIF", etc.

        try {
            // Create a File object for the output path
            File outputFile = new File(outputPath);

            // Use ImageIO.write() to save the BufferedImage to the file
            boolean result = ImageIO.write(img, format, outputFile);

            // Check if the image was successfully saved
            if (result) {
                System.out.println("Image was successfully saved.");
            } else {
                System.out.println("Image could not be saved (unsupported file format).");
            }
        } catch (IOException e) {
            // Handle possible I/O errors
            e.printStackTrace();
        }
    }
//    private Rectangle findLocation(List<Word> words, String keyword) {
//        Rectangle rect = null;
//
//        for (Word word : words) {
//            if (word.getText().matches(keyword)) rect = word.getBoundingBox();
//        }
//        return rect;
//    }
}
