package readers;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.tess4j.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Reader {

    public ITesseract tessReader; //TODO: CHANGE BACK

    public Reader(){
        this.tessReader = new Tesseract();
        setTessData((this.tessReader));
    }

    private static void setTessData(ITesseract reader) {
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
            e.printStackTrace();
        }
    }

    protected List<List<Integer>> findLongestChain(List<List<Integer>> locations) {
        if (locations.isEmpty()) {
            return locations;
        }

        // Sort locations by the third element (vertical position)
        Collections.sort(locations, Comparator.comparingInt(a -> a.get(2)));

        List<List<Integer>> longestChain = new ArrayList<>();
        List<List<Integer>> currentChain = new ArrayList<>();
        currentChain.add(locations.get(0));

        for (int i = 1; i < locations.size(); i++) {
            // Compare the third element of the current and previous locations
            if (Math.abs(locations.get(i).get(2) - locations.get(i - 1).get(2)) <= 10) {
                // If the difference is 10 or less, continue the current chain
                currentChain.add(locations.get(i));
            } else {
                // If the difference is more than 10, check if the current chain is the longest
                if (currentChain.size() > longestChain.size()) {
                    longestChain = new ArrayList<>(currentChain);
                }
                // Start a new chain with the current location
                currentChain = new ArrayList<>();
                currentChain.add(locations.get(i));
            }
        }

        // Check one last time if the ending chain is the longest
        if (currentChain.size() > longestChain.size()) {
            longestChain = currentChain;
        }

        return longestChain;
    }
}
