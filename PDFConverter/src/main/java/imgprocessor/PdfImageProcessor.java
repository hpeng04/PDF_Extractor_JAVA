package imgprocessor;

import app.ProgressDialog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PdfImageProcessor {

    public static Map<Integer, BufferedImage> convert(String filePath) throws IOException {
        Map<Integer, BufferedImage> cachedImages = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage img = cropPage(pdfRenderer, page);
                cachedImages.put(page, img);
            }
        }
        return cachedImages;
    }

    public static Map<Integer, BufferedImage> convert(File file, ProgressDialog dialog, AtomicInteger progress, int numFiles) throws IOException {
        Map<Integer, BufferedImage> cachedImages = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            double p = progress.get();
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage img = cropPage(pdfRenderer, page);
                cachedImages.put(page, img);
                p += 50.0 / (numFiles * document.getNumberOfPages());
                dialog.updateProgress(p);
            }
            progress.set((int) p);
        }
        return cachedImages;
    }

    private static BufferedImage cropPage(PDFRenderer renderer, int pageNumber) throws IOException {
        BufferedImage image = renderer.renderImage(pageNumber, 5, ImageType.BINARY);
        int width = image.getWidth();
        int height = image.getHeight();

        int top = (int) (height * 0.05);
        int bottom = (int) (height * 1);

        // TODO: delete
////         Get the user's desktop directory
//        String userHome = System.getProperty("user.home");
//        File desktopDir = new File(userHome, "Desktop/test_pngs/scanned");
//
//        // Save the cropped image to the desktop
//        File outputfile = new File(desktopDir, "cropped_image_page_" + pageNumber + ".png");
//        ImageIO.write(image, "png", outputfile);

        return image.getSubimage(0, top, width, bottom - top);
    }

}

