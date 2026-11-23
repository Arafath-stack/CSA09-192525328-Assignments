import java.sql.*;
import java.util.regex.Pattern;

/** B2 - unsafe login vs PreparedStatement vs weaker defences (escaping, regex validation). */
public class LoginDemo {

    // ---- Task A5.2 : UNSAFE version (string concatenation) ----
    static boolean unsafeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        System.out.println("    SQL sent : " + sql);
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
        catch (SQLException e) { System.out.println("    SQL error: " + e.getMessage().split("\n")[0]); return false; }
    }

    // ---- SAFE version (PreparedStatement) ----
    static boolean safeLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=? AND pwd=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id); ps.setString(2, pwd);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    // ---- Weaker alternative 1: escape quotes by doubling them ----
    static String escapeQuotes(String s) { return s.replace("'", "''"); }
    static boolean escapedLogin(Connection con, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + escapeQuotes(id) + "' AND pwd='" + escapeQuotes(pwd) + "'";
        System.out.println("    SQL sent : " + sql);
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
        catch (SQLException e) { System.out.println("    SQL error: " + e.getMessage().split("\n")[0]); return false; }
    }

    // ---- Weaker alternative 2: validate with a regular expression (whitelist) ----
    static final Pattern SAFE = Pattern.compile("[A-Za-z0-9_@.]{1,30}");
    static boolean validatedLogin(Connection con, String id, String pwd) throws SQLException {
        if (!SAFE.matcher(id).matches() || !SAFE.matcher(pwd).matches()) { System.out.println("    rejected by regex validation"); return false; }
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
    }

    static void show(String label, boolean unsafe, boolean safe) {
        System.out.printf("  RESULT  unsafe=%-8s safe(PreparedStatement)=%-8s %s%n",
            unsafe ? "ACCEPTED" : "rejected", safe ? "ACCEPTED" : "rejected", label);
    }

    public static void main(String[] args) throws Exception {
        try (Connection con = DbConfig.connect()) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate("DELETE FROM users");
                st.executeUpdate("INSERT INTO users VALUES ('arun','Pass@123'),('admin','Str0ng#Secret')");
            }
            System.out.println("Users table holds: arun/Pass@123 and admin/Str0ng#Secret\n");

            String[][] cases = {
                {"1. Valid login (arun, correct password)",       "arun",           "Pass@123"},
                {"2. Wrong password",                              "arun",           "wrong"},
                {"3. Attack: comment out the password check",      "admin' -- ",     "anything"},
                {"4. Attack: always-true condition (OR '1'='1')",  "x' OR '1'='1",   "x' OR '1'='1"},
            };
            for (String[] c : cases) {
                System.out.println(c[0] + "   id=[" + c[1] + "]  pwd=[" + c[2] + "]");
                boolean u = unsafeLogin(con, c[1], c[2]);
                boolean s = safeLogin(con, c[1], c[2]);
                show("", u, s);
                System.out.println();
            }

            System.out.println("=== Alternative defences (B2 Alternative Concept) ===\n");
            System.out.println("5. Quote-doubling on the plain attack  id=[admin' -- ]");
            boolean e5 = escapedLogin(con, "admin' -- ", "x");
            System.out.println("  RESULT  escaped version = " + (e5 ? "ACCEPTED" : "rejected (this attack is blocked)") + "\n");

            String bid = "\\", bpwd = " OR 1=1 -- ";
            bpwd = "'" + bpwd;     // pwd = ' OR 1=1 --
            System.out.println("6. Quote-doubling BYPASSED using a backslash  id=[\\]  pwd=[" + bpwd + "]");
            boolean e6 = escapedLogin(con, bid, bpwd);
            boolean s6 = safeLogin(con, bid, bpwd);
            System.out.println("  RESULT  escaped version = " + (e6 ? "ACCEPTED (bypassed!)" : "rejected") + " | PreparedStatement = " + (s6 ? "ACCEPTED" : "rejected") + "\n");

            System.out.println("7. Regex validation blocks the attack  id=[admin' -- ]");
            boolean v7 = validatedLogin(con, "admin' -- ", "x");
            System.out.println("  RESULT  validated version = " + (v7 ? "ACCEPTED" : "rejected") + "\n");

            System.out.println("8. Regex validation rejects a LEGITIMATE password containing a quote/space: pwd=[It's my pass]");
            try (Statement st = con.createStatement()) { st.executeUpdate("INSERT INTO users VALUES ('neha','It''s my pass')"); }
            boolean v8 = validatedLogin(con, "neha", "It's my pass");
            boolean s8 = safeLogin(con, "neha", "It's my pass");
            System.out.println("  RESULT  validated version = " + (v8 ? "ACCEPTED" : "REJECTED (valid user locked out)") + " | PreparedStatement = " + (s8 ? "ACCEPTED" : "rejected") + "\n");

            // ---- cost per query ----
            System.out.println("=== Time cost per login check (5,000 repetitions, microseconds per call) ===");
            int N = 5000;
            for (int i = 0; i < 500; i++) { safeLogin(con, "arun", "Pass@123"); }
            long t = System.nanoTime();
            for (int i = 0; i < N; i++) SAFE.matcher("arun").matches();
            double reg = (System.nanoTime() - t) / 1e3 / N;
            t = System.nanoTime();
            for (int i = 0; i < N; i++) escapeQuotes("arun' OR 1=1");
            double esc = (System.nanoTime() - t) / 1e3 / N;
            t = System.nanoTime();
            for (int i = 0; i < N; i++) safeLogin(con, "arun", "Pass@123");
            double ps = (System.nanoTime() - t) / 1e3 / N;
            t = System.nanoTime();
            PrintStreamMute.on();
            for (int i = 0; i < N; i++) unsafeLogin(con, "arun", "Pass@123");
            PrintStreamMute.off();
            double un = (System.nanoTime() - t) / 1e3 / N;
            System.out.printf("  regex validation only         : %8.3f us%n", reg);
            System.out.printf("  quote escaping only           : %8.3f us%n", esc);
            System.out.printf("  whole unsafe query (concat)   : %8.1f us%n", un);
            System.out.printf("  whole PreparedStatement query : %8.1f us%n", ps);
        }
    }

    /** Small helper to silence the 'SQL sent' printing during timing. */
    static class PrintStreamMute {
        static final java.io.PrintStream real = System.out;
        static void on()  { System.setOut(new java.io.PrintStream(java.io.OutputStream.nullOutputStream())); }
        static void off() { System.setOut(real); }
    }
}
