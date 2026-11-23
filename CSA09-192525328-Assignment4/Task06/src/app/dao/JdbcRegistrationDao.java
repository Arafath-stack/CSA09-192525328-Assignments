package app.dao;

import app.model.Event;
import app.model.Registration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation (JDBC). Every statement is a PreparedStatement (no string concatenation of user data), every
 * Connection/Statement/ResultSet is closed by try-with-resources. Needs the MySQL Connector/J jar on the class path.
 * Duplicate protection is enforced by the database itself (UNIQUE KEY uq_student_event) in addition to the service's check.
 */
public class JdbcRegistrationDao implements RegistrationDao {
    private static final String ROW = "SELECT r.id, r.reg_no, r.student_name, r.email, r.event_id, e.name, r.registered_at FROM registrations r JOIN events e ON e.id = r.event_id";
    private final String url, user, password;
    public JdbcRegistrationDao(String url, String user, String password) { this.url = url; this.user = user; this.password = password; }
    private Connection open() throws SQLException { return DriverManager.getConnection(url, user, password); }

    @Override public List<Event> events() throws DaoException {
        List<Event> out = new ArrayList<>();
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT id, name, event_date FROM events ORDER BY event_date, name"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Event(rs.getInt(1), rs.getString(2), rs.getDate(3).toLocalDate()));
        } catch (SQLException e) { throw new DaoException("Could not read the events: " + e.getMessage(), e); }
        return out;
    }
    @Override public boolean exists(String regNo, int eventId) throws DaoException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM registrations WHERE reg_no = ? AND event_id = ?")) {
            ps.setString(1, regNo); ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DaoException("Could not check for a duplicate: " + e.getMessage(), e); }
    }
    @Override public void insert(String regNo, String name, String email, int eventId) throws DaoException, DuplicateRegistrationException {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("INSERT INTO registrations (reg_no, student_name, email, event_id) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, regNo); ps.setString(2, name); ps.setString(3, email); ps.setInt(4, eventId); ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) { throw new DuplicateRegistrationException(regNo, eventId);        // UNIQUE (reg_no, event_id) violated
        } catch (SQLException e) { if (e.getErrorCode() == 1062) throw new DuplicateRegistrationException(regNo, eventId); throw new DaoException("Could not save the registration: " + e.getMessage(), e); }
    }
    @Override public long count(int eventId) throws DaoException {
        String sql = "SELECT COUNT(*) FROM registrations" + (eventId > 0 ? " WHERE event_id = ?" : "");
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (eventId > 0) ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        } catch (SQLException e) { throw new DaoException("Could not count registrations: " + e.getMessage(), e); }
    }
    @Override public List<Registration> page(int eventId, int limit, int offset) throws DaoException {
        String sql = ROW + (eventId > 0 ? " WHERE r.event_id = ?" : "") + " ORDER BY r.id LIMIT ? OFFSET ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1; if (eventId > 0) ps.setInt(i++, eventId); ps.setInt(i++, limit); ps.setInt(i, offset);
            return read(ps);
        } catch (SQLException e) { throw new DaoException("Could not load registrations: " + e.getMessage(), e); }
    }
    @Override public List<Registration> all(int eventId) throws DaoException {
        String sql = ROW + (eventId > 0 ? " WHERE r.event_id = ?" : "") + " ORDER BY r.id";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) { if (eventId > 0) ps.setInt(1, eventId); return read(ps); }
        catch (SQLException e) { throw new DaoException("Could not load registrations: " + e.getMessage(), e); }
    }
    private static List<Registration> read(PreparedStatement ps) throws SQLException {
        List<Registration> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new Registration(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getString(6), rs.getTimestamp(7).toLocalDateTime())); }
        return out;
    }
}
