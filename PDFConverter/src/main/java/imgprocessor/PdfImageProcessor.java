package imgprocessor;

import app.ProgressDialog;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
// import utils.AtomicDoubleBackedByAtomicLong;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        Map<Integer, BufferedImage> cachedImages = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            int totalPages = document.getNumberOfPages();
            List<Future<Void>> futures = new ArrayList<>();
            
            for (int page = 0; page < totalPages; page++) {
                final int pageNum = page;
                Future<Void> future = executor.submit(() -> {
                    try (PDDocument threadDocument = Loader.loadPDF(new File(filePath))) {
                        PDFRenderer pdfRenderer = new PDFRenderer(threadDocument);
                        BufferedImage img = cropPage(pdfRenderer, pageNum);
                        cachedImages.put(pageNum, img);
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing page " + pageNum, e);
                    }
                });
                futures.add(future);
            }
            
            // Wait for all tasks and handle exceptions
            for (Future<Void> future : futures) {
                future.get(); // This will throw if any task failed
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException();
        } finally {
            executor.shutdown();
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
    public static Map<Integer, BufferedImage> convert(File file, ProgressDialog dialog, 
                                                    AtomicInteger progress, int numFiles) throws IOException {
        Map<Integer, BufferedImage> cachedImages = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        try (PDDocument document = Loader.loadPDF(file)) {
            int totalPages = document.getNumberOfPages();
            List<Future<Void>> futures = new ArrayList<>();
            
            // Calculate progress increment per page
            double progressPerPage = 50.0 / (numFiles * totalPages);
            
            for (int page = 0; page < totalPages; page++) {
                final int pageNum = page;
                Future<Void> future = executor.submit(() -> {
                    // Each thread loads its own document instance
                    try (PDDocument threadDocument = Loader.loadPDF(file)) {
                        PDFRenderer pdfRenderer = new PDFRenderer(threadDocument);
                        BufferedImage img = cropPage(pdfRenderer, pageNum);
                        cachedImages.put(pageNum, img);
                        
                        // Update progress atomically
                        double newProgress = progress.addAndGet((int) progressPerPage);
                        dialog.updateProgress(newProgress);
                        
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing page " + pageNum, e);
                    }
                });
                futures.add(future);
            }
            
            // Wait for all tasks and check for failures
            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    throw new IOException("Page processing failed", e.getCause());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF processing was interrupted", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
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

        return image.getSubimage(0, top, width, bottom - top);
    }

}

