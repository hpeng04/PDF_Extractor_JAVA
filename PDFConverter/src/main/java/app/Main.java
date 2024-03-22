package app;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PdfSelectorGUI frame = new PdfSelectorGUI();
            frame.setLocationRelativeTo(null); // centers the JFrame on the screen
            frame.setVisible(true);
        });
    }
}
