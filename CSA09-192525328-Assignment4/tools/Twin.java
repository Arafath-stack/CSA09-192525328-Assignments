import java.util.ArrayList;
import java.util.List;

/** Receives the messages that the real program would show in a modal dialog box (twin classes call Twin.dialog). */
public class Twin {
    public static final List<String> messages = new ArrayList<>();
    public static void dialog(java.awt.Component parent, Object message) { messages.add(String.valueOf(message)); }
    public static void dialog(java.awt.Component parent, Object message, String title, int type) { messages.add(title + ": " + message); }
}
