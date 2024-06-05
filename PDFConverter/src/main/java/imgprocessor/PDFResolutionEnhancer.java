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

public class PDFResolutionEnhancer {

    private static final String userHome = System.getProperty("user.home");
    private static final File desktopDir = new File(userHome, "Desktop/");

    static {
        java.net.URL url = ClassLoader.getSystemResource("opencv/x64/opencv_java490.dll");
        System.load(url.getPath());
    }

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

    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat;
        if (bi.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC1);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (bi.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
            byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
            mat.put(0, 0, data);
        } else if (bi.getType() == BufferedImage.TYPE_INT_RGB) {
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
        return null;
    }
}
