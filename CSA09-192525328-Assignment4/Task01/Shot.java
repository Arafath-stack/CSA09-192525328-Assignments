import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Off-screen "screenshot" helper: lays out a real Swing component tree and paints it to a PNG (works without a display). */
public class Shot {
    public static void layoutAll(Component c) {
        if (c instanceof Container k) { k.doLayout(); for (Component x : k.getComponents()) layoutAll(x); }
    }
    public static BufferedImage image(Component c, int w, int h) {
        c.setSize(w, h); layoutAll(c);
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics(); g.setColor(Color.WHITE); g.fillRect(0, 0, w, h);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.paint(g); g.dispose(); return im;
    }
    /** Renders c at w x h inside a simple window frame (title bar drawn by this helper) and saves it as PNG. */
    public static void render(Component c, int w, int h, String title, String file) throws Exception {
        BufferedImage body = image(c, w, h);
        BufferedImage out = new BufferedImage(w + 2, h + 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics(); g.setColor(new Color(40, 90, 160)); g.fillRect(0, 0, w + 2, h + 30);
        g.setColor(Color.WHITE); g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12)); g.drawString(title + "   [  _  []  X  ]", 8, 19);
        g.drawImage(body, 1, 29, null); g.dispose();
        ImageIO.write(out, "png", new File(file));
    }
}
