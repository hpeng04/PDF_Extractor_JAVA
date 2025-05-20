package imgprocessor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.Loader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.net.URL;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.Core;
import org.opencv.core.MatOfInt;
import readers.Reader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Utility class to enhance the resolution of images within PDF files using OpenCV.
 * Contains a `main` method for testing, which loads a PDF, enhances its pages, and saves them as PNGs.
 * Provides helper methods to convert between {@link BufferedImage} and OpenCV {@link Mat} objects.
 */
public class PDFResolutionEnhancer {

    // Static fields for a default output directory (Desktop) - primarily for the test main method.
    private static final String userHome = System.getProperty("user.home");
    private static final File desktopDir = new File(userHome, "Desktop/");

    /**
     * Static initializer block to load the OpenCV native library.
     * It attempts to find `opencv_java490.dll` (for Windows) in the classpath resources (e.g., in a `opencv/x64` folder).
     * The exact path and library name might need adjustment based on the OS and OpenCV version.
     */
    static {
        // java.net.URL is used here to get the path of the resource.
        java.net.URL url = ClassLoader.getSystemResource("opencv/x64/opencv_java490.dll");
        if (url != null) {
            // System.load expects an absolute path to the native library.
            System.load(url.getPath().replaceFirst("^/", "")); // remove leading slash if present for windows paths
        } else {
            System.err.println("OpenCV native library not found. Please ensure opencv_java490.dll is in the classpath.");
        }
    }

    /**
     * Main method for testing the PDF resolution enhancement.
     * Reads a PDF named "50P-scan.pdf" from the Desktop, processes each page to enhance its resolution by a factor of 2,
     * and saves the enhanced images as PNG files in a "Desktop/test_pngs/scanned" directory.
     *
     * @param args Command line arguments (not used).
     * @throws IOException If an error occurs during file I/O or PDF processing.
     */
    public static void main(String[] args) throws IOException {

        File inputFile = new File(desktopDir, "50P-scan.pdf");
        PDDocument document = Loader.loadPDF(inputFile);
        PDFRenderer pdfRenderer = new PDFRenderer(document);

//        PDDocument outputDocument = new PDDocument();

        for (int page = 0; page < document.getNumberOfPages(); ++page) {
            BufferedImage bufferedImage = pdfRenderer.renderImage(page, 5, ImageType.RGB);

            // Convert BufferedImage to OpenCV Mat
            Mat src = bufferedImageToMat(bufferedImage);

            // Enhance resolution using OpenCV
            Mat dst = new Mat();
            Imgproc.resize(src, dst, new Size(src.cols() * 2, src.rows() * 2), 0, 0, Imgproc.INTER_CUBIC);

            // Convert back to BufferedImage
            BufferedImage enhancedImage = matToBufferedImage(dst);

    //         Get the user's desktop directory
            String userHome = System.getProperty("user.home");
            File desktopDir = new File(userHome, "Desktop/test_pngs/scanned");

            // Save the cropped image to the desktop
            File outputfile = new File(desktopDir, "cropped_image_page_" + page + ".png");
            ImageIO.write(enhancedImage, "png", outputfile);
        }
        document.close();
        System.out.println("Processing complete.");
    }

    /**
     * Converts a {@link BufferedImage} to an OpenCV {@link Mat} object.
     * Handles different BufferedImage types (TYPE_BYTE_GRAY, TYPE_3BYTE_BGR, TYPE_INT_RGB).
     * If the type is not directly supported, it converts the image to TYPE_3BYTE_BGR first.
     *
     * @param bi The input {@link BufferedImage}.
     * @return The corresponding OpenCV {@link Mat} object.
     */
    private static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat;
        int type = bi.getType();
        if (type == BufferedImage.TYPE_BYTE_GRAY) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (type == BufferedImage.TYPE_3BYTE_BGR) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (type == BufferedImage.TYPE_INT_RGB) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
            int[] data = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
            byte[] bytes = new byte[data.length * 3];
            for (int i = 0; i < data.length; i++) {
                bytes[i * 3] = (byte) ((data[i] >> 16) & 0xFF);
                bytes[i * 3 + 1] = (byte) ((data[i] >> 8) & 0xFF);
                bytes[i * 3 + 2] = (byte) (data[i] & 0xFF);
            }
            mat.put(0, 0, bytes);
        } else {
            BufferedImage convertedImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = convertedImg.createGraphics();
            g.drawImage(bi, 0, 0, null);
            g.dispose();
            mat = bufferedImageToMat(convertedImg);
        }
        return mat;
    }

    /**
     * Converts an OpenCV {@link Mat} object to a {@link BufferedImage}.
     * Handles CV_8UC1 (grayscale) and CV_8UC3 (BGR color) Mat types.
     *
     * @param mat The input OpenCV {@link Mat} object.
     * @return The corresponding {@link BufferedImage}, or null if the Mat type is not supported.
     */
    private static BufferedImage matToBufferedImage(Mat mat) {
        if (mat.type() == CvType.CV_8UC1) {
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_BYTE_GRAY);
            byte[] data = new byte[mat.rows() * mat.cols()];
            mat.get(0, 0, data);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
            return image;
        } else if (mat.type() == CvType.CV_8UC3) {
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = new byte[mat.rows() * mat.cols() * (int) mat.elemSize()];
            mat.get(0, 0, data);
            image.getRaster().setDataElements(0, 0, mat.cols(), mat.rows(), data);
            return image;
        }
        System.err.println("Unsupported Mat type for conversion: " + mat.type());
        return null;
    }
}
