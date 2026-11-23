import java.awt.*;
import java.awt.event.*;

/** The defective program exactly as supplied (only the missing imports were added). */
public class DrawPad extends Frame {
    int count = 0; Label status = new Label("Clicks: 0");
    public DrawPad() {
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            public void mouseclicked(MouseEvent e) {
                count++;
                Graphics g = getGraphics(); g.fillOval(e.getX(), e.getY(), 6, 6);
            }
        });
        setSize(400, 300); setVisible(true);
    }
}
