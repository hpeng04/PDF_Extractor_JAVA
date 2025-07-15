package readers;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

/**
 * Base class for readers, primarily responsible for initializing and configuring
 * the Tesseract OCR engine ({@link net.sourceforge.tess4j.ITesseract}).
 */
public class Reader {

    protected ITesseract tessReader;

    public Reader(){
        this.tessReader = new Tesseract();
        configureTesseract(this.tessReader);
    }

    /**
     * Configures the provided Tesseract instance.
     * Made public so it can be used by thread-local instances.
     */
    public void configureTesseract(ITesseract reader) {
        try {
            ClassLoader classLoader = Reader.class.getClassLoader();
            URL tessdataURL = classLoader.getResource("tessdata");

            if (tessdataURL == null) {
                System.err.println("Error: tessdata directory not found in resources.");
                return;
            }

            try {
                File tessdataDir = Paths.get(tessdataURL.toURI()).toFile();
                reader.setDatapath(tessdataDir.getAbsolutePath());
            } catch (URISyntaxException e) {
                System.err.println("Error converting tessdata URL to URI: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            reader.setLanguage("eng");
            reader.setOcrEngineMode(1);

        } catch (Exception e) {
            System.err.println("An unexpected error occurred during Tesseract setup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}