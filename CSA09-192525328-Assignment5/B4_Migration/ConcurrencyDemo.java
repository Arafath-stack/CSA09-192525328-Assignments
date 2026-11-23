import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * B4 Alternative - several counters issue passes at the same time.
 *  (1) Serialized-file design: each counter loads the whole list, adds a pass, saves the whole list  (read-modify-write)
 *  (2) Database design: each counter has its own connection and inserts a row
 */
public class ConcurrencyDemo {
    static final int COUNTERS = 4, PER = 100;

    static class Pass implements Serializable { private static final long serialVersionUID = 1L; String id; Pass(String id) { this.id = id; } }

    @SuppressWarnings("unchecked")
    static List<Pass> load(Path p) throws Exception {
        if (!Files.exists(p) || Files.size(p) == 0) return new ArrayList<>();
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(p)))) { return (List<Pass>) in.readObject(); }
    }
    static void save(Path p, List<Pass> l) throws Exception {
        try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(p)))) { o.writeObject(l); }
    }

    public static void main(String[] args) throws Exception {
        // ---------- (1) shared serialized file, NO coordination between counters ----------
        Path f = Files.createTempFile("campus", ".ser");
        Files.delete(f);
        AtomicInteger errors = new AtomicInteger();
        Thread[] ts = new Thread[COUNTERS];
        for (int c = 0; c < COUNTERS; c++) {
            final int id = c;
            ts[c] = new Thread(() -> {
                for (int i = 0; i < PER; i++) {
                    try { List<Pass> l = load(f); l.add(new Pass("C" + id + "-" + i)); save(f, l); }
                    catch (Exception e) { errors.incrementAndGet(); }
                }
            });
        }
        for (Thread t : ts) t.start(); for (Thread t : ts) t.join();
        int finalCount;
        try { finalCount = load(f).size(); } catch (Exception e) { finalCount = -1; }
        System.out.println("(1) Serialized file shared by " + COUNTERS + " counters x " + PER + " passes each:");
        System.out.println("    passes issued = " + (COUNTERS * PER) + " | passes found in file afterwards = " + (finalCount < 0 ? "FILE UNREADABLE (corrupted)" : finalCount) + " | I/O errors = " + errors.get());
        System.out.println("    -> " + (finalCount < 0 ? "file corrupted" : (COUNTERS * PER - finalCount) + " passes silently LOST (lost-update problem)"));
        Files.deleteIfExists(f);

        // ---------- (2) database, one connection per counter ----------
        try (Connection setup = DbConfig.connect(); Statement st = setup.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
        AtomicInteger dbErrors = new AtomicInteger();
        for (int c = 0; c < COUNTERS; c++) {
            final int id = c;
            ts[c] = new Thread(() -> {
                try (Connection con = DbConfig.connect();
                     PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?,?,?,?,?)")) {
                    for (int i = 0; i < PER; i++) {
                        ps.setString(1, "C" + id + "-" + i); ps.setString(2, "192310001"); ps.setString(3, "Student"); ps.setInt(4, 1); ps.setDouble(5, 3500);
                        ps.executeUpdate();
                    }
                } catch (SQLException e) { dbErrors.incrementAndGet(); }
            });
        }
        for (Thread t : ts) t.start(); for (Thread t : ts) t.join();
        try (Connection con = DbConfig.connect(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            rs.next();
            System.out.println("(2) Database used by " + COUNTERS + " counters x " + PER + " passes each:");
            System.out.println("    passes issued = " + (COUNTERS * PER) + " | rows in table afterwards = " + rs.getInt(1) + " | errors = " + dbErrors.get() + "  -> nothing lost");
        }

        // ---------- (3) two counters issue the SAME pass id at the same moment ----------
        try (Connection setup = DbConfig.connect(); Statement st = setup.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        Thread[] two = new Thread[2];
        for (int c = 0; c < 2; c++) {
            two[c] = new Thread(() -> {
                try (Connection con = DbConfig.connect(); PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ('P-SAME','192310001','X',1,3500)")) {
                    ps.executeUpdate(); ok.incrementAndGet();
                } catch (SQLException e) { rejected.incrementAndGet(); }
            });
        }
        for (Thread t : two) t.start(); for (Thread t : two) t.join();
        System.out.println("(3) Two counters insert the same pass id simultaneously: accepted=" + ok.get() + ", rejected by PRIMARY KEY=" + rejected.get());
        try (Connection setup = DbConfig.connect(); Statement st = setup.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
    }
}
