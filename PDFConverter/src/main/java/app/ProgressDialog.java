package app;

import javax.swing.*;
import java.awt.*;

/**
 * A simple dialog to display progress of a task using a JProgressBar.
 * This dialog is modal, meaning it blocks interaction with its parent window until closed.
 * It extends {@link javax.swing.JDialog}.
 */
public class ProgressDialog extends JDialog {
    private final JProgressBar progressBar;

    /**
     * Constructor for ProgressDialog.
     * @param parent The parent Frame of this dialog. Can be null.
     * @param title The title of the dialog window.
     */
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

    /**
     * Makes the progress dialog visible.
     * Uses {@link javax.swing.SwingUtilities#invokeLater(Runnable)} to ensure this happens on the Event Dispatch Thread.
     */
    public void showDialog() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    /**
     * Updates the progress bar's value and displayed percentage string.
     * Uses {@link javax.swing.SwingUtilities#invokeLater(Runnable)} for thread safety.
     * @param value The new progress value (0-100).
     */
    public void updateProgress(double value) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue((int) value);
            String message = String.format("%d%%", (int) value);
            progressBar.setString(message);
        });
    }

    /**
     * Closes and disposes of the progress dialog.
     * Uses {@link javax.swing.SwingUtilities#invokeLater(Runnable)} for thread safety.
     */
    public void closeDialog() {
        SwingUtilities.invokeLater(this::dispose);
    }
}

