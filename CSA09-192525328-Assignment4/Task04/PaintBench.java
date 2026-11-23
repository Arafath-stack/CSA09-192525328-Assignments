import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/** Task 4 alternative concept: redraw n stored points in paint() versus copy one off-screen BufferedImage. */
public class PaintBench {
    public static void main(String[] args) throws Exception {
        int W = 400, H = 300; Random r = new Random(1); PrintWriter csv = new PrintWriter("paint_times.csv"); csv.println("n,approach,rep,us_per_repaint");
        BufferedImage screen = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB); Graphics2D sg = screen.createGraphics();
        System.out.printf("%-9s %-34s %18s %14s%n", "n clicks", "approach", "us per repaint", "memory");
        for (int n : new int[]{100, 1_000, 10_000, 100_000}) {
            List<Point> pts = new ArrayList<>(); for (int i = 0; i < n; i++) pts.add(new Point(r.nextInt(W), r.nextInt(H)));
            BufferedImage buf = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB); Graphics2D bg = buf.createGraphics(); bg.setColor(Color.WHITE); bg.fillRect(0, 0, W, H); bg.setColor(Color.BLACK);
            for (Point p : pts) bg.fillOval(p.x - 3, p.y - 3, 6, 6);
            for (String ap : new String[]{"redraw n stored points", "copy one BufferedImage"}) {
                int reps = ap.startsWith("redraw") ? Math.max(5, 20_000 / n * 5) : 2000; double[] us = new double[7];
                for (int rep = -2; rep < 7; rep++) {
                    long t0 = System.nanoTime();
                    for (int k = 0; k < reps; k++) {
                        sg.setColor(Color.WHITE); sg.fillRect(0, 0, W, H); sg.setColor(Color.BLACK);
                        if (ap.startsWith("redraw")) for (Point p : pts) sg.fillOval(p.x - 3, p.y - 3, 6, 6); else sg.drawImage(buf, 0, 0, null);
                    }
                    double d = (System.nanoTime() - t0) / 1e3 / reps; if (rep >= 0) { us[rep] = d; csv.printf("%d,%s,%d,%.2f%n", n, ap, rep, d); }
                }
                double m = Arrays.stream(us).average().orElse(0);
                System.out.printf("%-9d %-34s %18.1f %14s%n", n, ap, m, ap.startsWith("redraw") ? "~" + (n * 28L) / 1024 + " KB (n Points)" : (W * H * 4L) / 1024 + " KB (W x H x 4 bytes)");
            }
        }
        csv.close(); System.out.println("(a java.awt.Point is about 24 bytes + a 4-byte list slot, so n Points = about 28 n bytes)");
    }
}
