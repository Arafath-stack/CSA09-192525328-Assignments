import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Task 4 evidence. DrawPadBuggyT / DrawPadFixedT are mechanically derived from buggy/DrawPad.java and DrawPad.java
 * (tools/make_twin.py): same listener and paint code, JPanel instead of Frame, because the build machine has no display.
 * Mouse clicks are delivered with dispatchEvent(MouseEvent.MOUSE_CLICKED), the same call the AWT event queue makes.
 */
public class DrawPadDemo {
    static void click(Component c, int x, int y) { c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false)); }
    static JLabel statusOf(Container c) { for (Component k : c.getComponents()) if (k instanceof JLabel l) return l; return null; }
    static int dark(BufferedImage im) { int n = 0; for (int y = 0; y < im.getHeight(); y++) for (int x = 0; x < im.getWidth(); x++) if ((im.getRGB(x, y) & 0xFFFFFF) == 0) n++; return n; }

    public static void main(String[] args) throws Exception {
        int[][] pts = {{80, 60}, {200, 120}, {320, 200}};
        System.out.println("== Task 4.1  Defective program: three clicks delivered to the window");
        DrawPadBuggyT b = new DrawPadBuggyT(); b.setSize(400, 300);
        for (int[] p : pts) click(b, p[0], p[1]);
        System.out.println("label text after 3 clicks : " + statusOf(b).getText() + "   (field count = " + b.count + ")");
        Shot.render(b, 400, 300, "Draw Pad (defective)", "shot_4_1_defective_after_3_clicks.png");

        System.out.println("\n== Task 4.2  First defect fixed, but dots drawn with getGraphics() instead of in paint()");
        // Stand-in for getGraphics(): the dots are drawn straight onto the visible surface, outside the paint cycle.
        BufferedImage screen = Shot.image(b, 400, 300); Graphics2D g = screen.createGraphics(); g.setColor(Color.BLACK);
        for (int[] p : pts) g.fillOval(p[0], p[1], 6, 6); g.dispose(); ImageIO.write(screen, "png", new File("shot_4_2_before_minimise_dots_visible.png"));
        System.out.println("dark pixels on the surface right after the clicks : " + dark(screen));
        BufferedImage restored = Shot.image(b, 400, 300);                   // minimise + restore = the window is asked to paint itself again
        ImageIO.write(restored, "png", new File("shot_4_2_after_restore_dots_gone.png"));
        System.out.println("dark pixels after minimise + restore (repaint)     : " + dark(restored) + "   -> the dots are gone: paint() did not know them");

        System.out.println("\n== Task 4.3  Repaired program (store points, draw them in paint())");
        DrawPadFixedT f = new DrawPadFixedT(); f.setSize(400, 300);
        for (int[] p : pts) click(f, p[0], p[1]);
        System.out.println("label text after 3 clicks : " + statusOf(f).getText());
        BufferedImage before = Shot.image(f, 400, 300); ImageIO.write(before, "png", new File("shot_4_3_before_minimise.png"));
        BufferedImage after = Shot.image(f, 400, 300);   ImageIO.write(after, "png", new File("shot_4_3_after_restore.png"));
        System.out.println("dark pixels before minimise : " + dark(before) + "   after restore : " + dark(after) + "   (equal = dots survive)");
        for (int i = 0; i < 2; i++) click(f, 50 + i * 40, 250);
        System.out.println("two more clicks -> label: " + statusOf(f).getText());
        Shot.render(f, 400, 300, "Draw Pad (repaired)", "shot_4_3_repaired_window.png");
    }
}
