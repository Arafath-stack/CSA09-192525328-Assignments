import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * Task 3: Campus Library search screen (Swing).
 *   LibraryPanel (BorderLayout)
 *     NORTH  : JPanel top (BorderLayout)   - title JLabel (NORTH) + JPanel searchRow (FlowLayout.LEFT, CENTER)
 *     CENTER : JScrollPane(JList results)  - receives ALL extra space when the window is resized
 *     SOUTH  : JPanel bottom (BorderLayout) - JPanel buttons (FlowLayout.CENTER, CENTER) + status JLabel (SOUTH)
 */
public class LibraryPanel extends JPanel {
    static final String[][] CATALOGUE = {
        {"Books", "Java: The Complete Reference"}, {"Books", "Effective Java"}, {"Books", "Data Structures in Java"}, {"Books", "Operating System Concepts"},
        {"Books", "Computer Networks"}, {"Books", "Database System Concepts"}, {"Books", "Introduction to Algorithms"}, {"Books", "Clean Code"},
        {"Journals", "Journal of Software Engineering"}, {"Journals", "IEEE Transactions on Computers"}, {"Journals", "ACM Computing Surveys"},
        {"Theses", "Design of a Parking Allocation System"}, {"Theses", "Event Driven GUI Frameworks"}, {"Theses", "Java Collections Performance Study"} };
    static final int PAGE = 5;

    final JTextField query = new JTextField(18);
    final JComboBox<String> category = new JComboBox<>(new String[]{"All", "Books", "Journals", "Theses"});
    final JButton search = new JButton("Search");
    final DefaultListModel<String> model = new DefaultListModel<>();
    final JList<String> results = new JList<>(model);
    final JScrollPane resultsPane = new JScrollPane(results);
    final JButton prev = new JButton("Prev"), next = new JButton("Next"), reserve = new JButton("Reserve");
    final JLabel status = new JLabel("Ready", SwingConstants.LEFT);
    final JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT)), buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private final java.util.List<String> hits = new ArrayList<>();
    private int page = 0;

    public LibraryPanel() {
        super(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Campus Library Search", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f)); title.setOpaque(true); title.setBackground(new Color(220, 230, 245));
        top.add(title, BorderLayout.NORTH);
        searchRow.add(new JLabel("Search:")); searchRow.add(query); searchRow.add(category); searchRow.add(search);
        top.add(searchRow, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(resultsPane, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout());
        buttons.add(prev); buttons.add(next); buttons.add(reserve);
        bottom.add(buttons, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        bottom.add(status, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        search.addActionListener(e -> doSearch());
        query.addActionListener(e -> doSearch());
        prev.addActionListener(e -> { if (page > 0) { page--; showPage(); } });
        next.addActionListener(e -> { if ((page + 1) * PAGE < hits.size()) { page++; showPage(); } });
        reserve.addActionListener(e -> status.setText(results.getSelectedValue() == null ? "Select a title first" : "Reserved: " + results.getSelectedValue()));
    }

    void doSearch() {
        String q = query.getText().trim().toLowerCase(); String cat = (String) category.getSelectedItem();
        hits.clear();
        for (String[] row : CATALOGUE) if ((cat.equals("All") || cat.equals(row[0])) && row[1].toLowerCase().contains(q)) hits.add(row[1]);
        page = 0; showPage();
    }
    void showPage() {
        model.clear();
        int from = page * PAGE, to = Math.min(hits.size(), from + PAGE);
        for (int i = from; i < to; i++) model.addElement(hits.get(i));
        status.setText(hits.isEmpty() ? "No titles found" : "Showing " + (from + 1) + "-" + to + " of " + hits.size());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> { JFrame f = new JFrame("Campus Library Search"); f.setContentPane(new LibraryPanel()); f.setSize(560, 380);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); f.setVisible(true); });
    }
}
