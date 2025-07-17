package app;

import javax.swing.*;

/**
 * Main class for the PDF Converter application.
 * This class contains the entry point of the application, launching the user interface.
 */
public class Main {

    /**
     * The main method, which serves as the entry point for the Java application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PdfSelectorGUI frame = new PdfSelectorGUI();
            frame.setLocationRelativeTo(null); 
            frame.setVisible(true);
        });
    }
}
