import java.awt.*;
import javax.swing.*;

/** The same widgets laid out with GridBagLayout and with absolute positioning (setLayout(null) + setBounds). */
public class LayoutVariants {
    static abstract class Base extends JPanel {
        final JTextField query = new JTextField(18); final JComboBox<String> category = new JComboBox<>(new String[]{"All", "Books", "Journals", "Theses"}); final JButton search = new JButton("Search");
        final DefaultListModel<String> model = new DefaultListModel<>(); final JList<String> list = new JList<>(model); final JScrollPane results = new JScrollPane(list);
        final JButton prev = new JButton("Prev"), next = new JButton("Next"), reserve = new JButton("Reserve");
        final JLabel status = new JLabel("Ready"), title = new JLabel("Campus Library Search", SwingConstants.CENTER), lbl = new JLabel("Search:");
        Base() { for (String s : new String[]{"Java: The Complete Reference", "Effective Java", "Data Structures in Java", "Clean Code"}) model.addElement(s); title.setFont(title.getFont().deriveFont(Font.BOLD, 16f)); title.setOpaque(true); title.setBackground(new Color(220, 230, 245)); }
    }
    /** GridBagLayout: ONE layout manager for the whole screen; weights decide who grows. */
    static class GridBag extends Base {
        GridBag() {
            setLayout(new GridBagLayout()); GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(3, 4, 3, 4);
            c.gridx = 0; c.gridy = 0; c.gridwidth = 6; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; add(title, c);
            c.gridwidth = 1; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST; add(lbl, c);
            c.gridx = 1; add(query, c); c.gridx = 2; add(category, c); c.gridx = 3; add(search, c);
            c.gridx = 0; c.gridy = 2; c.gridwidth = 6; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH; add(results, c);
            c.gridwidth = 1; c.weighty = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE; c.gridy = 3; c.gridx = 1; add(prev, c); c.gridx = 2; add(next, c); c.gridx = 3; add(reserve, c);
            c.gridx = 0; c.gridy = 4; c.gridwidth = 6; c.fill = GridBagConstraints.HORIZONTAL; add(status, c);
        }
    }
    /** Absolute positioning: every component placed by hand; nothing reacts to resizing. */
    static class Absolute extends Base {
        Absolute() {
            setLayout(null);
            title.setBounds(0, 0, 540, 26); lbl.setBounds(10, 34, 52, 26); query.setBounds(66, 34, 190, 26); category.setBounds(264, 34, 90, 26); search.setBounds(362, 34, 80, 26);
            results.setBounds(10, 68, 520, 190); prev.setBounds(150, 266, 70, 28); next.setBounds(226, 266, 70, 28); reserve.setBounds(302, 266, 90, 28); status.setBounds(10, 300, 520, 20);
            add(title); add(lbl); add(query); add(category); add(search); add(results); add(prev); add(next); add(reserve); add(status);
        }
    }
    static int panels(Container c) { int n = (c instanceof JPanel) ? 1 : 0; for (Component x : c.getComponents()) if (x instanceof Container k) n += panels(k); return n; }

    public static void main(String[] args) throws Exception {
        JPanel[] v = { new LibraryPanel(), new GridBag(), new Absolute() }; String[] names = { "nested BorderLayout + FlowLayout", "GridBagLayout", "absolute (null layout)" };
        int[][] sz = {{500, 350}, {900, 600}}; int passes = 4000;
        System.out.printf("%-36s %16s %26s %10s%n", "design", "JPanel objects", "layout pass time (us)", "(sd)");
        java.io.PrintWriter csv = new java.io.PrintWriter("layout_times.csv"); csv.println("design,rep,us_per_layout_pass");
        for (int i = 0; i < 3; i++) {
            JPanel p = v[i]; double[] us = new double[7];
            for (int rep = -3; rep < 7; rep++) {
                long t0 = System.nanoTime();
                for (int k = 0; k < passes; k++) { int[] s = sz[k % 2]; p.setSize(s[0], s[1]); Shot.layoutAll(p); }
                double d = (System.nanoTime() - t0) / 1e3 / passes; if (rep >= 0) { us[rep] = d; csv.printf("%s,%d,%.2f%n", names[i], rep, d); }
            }
            double m = 0; for (double x : us) m += x; m /= us.length; double sd = 0; for (double x : us) sd += (x - m) * (x - m); sd = Math.sqrt(sd / us.length);
            System.out.printf("%-36s %16d %26.1f %10.1f%n", names[i], panels(p) - 0, m, sd);
        }
        csv.close();
    }
}
