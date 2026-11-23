import java.awt.*;
import javax.swing.*;

/** Task 3.4: renders the screen at three window sizes and prints how each region behaved. */
public class LibraryShots {
    public static void main(String[] args) throws Exception {
        int[][] sizes = {{420, 300}, {700, 440}, {1000, 640}};
        LibraryPanel p = new LibraryPanel(); p.query.setText("java"); p.doSearch();
        System.out.printf("%-12s | %-22s | %-22s | %-24s | %-22s%n", "window", "results area (WxH)", "search row (WxH)", "query field width", "button row (WxH)");
        for (int[] s : sizes) {
            Shot.render(p, s[0], s[1], "Campus Library Search", "shot_3_nested_" + s[0] + "x" + s[1] + ".png");
            System.out.printf("%-12s | %-22s | %-22s | %-24s | %-22s%n", s[0] + "x" + s[1], p.resultsPane.getWidth() + "x" + p.resultsPane.getHeight(), p.searchRow.getWidth() + "x" + p.searchRow.getHeight(), p.query.getWidth(), p.buttons.getWidth() + "x" + p.buttons.getHeight());
        }
        System.out.println();
        LayoutVariants.Absolute a = new LayoutVariants.Absolute();
        System.out.printf("%-12s | %-22s | %-24s%n", "window", "absolute: results area", "absolute: query width");
        for (int[] s : sizes) { Shot.render(a, s[0], s[1], "Absolute positioning", "shot_3_absolute_" + s[0] + "x" + s[1] + ".png"); System.out.printf("%-12s | %-22s | %-24s%n", s[0] + "x" + s[1], a.results.getWidth() + "x" + a.results.getHeight(), a.query.getWidth()); }
        System.out.println(); LayoutVariants.GridBag g = new LayoutVariants.GridBag();
        for (int[] s : sizes) Shot.render(g, s[0], s[1], "GridBagLayout", "shot_3_gridbag_" + s[0] + "x" + s[1] + ".png");
    }
}
