import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/** Shows a message dialog, or records it when there is no display (headless test runs). */
public class UiMessages {
    public static final List<String> recorded = new ArrayList<>();
    public static void info(Component parent, String title, String text) {
        recorded.add(title + ": " + text.replace('\n', '|'));
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(parent, text, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
