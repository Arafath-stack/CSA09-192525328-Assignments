import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * B4 Alternative - 12 campuses.  Each campus has ~66,667 passes (about 800,000 in total = 100 x 8,000).
 *   Design X: one serialized file per campus  -> to find a pass anywhere we must load up to 12 files.
 *   Design Y: one shared database table with a primary-key index.
 */
public class ScaleTest {
    static class Pass implements Serializable { private static final long serialVersionUID = 1L; String id, reg, name; int route; double amount;
        Pass(String id, String reg, String name, int route, double amount) { this.id = id; this.reg = reg; this.name = name; this.route = route; this.amount = amount; } }

    public static void main(String[] args) throws Exception {
        int campuses = 12, per = 66_667;
        Path dir = Files.createTempDirectory("campuses");
        long totalBytes = 0; List<Path> files = new ArrayList<>();
        for (int c = 0; c < campuses; c++) {
            List<Pass> l = new ArrayList<>(per);
            for (int i = 0; i < per; i++) { int k = c * per + i + 1; l.add(new Pass(String.format("P%07d", k), "19231" + String.format("%04d", k % 10000), "Student" + k, 1 + k % 40, 3500 + k % 1000)); }
            Path p = dir.resolve("campus" + c + ".ser");
            try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(p)))) { o.writeObject(l); }
            files.add(p); totalBytes += Files.size(p);
        }
        String target = String.format("P%07d", campuses * per);         // pass at the end of the LAST campus (worst case)

        // Design X : search all campus files
        double xs = 0;
        for (int rep = 0; rep < 3; rep++) {
            long t = System.nanoTime(); boolean found = false;
            Runtime rt = Runtime.getRuntime(); System.gc();
            for (Path p : files) {
                @SuppressWarnings("unchecked") List<Pass> l;
                try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(p)))) { l = (List<Pass>) in.readObject(); }
                for (Pass x : l) if (x.id.equals(target)) { found = true; break; }
                if (found) break;
            }
            xs = (System.nanoTime() - t) / 1e6;
        }
        long heapAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Design Y : database
        try (Connection con = DbConfig.connect()) {
            try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
            Path csv = dir.resolve("all.csv");
            try (BufferedWriter w = Files.newBufferedWriter(csv)) {
                for (int k = 1; k <= campuses * per; k++) w.write(String.format("P%07d", k) + ",19231" + String.format("%04d", k % 10000) + ",Student" + k + "," + (1 + k % 40) + "," + (3500 + k % 1000) + ".0\n");
            }
            try (Statement st = con.createStatement()) {
                long t = System.nanoTime();
                st.execute("LOAD DATA LOCAL INFILE '" + csv + "' INTO TABLE bus_pass FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n'");
                System.out.printf("(loaded %d rows into MySQL with LOAD DATA in %.0f ms)%n", campuses * per, (System.nanoTime() - t) / 1e6);
            }
            double ys = 0, yRoute = 0; int cnt = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM bus_pass WHERE pass_id=?")) {
                for (int i = 0; i < 200; i++) { ps.setString(1, target); try (ResultSet rs = ps.executeQuery()) { rs.next(); } }
                long t = System.nanoTime();
                for (int i = 0; i < 2000; i++) { ps.setString(1, target); try (ResultSet rs = ps.executeQuery()) { rs.next(); } }
                ys = (System.nanoTime() - t) / 1e3 / 2000;
            }
            long t = System.nanoTime();
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass WHERE route_no=12")) { rs.next(); cnt = rs.getInt(1); }
            yRoute = (System.nanoTime() - t) / 1e6;
            long tbl = 0;
            try (Statement st = con.createStatement()) { st.execute("ANALYZE TABLE bus_pass"); }   // refresh size statistics
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT data_length+index_length FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='bus_pass'")) { rs.next(); tbl = rs.getLong(1); }

            System.out.println("Total passes: " + campuses * per + " in " + campuses + " campuses");
            System.out.printf("Design X (12 serialized files): total size %.1f MB | find the last pass by loading files: %.0f ms | heap in use afterwards ~%d MB%n", totalBytes / 1048576.0, xs, heapAfter / 1048576);
            System.out.printf("Design Y (one MySQL table)    : table+index size %.1f MB | find by pass_id (primary key): %.0f microseconds%n", tbl / 1048576.0, ys);
            System.out.printf("                                 count of route-12 passes across all campuses (full scan, no index): %.0f ms (%d passes)%n", yRoute, cnt);
            System.out.printf("Speed-up for single pass lookup: about %.0f times%n", xs * 1000 / ys);
            try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
        }
    }
}
