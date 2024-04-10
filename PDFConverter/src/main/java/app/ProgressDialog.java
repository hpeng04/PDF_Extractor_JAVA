package app;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {
    private final JProgressBar progressBar;

    public ProgressDialog(Frame parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setValue(0);

        add(progressBar, BorderLayout.CENTER);
        add(new JLabel("Please wait..."), BorderLayout.PAGE_START);
        setSize(300, 100);
        setLocationRelativeTo(parent);
    }

    public void showDialog() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    public void updateProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(value);
            String message = String.format("%d%%", value);
            progressBar.setString(message); // Update the displayed string
        });
    }

    public void closeDialog() {
        SwingUtilities.invokeLater(this::dispose);
    }
}

