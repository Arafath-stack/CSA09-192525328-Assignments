package app.dao;

import app.model.Event;
import app.model.Registration;
import java.util.List;

/** Data-access layer. eventId 0 means "all events". Implementations: JdbcRegistrationDao (MySQL) and InMemoryRegistrationDao (tests/demo). */
public interface RegistrationDao {
    List<Event> events() throws DaoException;
    boolean exists(String regNo, int eventId) throws DaoException;
    void insert(String regNo, String name, String email, int eventId) throws DaoException, DuplicateRegistrationException;
    long count(int eventId) throws DaoException;
    List<Registration> page(int eventId, int limit, int offset) throws DaoException;
    List<Registration> all(int eventId) throws DaoException;
}
