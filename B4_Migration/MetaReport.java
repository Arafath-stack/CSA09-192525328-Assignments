import java.sql.*;

/** B4.2 - inspect the database with DatabaseMetaData. */
public class MetaReport {
    public static void main(String[] args) throws Exception {
        try (Connection con = DbConfig.connect()) {
            DatabaseMetaData m = con.getMetaData();
            System.out.println("Database product   : " + m.getDatabaseProductName() + " " + m.getDatabaseProductVersion());
            System.out.println("Driver             : " + m.getDriverName() + " " + m.getDriverVersion() + " (JDBC " + m.getJDBCMajorVersion() + "." + m.getJDBCMinorVersion() + ")");
            System.out.println("URL / user         : " + m.getURL() + " / " + m.getUserName());
            System.out.println("Supports transactions        : " + m.supportsTransactions());
            System.out.println("Supports batch updates       : " + m.supportsBatchUpdates());
            System.out.println("Default isolation level      : " + isolation(m.getDefaultTransactionIsolation()));
            System.out.println("Max connections (0=unknown)  : " + m.getMaxConnections());
            System.out.println("Tables in catalog '" + con.getCatalog() + "':");
            try (ResultSet rs = m.getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String t = rs.getString("TABLE_NAME");
                    System.out.println("  - " + t);
                    try (ResultSet c = m.getColumns(con.getCatalog(), null, t, "%")) {
                        while (c.next()) System.out.println("      " + c.getString("COLUMN_NAME") + " " + c.getString("TYPE_NAME") + "(" + c.getInt("COLUMN_SIZE") + ")");
                    }
                    try (ResultSet k = m.getPrimaryKeys(con.getCatalog(), null, t)) {
                        while (k.next()) System.out.println("      PRIMARY KEY: " + k.getString("COLUMN_NAME"));
                    }
                }
            }
        }
    }
    static String isolation(int i) {
        switch (i) { case Connection.TRANSACTION_READ_COMMITTED: return "READ_COMMITTED"; case Connection.TRANSACTION_REPEATABLE_READ: return "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE: return "SERIALIZABLE"; case Connection.TRANSACTION_READ_UNCOMMITTED: return "READ_UNCOMMITTED"; default: return "NONE"; }
    }
}
