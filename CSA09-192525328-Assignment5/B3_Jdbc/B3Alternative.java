import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** B3 Alternative Concept: multi-row INSERT and LOAD DATA LOCAL INFILE compared with PassLoader batches. */
public class B3Alternative {
    static void clear(Connection con) throws SQLException {
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
    }
    static int count(Connection con) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); return rs.getInt(1); }
    }

    /** One INSERT statement carrying b rows, inside one transaction. Returns the largest SQL string length (memory indicator). */
    static int multiRow(Connection con, List<String[]> passes, int b) throws SQLException {
        con.setAutoCommit(false);
        int maxLen = 0;
        try (Statement st = con.createStatement()) {
            for (int i = 0; i < passes.size(); i += b) {
                StringBuilder sb = new StringBuilder("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ");
                for (int j = i; j < Math.min(i + b, passes.size()); j++) {
                    String[] p = passes.get(j);
                    if (j > i) sb.append(',');
                    sb.append("('").append(p[0]).append("','").append(p[1]).append("','").append(p[2]).append("',")
                      .append(Integer.parseInt(p[3])).append(',').append(Double.parseDouble(p[4])).append(')');
                }
                maxLen = Math.max(maxLen, sb.length());
                st.executeUpdate(sb.toString());
            }
            con.commit();
        } catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
        finally { con.setAutoCommit(true); }
        return maxLen;
    }

    static void loadData(Connection con, Path csv) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("LOAD DATA LOCAL INFILE '" + csv.toAbsolutePath() + "' INTO TABLE bus_pass FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' (pass_id, reg_no, name, route_no, amount)");
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = DbConfig.connect()) {
            System.out.println("rows,PassLoader_batch_txn_ms,multirow_insert_b1000_txn_ms,multirow_max_sql_chars,LOAD_DATA_ms,load_data_rows");
            for (int n : new int[]{1000, 10000, 100000}) {
                List<String[]> data = Gen.passes(n);
                clear(con);
                long t = System.nanoTime(); PassLoader.loadAll(con, data); double a = (System.nanoTime() - t) / 1e6;
                clear(con);
                t = System.nanoTime(); int len = multiRow(con, data, 1000); double b = (System.nanoTime() - t) / 1e6;
                if (count(con) != n) System.out.println("!! multi-row count mismatch");
                Path csv = Files.createTempFile("passes", ".csv");
                try (BufferedWriter w = Files.newBufferedWriter(csv)) { for (String[] p : data) { w.write(String.join(",", p)); w.write("\n"); } }
                clear(con);
                String loadMs; int rows;
                try { t = System.nanoTime(); loadData(con, csv); loadMs = String.format("%.0f", (System.nanoTime() - t) / 1e6); rows = count(con); }
                catch (SQLException e) { loadMs = "FAILED(" + e.getMessage().split("\n")[0] + ")"; rows = -1; }
                Files.delete(csv);
                System.out.printf("%d,%.0f,%.0f,%d,%s,%d%n", n, a, b, len, loadMs, rows);
            }
            clear(con);
        }
    }
}
