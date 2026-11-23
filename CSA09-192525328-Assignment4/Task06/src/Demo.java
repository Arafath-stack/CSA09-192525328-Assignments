import app.dao.*;
import app.model.Event;
import app.service.RegistrationService;
import app.ui.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import javax.swing.*;

/** Headless demonstration of Task 6: normal, invalid and duplicate registrations, filtering, paging and export. */
public class Demo {
    static MainPanel main; static RegisterPanel reg; static RegistrationsPanel view; static int pass, fail;
    static void edt(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }
    static void check(String id, String what, Object expected, Object actual) { boolean ok = String.valueOf(expected).equals(String.valueOf(actual)); if (ok) pass++; else fail++; System.out.printf("%-4s %-52s expected=%-46s actual=%-46s %s%n", id, what, expected, actual, ok ? "PASS" : "FAIL"); }
    static void waitIdle() throws Exception { long end = System.currentTimeMillis() + 5000; Thread.sleep(60); while ((reg.isBusy() || view.isBusy() || main.isExporting()) && System.currentTimeMillis() < end) Thread.sleep(10); Thread.sleep(80); }
    static <T> T field(Object o, String name) throws Exception { java.lang.reflect.Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return (T) f.get(o); }
    static void shot(String file, String title) throws Exception { edt(() -> { try { JPanel w = new JPanel(new BorderLayout()); w.add(main.menuBar, BorderLayout.NORTH); w.add(main, BorderLayout.CENTER); Shot.render(w, 760, 440, title, file); w.removeAll(); } catch (Exception e) { throw new RuntimeException(e); } }); }
    static void fill(String r, String n, String e, int evIndex) throws Exception { edt(() -> { JTextField a = field0("regNo"), b = field0("name"), c = field0("email"); a.setText(r); b.setText(n); c.setText(e); ((JComboBox<?>) field0("event")).setSelectedIndex(evIndex); }); }
    @SuppressWarnings("unchecked") static <T> T field0(String n) { try { return (T) field(reg, n); } catch (Exception e) { throw new RuntimeException(e); } }
    @SuppressWarnings("unchecked") static <T> T vfield(String n) { try { return (T) field(view, n); } catch (Exception e) { throw new RuntimeException(e); } }

    public static void main(String[] args) throws Exception {
        InMemoryRegistrationDao dao = new InMemoryRegistrationDao().seed(45); RegistrationService svc = new RegistrationService(dao);
        edt(() -> { main = new MainPanel(svc); reg = field0main("registerPanel"); view = field0main("viewPanel"); }); waitIdle();
        JLabel status = field0("status");
        System.out.println("== Screen 1: registration form");
        shot("shot_6_1_form_empty.png", "Campus Event Registration - New registration");
        System.out.println("\n== Stage 5 demonstration: normal, invalid and duplicate registrations");
        long before = dao.count(0);
        fill("192399001", "Anitha Raj", "anitha.raj@college.edu", 2); edt(() -> ((JButton) field0("register")).doClick()); waitIdle();
        check("N1", "normal registration: message", "Registered Anitha Raj (192399001) for Battle of Bands.", status.getText());
        check("N2", "normal registration: row stored", before + 1, dao.count(0));
        shot("shot_6_2_normal_registration.png", "Campus Event Registration - New registration");
        String[][] bad = {{"1923990", "Anitha Raj", "a@college.edu", "1"}, {"192399002", "A", "a@college.edu", "1"}, {"192399002", "Anitha Raj", "not-an-email", "1"}, {"192399002", "Anitha Raj", "a@college.edu", "0"}};
        String[] expect = {"Register number must be exactly 9 digits.", "Name must be 2 to 60 letters (spaces, dots, apostrophes and hyphens allowed).", "Enter a valid e-mail address.", "Select an event."};
        long rowsNow = dao.count(0);
        for (int i = 0; i < bad.length; i++) { fill(bad[i][0], bad[i][1], bad[i][2], Integer.parseInt(bad[i][3])); edt(() -> ((JButton) field0("register")).doClick()); waitIdle(); check("I" + (i + 1), "invalid input #" + (i + 1), expect[i], status.getText()); if (i == 2) shot("shot_6_3_invalid_input.png", "Campus Event Registration - New registration"); }
        check("I5", "invalid inputs store nothing", rowsNow, dao.count(0));
        UiMessages.recorded.clear(); fill("192399001", "Anitha Raj", "anitha.raj@college.edu", 2); edt(() -> ((JButton) field0("register")).doClick()); waitIdle();
        check("D1", "duplicate registration: same student, same event", "Student 192399001 is already registered for Battle of Bands.", status.getText());
        check("D2", "duplicate: warning dialog shown", "Duplicate registration: Student 192399001 is already registered for Battle of Bands.", UiMessages.recorded.isEmpty() ? "-" : UiMessages.recorded.get(0));
        check("D3", "duplicate: nothing stored", rowsNow + 0, dao.count(0));
        shot("shot_6_4_duplicate.png", "Campus Event Registration - New registration");
        fill("192399001", "Anitha Raj", "anitha.raj@college.edu", 3); edt(() -> ((JButton) field0("register")).doClick()); waitIdle();
        check("D4", "same student, DIFFERENT event is allowed", "Registered Anitha Raj (192399001) for Drama Fest.", status.getText());

        System.out.println("\n== Screen 2: registrations table (paged, filtered)");
        edt(() -> main.showScreen("view")); edt(() -> view.loadPage()); waitIdle();
        JTable table = vfield("table"); JLabel pageLabel = vfield("pageLabel"); JComboBox<Event> filter = vfield("filter");
        check("T1", "page 1 of all events: 20 rows, label", "20 | Page 1 of 3  (47 registrations)", table.getRowCount() + " | " + pageLabel.getText());
        shot("shot_6_5_table_page1.png", "Campus Event Registration - Registrations");
        edt(() -> ((JButton) vfield("next")).doClick()); waitIdle(); check("T2", "Next: page 2 shown", "Page 2 of 3  (47 registrations)", pageLabel.getText());
        edt(() -> ((JButton) vfield("next")).doClick()); waitIdle(); check("T3", "Next: last page has the remaining 7 rows", "7", table.getRowCount());
        edt(() -> filter.setSelectedIndex(2)); waitIdle(); long exp = dao.count(2);
        check("T4", "filter 'Battle of Bands': only that event, from page 1", "true | 1", allEvent(table, "Battle of Bands") + " | " + (pageLabel.getText().contains("Page 1") ? 1 : 0));
        check("T5", "filter count equals database count", exp + " registrations", pageLabel.getText().replaceAll(".*\\((\\d+ registrations)\\)", "$1"));
        shot("shot_6_6_table_filtered.png", "Campus Event Registration - Registrations (filter: Battle of Bands)");
        File csv = new File("registrations_battle_of_bands.csv"); edt(() -> main.exportTo(csv)); waitIdle();
        check("E1", "export CSV message", "Exported " + exp + " registrations to registrations_battle_of_bands.csv", field(main, "lastExportMessage"));
        java.util.List<String> lines = Files.readAllLines(csv.toPath()); check("E2", "CSV has a header and one line per row", exp + 1, lines.size());
        System.out.println("   CSV header: " + lines.get(0) + "\n   CSV line 1: " + lines.get(1));
        System.out.println("\nTotal: " + (pass + fail) + "   passed: " + pass + "   failed: " + fail); System.exit(0);
    }
    @SuppressWarnings("unchecked") static <T> T field0main(String n) { try { return (T) field(main, n); } catch (Exception e) { throw new RuntimeException(e); } }
    static boolean allEvent(JTable t, String ev) { for (int i = 0; i < t.getRowCount(); i++) if (!ev.equals(t.getValueAt(i, 3))) return false; return t.getRowCount() > 0; }
}
