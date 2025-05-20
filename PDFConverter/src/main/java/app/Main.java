package app;

import javax.swing.*; // Import for Swing GUI components, including SwingUtilities and JFrame (via PdfSelectorGUI inheritance)

/**
 * Main class for the PDF Converter application.
 * This class contains the entry point of the application, launching the user interface.
 */
public class Main {

    /**
     * The main method, which serves as the entry point for the Java application.
     * @param args Command line arguments (not used in this application).
     */
    public static void main(String[] args) {
        // SwingUtilities.invokeLater is a static method from the javax.swing.SwingUtilities class.
        // It ensures that the GUI creation and updates are done on the Event Dispatch Thread (EDT),
        // which is the proper way to handle Swing GUI operations to prevent threading issues.
        SwingUtilities.invokeLater(() -> {
            // Creates an instance of our custom PdfSelectorGUI class (defined in app.PdfSelectorGUI.java).
            // PdfSelectorGUI likely extends javax.swing.JFrame or another Swing container.
            PdfSelectorGUI frame = new PdfSelectorGUI();

            // frame.setLocationRelativeTo is a method inherited from java.awt.Window (superclass of javax.swing.JFrame).
            // Passing null centers the frame on the screen.
            frame.setLocationRelativeTo(null); // centers the JFrame on the screen

            // frame.setVisible is a method inherited from java.awt.Component (superclass of java.awt.Window).
            // Passing true makes the frame and its contents visible to the user.
            frame.setVisible(true);
        });
    }
}
