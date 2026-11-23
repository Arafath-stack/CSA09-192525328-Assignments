import java.sql.*;
import java.util.*;

/** B3.1 - B3.3 : Statement vs PreparedStatement vs batch, auto-commit vs transaction, failure test. */
public class B3Experiments {

    static void clear(Connection con) throws SQLException {
        con.setAutoCommit(true);
        try (Statement st = con.createStatement()) { st.executeUpdate("TRUNCATE TABLE bus_pass"); }
    }
    static int count(Connection con) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) { rs.next(); return rs.getInt(1); }
    }

    // ---------- (a) Statement : SQL string built for every row ----------
    static void viaStatement(Connection con, List<String[]> ps) throws SQLException {
        try (Statement st = con.createStatement()) {
            for (String[] p : ps)
                st.executeUpdate("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ('"
                    + p[0] + "','" + p[1] + "','" + p[2] + "'," + p[3] + "," + p[4] + ")");
        }
    }
    // ---------- (b) PreparedStatement : one statement per row ----------
    static void viaPrepared(Connection con, List<String[]> passes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?,?,?,?,?)")) {
            for (String[] p : passes) { bind(ps, p); ps.executeUpdate(); }
        }
    }
    // ---------- (c) PreparedStatement + addBatch/executeBatch (batches of 1000), current commit mode ----------
    static void viaBatch(Connection con, List<String[]> passes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?,?,?,?,?)")) {
            int n = 0;
            for (String[] p : passes) { bind(ps, p); ps.addBatch(); if (++n % 1000 == 0) ps.executeBatch(); }
            ps.executeBatch();
        }
    }
    static void bind(PreparedStatement ps, String[] p) throws SQLException {
        ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]);
        ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4]));
    }
    // ---------- batch with a single transaction (same thing, via the completed PassLoader) ----------

    static void b31(Connection con) throws SQLException {
        System.out.println("=== B3.1 Correctness on small data (10 records, each approach) ===");
        List<String[]> ten = Gen.passes(10);
        String[] names = {"Statement", "PreparedStatement", "PreparedStatement + batch", "PassLoader.loadAll (transaction)"};
        for (int a = 0; a < 4; a++) {
            clear(con);
            if (a == 0) viaStatement(con, ten); else if (a == 1) viaPrepared(con, ten); else if (a == 2) viaBatch(con, ten);
            else System.out.println("  PassLoader.loadAll returned " + PassLoader.loadAll(con, ten));
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM bus_pass ORDER BY pass_id")) {
                int rows = 0; String first = null, last = null;
                while (rs.next()) { rows++; String r = rs.getString(1) + "," + rs.getString(2) + "," + rs.getString(3) + "," + rs.getInt(4) + "," + rs.getBigDecimal(5);
                    if (first == null) first = r; last = r; }
                System.out.printf("  %-34s -> %d rows  first=[%s]  last=[%s]%n", names[a], rows, first, last);
                if (a == 0) {
                    ResultSetMetaData md = rs.getMetaData();
                    System.out.println("  ResultSetMetaData of SELECT * FROM bus_pass:");
                    for (int c = 1; c <= md.getColumnCount(); c++)
                        System.out.printf("     col %d: %-9s type=%-8s (java.sql.Types=%d) java=%s size=%d nullable=%s%n", c, md.getColumnName(c),
                            md.getColumnTypeName(c), md.getColumnType(c), md.getColumnClassName(c), md.getPrecision(c),
                            md.isNullable(c) == ResultSetMetaData.columnNoNulls ? "NO" : "YES");
                }
            }
        }
        System.out.println();
    }

    static void b32(Connection con, int[] sizes, boolean includeSlowStatement) throws SQLException {
        System.out.println("=== B3.2 Increase the data size (auto-commit ON for all three; times in ms) ===");
        System.out.println("rows,Statement_ms,PreparedStatement_ms,PreparedStatement_batch_ms");
        for (int n : sizes) {
            List<String[]> data = Gen.passes(n);
            double[] t = new double[3];
            for (int a = 0; a < 3; a++) {
                clear(con); con.setAutoCommit(true);
                long s = System.nanoTime();
                if (a == 0) viaStatement(con, data); else if (a == 1) viaPrepared(con, data); else viaBatch(con, data);
                t[a] = (System.nanoTime() - s) / 1e6;
                if (count(con) != n) System.out.println("  !! wrong row count for approach " + a);
            }
            System.out.printf("%d,%.0f,%.0f,%.0f%n", n, t[0], t[1], t[2]);
        }
        System.out.println();
    }

    static void b33(Connection con, int[] sizes) throws SQLException {
        System.out.println("=== B3.3 Batch insert: auto-commit ON vs ONE transaction (times in ms) ===");
        System.out.println("rows,batch_autocommit_ON_ms,batch_single_transaction_ms");
        for (int n : sizes) {
            List<String[]> data = Gen.passes(n);
            clear(con); con.setAutoCommit(true);
            long s = System.nanoTime(); viaBatch(con, data); double ta = (System.nanoTime() - s) / 1e6;
            clear(con);
            s = System.nanoTime(); PassLoader.loadAll(con, data); double tt = (System.nanoTime() - s) / 1e6;
            System.out.printf("%d,%.0f,%.0f%n", n, ta, tt);
        }
        System.out.println();

        System.out.println("--- Forced failure half-way (10,000 rows; row 5,001 has amount 'ABC') ---");
        List<String[]> bad = Gen.passes(10000);
        bad.get(5000)[4] = "ABC";
        clear(con); con.setAutoCommit(true);
        try { viaBatch(con, bad); } catch (RuntimeException e) { System.out.println("  [auto-commit ON ] failed with: " + e); }
        System.out.println("  [auto-commit ON ] rows in table after failure = " + count(con) + "   (partial data stays: table is inconsistent)");
        clear(con);
        try { PassLoader.loadAll(con, bad); } catch (RuntimeException e) { System.out.println("  [transaction    ] failed with: " + e); }
        System.out.println("  [transaction    ] rows in table after failure = " + count(con) + "   (rollback: all or nothing)");

        System.out.println("--- Failure caused by the DATABASE (duplicate primary key at row 7,001) ---");
        List<String[]> dup = Gen.passes(10000);
        dup.get(7000)[0] = "P000001";
        clear(con); con.setAutoCommit(true);
        try { viaBatch(con, dup); } catch (SQLException e) { System.out.println("  [auto-commit ON ] failed with: " + e.getClass().getSimpleName() + ": " + e.getMessage().split("\n")[0]); }
        System.out.println("  [auto-commit ON ] rows in table after failure = " + count(con));
        clear(con);
        try { PassLoader.loadAll(con, dup); } catch (SQLException e) { System.out.println("  [transaction    ] failed with: " + e.getClass().getSimpleName() + ": " + e.getMessage().split("\n")[0]); }
        System.out.println("  [transaction    ] rows in table after failure = " + count(con));
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = DbConfig.connect()) {
            DatabaseMetaData m = con.getMetaData();
            System.out.println("Database: " + m.getDatabaseProductName() + " " + m.getDatabaseProductVersion() + " | Driver: " + m.getDriverName() + " " + m.getDriverVersion() + "\n");
            String which = args.length > 0 ? args[0] : "all";
            if (which.equals("all") || which.equals("b31")) b31(con);
            if (which.equals("all") || which.equals("b32")) b32(con, new int[]{1000, 10000, 100000}, true);
            if (which.equals("all") || which.equals("b33")) b33(con, new int[]{1000, 10000, 100000});
            clear(con);
        }
    }
}
