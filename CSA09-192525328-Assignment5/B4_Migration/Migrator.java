import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * B4.1 - migrate the text file into MySQL.
 *   1. read the file with a BufferedReader
 *   2. validate every line (BusPassStore.parseLine) and detect duplicate pass IDs
 *   3. load the valid rows with PassLoader (batches of 1000, ONE transaction)
 *   4. write every rejected line + reason to a failure report
 */
public class Migrator {
    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args.length > 0 ? args[0] : "input/passes_legacy.csv");
        Path report = Paths.get(args.length > 1 ? args[1] : "outputs/failure_report.txt");
        Files.createDirectories(report.getParent());

        List<String[]> valid = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int lineNo = 0;
        long t0 = System.nanoTime();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(in), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                String[] why = new String[1];
                String[] f = BusPassStore.parseLine(line, why);
                if (f == null) { failures.add("line " + lineNo + ": " + why[0] + "  -> [" + line + "]"); continue; }
                if (!seen.add(f[0])) { failures.add("line " + lineNo + ": duplicate pass id " + f[0] + "  -> [" + line + "]"); continue; }
                f[2] = BusPassStore.decodeName(f[2]);
                valid.add(f);
            }
        }
        double readMs = (System.nanoTime() - t0) / 1e6;

        int loaded = 0; String dbStatus = "OK";
        try (Connection con = DbConfig.connect()) {
            try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }   // demo only: start from an empty table
            long t1 = System.nanoTime();
            try { loaded = PassLoader.loadAll(con, valid); }
            catch (SQLException | RuntimeException e) { dbStatus = "FAILED and rolled back: " + e.getMessage().split("\n")[0]; }
            double loadMs = (System.nanoTime() - t1) / 1e6;
            int inTable;
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); inTable = rs.getInt(1); }

            try (BufferedWriter w = Files.newBufferedWriter(report)) {
                w.write("Migration failure report for " + in + "\n");
                w.write("Lines read: " + lineNo + " | valid: " + valid.size() + " | rejected: " + failures.size() + "\n\n");
                for (String f : failures) { w.write(f); w.newLine(); }
            }
            System.out.println("=== Migration summary ===");
            System.out.println("Lines read              : " + lineNo);
            System.out.println("Valid records           : " + valid.size());
            System.out.println("Rejected (not migrated) : " + failures.size() + "   (details in " + report + ")");
            System.out.println("Rows loaded by PassLoader: " + loaded + "   [" + dbStatus + "]");
            System.out.println("Rows now in bus_pass    : " + inTable);
            System.out.println("Reconciliation          : " + lineNo + " read = " + inTable + " loaded + " + failures.size() + " rejected  -> " + ((inTable + failures.size() == lineNo) ? "MATCHES" : "MISMATCH"));
            System.out.printf("Time: read+validate %.0f ms, database load %.0f ms%n", readMs, loadMs);
            System.out.println("\n--- Rejected records ---");
            for (String f : failures) System.out.println(f);
        }
    }
}
