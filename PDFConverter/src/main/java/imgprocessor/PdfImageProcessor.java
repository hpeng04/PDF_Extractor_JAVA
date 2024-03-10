package imgprocessor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PdfImageProcessor {

    public static Map<Integer, BufferedImage> convert(String filePath) throws IOException {
        Map<Integer, BufferedImage> cachedImages = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage img = processPage(pdfRenderer, page);
                cachedImages.put(page, img);
            }
        }
        return cachedImages;
    }

    private static BufferedImage processPage(PDFRenderer renderer, int pageNumber) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageNumber, 300);
        int width = image.getWidth();
        int height = image.getHeight();

        int top = (int) (height * 0.05);
        int bottom = (int) (height * 0.95);

        return image.getSubimage(0, top, width, bottom - top);
    }

//    public static void main(String[] args) {
//
//        String filePath = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\PDFs\\Home 1 PR.pdf"; // Update this path
//
//        String outputDir = "C:\\Users\\45799\\Desktop\\py\\pdfReader\\java_outs"; // Update this path
//
//        long startTime = System.nanoTime();
//        try {
//
//            Map<Integer, BufferedImage> map = convert(filePath);
//            System.out.println(map.size());
//            saveImages(map, outputDir);
//
//        } catch (IOException e) {
//
//            e.printStackTrace();
//
//        }
//
//        long endTime = System.nanoTime();
//
//        long duration = (endTime - startTime);  // Calculate the duration in nanoseconds
//
//        System.out.println("Execution time: " + duration / 1_000_000 + " milliseconds");
//    }

//    public static void saveImages(Map<Integer, BufferedImage> images, String outputPath) {
//        // Ensure the output directory exists
//        File directory = new File(outputPath);
//        if (!directory.exists()) {
//            directory.mkdirs();
//        }
//
//        // Iterate through the map and save each image
//        for (Map.Entry<Integer, BufferedImage> entry : images.entrySet()) {
//            Integer pageNumber = entry.getKey();
//            BufferedImage image = entry.getValue();
//            File outputFile = new File(directory, pageNumber + ".png");
//            try {
//                ImageIO.write(image, "PNG", outputFile);
//                System.out.println("Saved image: " + outputFile.getAbsolutePath());
//            } catch (IOException e) {
//                System.err.println("Error saving image for page " + pageNumber + ": " + e.getMessage());
//            }
//        }
//    }

}

