import static imgprocessor.PdfImageProcessor.*;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.TesseractException;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import readers.PdfReader;
import readers.Reader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Disabled
public class readerTest {

    @Test
    public void test1() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";

        Map<Integer, BufferedImage> imgs = convert(filePath);
        Reader lineReader = new Reader();
        ITesseract tessReader = lineReader.tessReader;

        StringBuilder sb = new StringBuilder();

//        System.out.println(text);

        imgs.forEach((key, value) -> {
            try {
                sb.append(tessReader.doOCR(value));
            } catch (TesseractException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println(sb.toString());
//        List<Word> words = tessReader.getWords(imgs.get(0), 3);
//        for (Word word : words) {
//            String text = word.getText();
//            Rectangle rect = word.getBoundingBox();
//            if (text.contains("File:")) {
//                System.out.println(rect);
//                System.out.println(text);
//            }
//        }
//        System.out.println(words);
    }

    @Test
    public void test2() throws IOException, TesseractException {
        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\example.pdf";
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\House 3 PR.pdf";
        Reader lineReader = new Reader();
        ITesseract tessReader = lineReader.tessReader;
        Map<Integer, BufferedImage> imgs = convert(filePath);

//        List<Word> s = tessReader.getWords(imgs.get(11), 2);
//        for (Word w : s) {
//            System.out.println(w.getText());
////            System.out.println(w);
//        }
//        System.out.println(s);

//        System.out.println("----------------------------");
//        String text = tessReader.doOCR(imgs.get(18));
//        System.out.println(text);

        imgs.forEach((key, value) -> {
            try {
                String t = tessReader.doOCR(value);
                System.out.println(t);
            } catch (TesseractException e) {
                throw new RuntimeException(e);
            }
        });

    }




    @Test
    public void matcherTest(){
        String str1 = "Southeast Windows";
        String str2 = "east Windows";
        Matcher matcher = Pattern.compile("^" + str2).matcher(str1);

        if (matcher.find()) {
            System.out.println("Matched");
        } else {
            System.out.println("Not Matched");
        }
    }

    @Test
    public void test3() {
        double value = 100.6786897;
        List<Integer> values = new ArrayList<>();
        values.add(0, 123);
        values.add(0, 1112);
        values.add(0, 1233);
        System.out.println(values);
    }


}
