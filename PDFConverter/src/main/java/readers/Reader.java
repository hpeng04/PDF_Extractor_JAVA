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

/**
 * Base class for readers, primarily responsible for initializing and configuring
 * the Tesseract OCR engine ({@link net.sourceforge.tess4j.ITesseract}).
 * It sets up the path to Tesseract's training data (`tessdata`)
 * and configures the language and OCR engine mode.
 */
public class Reader {

    protected ITesseract tessReader;

    /**
     * Constructor for the Reader class.
     * Initializes the Tesseract OCR engine (`tessReader`) and configures its data path,
     * language, and OCR engine mode by calling {@link #setTessData(ITesseract)}.
     */
    public Reader(){
        this.tessReader = new Tesseract();
        setTessData(this.tessReader);
    }

    /**
     * Configures the provided Tesseract instance.
     * Sets the path to the `tessdata` directory (expected to be in the classpath resources),
     * sets the OCR language to English ("eng"), and sets the OCR engine mode to 1 (LSTM only).
     *
     * @param reader The {@link net.sourceforge.tess4j.ITesseract} instance to configure.
     */
    private void setTessData(ITesseract reader) {
        try {
            // Create a temporary directory to store tessdata
            Path tempDir = Files.createTempDirectory("tessdata");

            // Assuming 'eng.traineddata' is stored in 'src/main/resources'
            String resourcePath = "/eng.traineddata";
            InputStream in = Reader.class.getResourceAsStream(resourcePath);
            File tessDataFile = new File(tempDir.toFile(), "eng.traineddata");

            try (OutputStream out = new FileOutputStream(tessDataFile)) {
                // Copy tessdata from resources to the temporary directory
                byte[] buffer = new byte[1024];
                int readBytes;
                while ((readBytes = in.read(buffer)) != -1) {
                    out.write(buffer, 0, readBytes);
                }
            }

            reader.setDatapath(tempDir.toAbsolutePath().toString());
            reader.setLanguage("eng");

        } catch (Exception e) {
            // Catch-all for other potential exceptions during Tesseract setup.
            System.err.println("An unexpected error occurred during Tesseract setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
