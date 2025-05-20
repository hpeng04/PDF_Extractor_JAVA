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

/**
 * Processes PDF files to convert their pages into {@link BufferedImage} objects.
 * It can crop the rendered images and provides progress updates via a {@link ProgressDialog}.
 */
public class PdfImageProcessor {

    /**
     * Converts all pages of a PDF file (specified by path) into a map of page numbers to BufferedImages.
     * Each page image is cropped.
     *
     * @param filePath The absolute path to the PDF file.
     * @return A Map where keys are 0-indexed page numbers and values are the corresponding cropped {@link BufferedImage} objects.
     * @throws IOException If an error occurs during PDF loading or rendering.
     */
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

    /**
     * Converts all pages of a PDF file into a map of page numbers to BufferedImages, with progress updates.
     * Each page image is cropped.
     *
     * @param file The PDF {@link File} object to process.
     * @param dialog The {@link ProgressDialog} to update with progress information.
     * @param progress An {@link AtomicInteger} tracking the overall progress percentage.
     * @param numFiles The total number of files being processed (used to calculate incremental progress).
     * @return A Map where keys are 0-indexed page numbers and values are the corresponding cropped {@link BufferedImage} objects.
     * @throws IOException If an error occurs during PDF loading or rendering.
     */
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

    /**
     * Renders a specific page of a PDF as a BufferedImage and then crops it.
     * The top 5% of the image is cropped.
     *
     * @param renderer The {@link PDFRenderer} instance for the document.
     * @param pageNumber The 0-indexed page number to render and crop.
     * @return The cropped {@link BufferedImage}.
     * @throws IOException If an error occurs during image rendering.
     */
    private static BufferedImage cropPage(PDFRenderer renderer, int pageNumber) throws IOException {
        BufferedImage image = renderer.renderImage(pageNumber, 5, ImageType.BINARY);
        int width = image.getWidth();
        int height = image.getHeight();

        int top = (int) (height * 0.05);
        int bottom = height;

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

