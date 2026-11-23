package app.service;

import app.dao.*;
import app.model.Event;
import app.model.Registration;
import java.io.IOException;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Service layer: all rules (validation, duplicate check, paging, export). No Swing and no SQL in here. */
public class RegistrationService {
    public static final int PAGE_SIZE = 20;
    public record Result(boolean ok, String message) { }
    public record Page(List<Registration> rows, long total, int pageIndex, int pages) { }

    private final RegistrationDao dao;
    public RegistrationService(RegistrationDao dao) { this.dao = dao; }
    public List<Event> events() throws DaoException { return dao.events(); }

    /** @return null if valid, otherwise a message for the user. Cheap: safe to call on the event-dispatch thread. */
    public String validate(String regNo, String name, String email, Event event) {
        if (regNo == null || !regNo.trim().matches("\\d{9}")) return "Register number must be exactly 9 digits.";
        if (name == null || !name.trim().matches("[A-Za-z][A-Za-z .'-]{1,59}")) return "Name must be 2 to 60 letters (spaces, dots, apostrophes and hyphens allowed).";
        if (email == null || !email.trim().matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) return "Enter a valid e-mail address.";
        if (event == null || event.id() <= 0) return "Select an event.";
        return null;
    }
    /** Blocking (database): call from a background thread. */
    public Result register(String regNo, String name, String email, Event event) {
        String error = validate(regNo, name, email, event);
        if (error != null) return new Result(false, error);
        try {
            if (dao.exists(regNo.trim(), event.id())) return new Result(false, "Student " + regNo.trim() + " is already registered for " + event.name() + ".");
            dao.insert(regNo.trim(), name.trim(), email.trim(), event.id());                      // the UNIQUE key also protects against two desks registering at once
            return new Result(true, "Registered " + name.trim() + " (" + regNo.trim() + ") for " + event.name() + ".");
        } catch (DuplicateRegistrationException e) { return new Result(false, e.getMessage());
        } catch (DaoException e) { return new Result(false, e.getMessage()); }
    }
    public Page page(int eventId, int pageIndex) throws DaoException {
        long total = dao.count(eventId); int pages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int idx = Math.max(0, Math.min(pageIndex, pages - 1));
        return new Page(dao.page(eventId, PAGE_SIZE, idx * PAGE_SIZE), total, idx, pages);
    }
    public List<Registration> all(int eventId) throws DaoException { return dao.all(eventId); }
    public int exportCsv(int eventId, Writer out) throws DaoException, IOException {
        List<Registration> rows = dao.all(eventId); DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        out.write("Register No,Name,Email,Event,Registered At\n");
        for (Registration r : rows) out.write(csv(r.regNo()) + "," + csv(r.name()) + "," + csv(r.email()) + "," + csv(r.eventName()) + "," + r.registeredAt().format(f) + "\n");
        return rows.size();
    }
    private static String csv(String s) { return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s; }
}
