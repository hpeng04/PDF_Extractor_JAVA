package readers;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

import java.io.File;
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
            // Create a temporary directory to store tessdata
            Path tempDir = Files.createTempDirectory("tessdata");

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
            e.printStackTrace();
        }
    }

}
