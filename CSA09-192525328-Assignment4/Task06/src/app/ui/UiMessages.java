package app.ui;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/** Shows a dialog, or records the text when there is no display (headless test runs). */
public class UiMessages {
    public static final List<String> recorded = new ArrayList<>();
    public static void show(Component parent, String title, String text, int type) {
        recorded.add(title + ": " + text);
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(parent, text, title, type);
    }
}
