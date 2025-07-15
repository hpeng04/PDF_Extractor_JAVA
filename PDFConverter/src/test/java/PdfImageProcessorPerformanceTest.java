import imgprocessor.PdfImageProcessor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Performance test to compare the speed of the optimized PdfImageProcessor.
 * This test is disabled by default to avoid dependency on specific PDF files.
 */
// @Disabled("Performance test - enable manually with a test PDF")
public class PdfImageProcessorPerformanceTest {

    @Test
    public void testProcessingSpeed() throws IOException {
        // Replace with path to your test PDF
        String testPdfPath = "/Users/hhpeng/Documents/Work/PDF_Extractor_JAVA/Test 4_imperial.pdf";
        File testPdf = new File(testPdfPath);
        
        if (!testPdf.exists()) {
            System.out.println("Test PDF not found: " + testPdfPath);
            return;
        }

        System.out.println("Testing PDF processing performance...");
        System.out.println("PDF: " + testPdf.getName());
        
        // Test the optimized version
        long startTime = System.currentTimeMillis();
        Map<Integer, BufferedImage> images = PdfImageProcessor.convert(testPdfPath);
        long endTime = System.currentTimeMillis();
        
        long processingTime = endTime - startTime;
        int pageCount = images.size();
        
        System.out.println("Results:");
        System.out.println("- Pages processed: " + pageCount);
        System.out.println("- Total time: " + processingTime + "ms");
        System.out.println("- Average per page: " + (processingTime / (double) pageCount) + "ms");
        System.out.println("- Available CPU cores: " + Runtime.getRuntime().availableProcessors());
        
        // Clean up memory
        images.clear();
        System.gc();
    }
}
