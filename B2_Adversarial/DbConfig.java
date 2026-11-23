import java.sql.*;

/** Connection settings. Override with environment variables DB_URL, DB_USER, DB_PASS. */
public class DbConfig {
    static String env(String k, String d) { String v = System.getenv(k); return v == null || v.isEmpty() ? d : v; }
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
            env("DB_URL", "jdbc:mysql://localhost:3306/buspass_test"),
            env("DB_USER", "bususer"), env("DB_PASS", "buspass123"));
    }
}
