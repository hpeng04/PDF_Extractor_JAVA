package readers;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Reader {

    protected ITesseract tessReader;

    public Reader(){
        this.tessReader = new Tesseract();
        setTessData((this.tessReader));
    }

    private void setTessData(ITesseract reader) {
        try {
            // Get the tessdata path from resources directory
            ClassLoader classLoader = Reader.class.getClassLoader();
            URL tessdataURL = classLoader.getResource("tessdata");
            if (tessdataURL == null) {
                System.err.println("tessdata directory not found in resources");
                return;
            }

            // Convert URL to a file path
            try {
                File tessdataDir = Paths.get(tessdataURL.toURI()).toFile();
                reader.setDatapath(tessdataDir.getAbsolutePath());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }

            reader.setLanguage("eng");
            reader.setOcrEngineMode(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
