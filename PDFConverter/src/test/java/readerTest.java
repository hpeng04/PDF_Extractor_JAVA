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
    public void matcherTest(){
        String str1 = ">19.1";
        String str2 = "east Windows";
        Matcher matcher = Pattern.compile("\\d+.\\d+").matcher(str1);
        if (matcher.find()) {
            System.out.println("Matched");
            System.out.printf("Found: %s%n", matcher.group());
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
