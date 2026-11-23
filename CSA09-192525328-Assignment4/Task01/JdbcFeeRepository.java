import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation: parameterised statements (no string concatenation) and connections closed by try-with-resources.
 *  Table: CREATE TABLE fees (id INT AUTO_INCREMENT PRIMARY KEY, reg_no CHAR(9) NOT NULL, amount DECIMAL(10,2) NOT NULL, paid_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP); */
public class JdbcFeeRepository implements FeeRepository {
    private final String url, user, password;
    public JdbcFeeRepository(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }

    @Override public void save(String regNo, BigDecimal amount) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement("INSERT INTO fees (reg_no, amount) VALUES (?, ?)")) {
            ps.setString(1, regNo); ps.setBigDecimal(2, amount); ps.executeUpdate();
        }
    }
    @Override public List<Payment> findByRegNo(String regNo) throws SQLException {
        List<Payment> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, user, password);
             PreparedStatement ps = c.prepareStatement("SELECT reg_no, amount, paid_at FROM fees WHERE reg_no = ? ORDER BY paid_at")) {
            ps.setString(1, regNo);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new Payment(rs.getString(1), rs.getBigDecimal(2), rs.getTimestamp(3).toLocalDateTime())); }
        }
        return out;
    }
}
