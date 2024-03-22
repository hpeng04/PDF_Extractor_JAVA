package app;

import javax.swing.*;
import java.awt.*;

public class ProgressDialog extends JDialog {
    private final JProgressBar progressBar;

    public ProgressDialog(Frame parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        add(progressBar, BorderLayout.CENTER);
        add(new JLabel("Please wait..."), BorderLayout.PAGE_START);
        setSize(300, 100);
        setLocationRelativeTo(parent);
    }

    public void showDialog() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    public void closeDialog() {
        SwingUtilities.invokeLater(this::dispose);
    }
}

