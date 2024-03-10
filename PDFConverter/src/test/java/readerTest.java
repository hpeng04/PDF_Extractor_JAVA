import static imgprocessor.PdfImageProcessor.*;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import org.junit.jupiter.api.Test;
import readers.LineReader;
import readers.Reader;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class readerTest {

    @Test
    public void test1() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        Reader lineReader = new Reader();
        ITesseract tessReader = lineReader.tessReader;

//        String text = tessReader.doOCR(imgs.get(19));

        List<Word> words = tessReader.getWords(imgs.get(0), 3);
        for (Word word : words) {
            String text = word.getText();
            Rectangle rect = word.getBoundingBox();
            if (text.contains("File:")) {
                System.out.println(rect);
                System.out.println(text);
            }
        }
//        System.out.println(words);
    }

    @Test
    public void test2() throws IOException, TesseractException {
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(10));
        String s = lineReader.findTextLine("Fan and Preheater Power");
        System.out.println(s);

//        String text = tessReader.doOCR(imgs.get(19));

//        System.out.println(words);
    }

    @Test
    public void testFileName() throws IOException, TesseractException {
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(0));
        String fileName = lineReader.findFileName();
        System.out.println(fileName);

    }

    @Test
    public void testFanPower() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(10));
        String power = lineReader.findFanPower();
//        System.out.println(power);
        List<String> powers = new ArrayList<>(Arrays.asList(power.split("\\n")));
        String P0 = new String();
        String P25 = new String();
        System.out.println(powers);
        for (String p : powers) {
            if (p.contains("Fan and Preheater Power at 0.0 ")) P0 = p;
            if (p.contains("Fan and Preheater Power at -25 ")) P25 = p;
        }
        System.out.println("P0:" + P0);
        System.out.println("P25:" + P25);
    }

    @Test
    public void testSensibleHeat() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(10));
        String heat = lineReader.findSensibleHeat();
        System.out.println(heat);

    }

    @Test
    public void testVent() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(11));
        String vent = lineReader.findVentEload();
        System.out.println(vent);

    }

    @Test
    public void testWaterHeatingEquip() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        LineReader lineReader = new LineReader();
        lineReader.setWords(imgs.get(12));
        String temp = lineReader.findWaterHeatingEquipment();
        List<String> water_heating_list = new ArrayList<>(Arrays.asList(temp.split("\\n")));
        String water_heating = null;
        for (int i = 0; i < water_heating_list.size(); i++) {
            if (water_heating_list.get(i).matches("Equipment:") && i != 0) {
                water_heating = water_heating_list.get(i - 1);
            }
        }
        if (water_heating != null) {
            water_heating = water_heating.replace("Water Heating", "").stripLeading();
        }
        System.out.println(water_heating);

    }
}
