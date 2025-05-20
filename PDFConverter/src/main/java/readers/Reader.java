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
            // Get the tessdata path from the resources directory.
            // ClassLoader.getResource() finds a resource with a given name.
            ClassLoader classLoader = Reader.class.getClassLoader();
            URL tessdataURL = classLoader.getResource("tessdata"); // Expects "tessdata" folder in resources

            if (tessdataURL == null) {
                System.err.println("Error: tessdata directory not found in resources.");
                // Consider throwing an exception here or handling it more gracefully
                // depending on whether Tesseract is critical for all subclasses.
                return;
            }

            // Convert URL to a file path.
            // Paths.get(tessdataURL.toURI()).toFile() converts URL to File object.
            try {
                File tessdataDir = Paths.get(tessdataURL.toURI()).toFile();
                // reader.setDatapath() sets the directory containing Tesseract training data.
                reader.setDatapath(tessdataDir.getAbsolutePath());
            } catch (URISyntaxException e) {
                // This exception occurs if the URL is not formatted strictly according to RFC2396.
                System.err.println("Error converting tessdata URL to URI: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            reader.setLanguage("eng"); // Sets the language for OCR to English.
            // reader.setOcrEngineMode(1) typically sets Tesseract to use the LSTM OCR engine only.
            // LSTM (Long Short-Term Memory) is a neural network based engine, often more accurate.
            reader.setOcrEngineMode(1);

        } catch (Exception e) {
            // Catch-all for other potential exceptions during Tesseract setup.
            System.err.println("An unexpected error occurred during Tesseract setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
