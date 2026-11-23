package app.dao;

import app.model.Event;
import app.model.Registration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Same contract as the JDBC DAO, held in memory: used for the headless tests, the demo and the benchmark. */
public class InMemoryRegistrationDao implements RegistrationDao {
    private final List<Event> events = List.of(new Event(1, "Inaugural Dance Night", LocalDate.of(2026, 12, 5)), new Event(2, "Battle of Bands", LocalDate.of(2026, 12, 6)),
            new Event(3, "Drama Fest", LocalDate.of(2026, 12, 7)), new Event(4, "Quiz Marathon", LocalDate.of(2026, 12, 8)));
    private final List<Registration> rows = new ArrayList<>(); private final Set<String> keys = new HashSet<>(); private long nextId = 1;
    public long simulatedLatencyMs;                                       // optional delay per call, to imitate a remote database

    /** Deterministic sample data: n registrations spread over the 4 events. */
    public InMemoryRegistrationDao seed(int n) {
        Random r = new Random(7); String[] first = {"Arun", "Divya", "Karthik", "Meena", "Ravi", "Sneha", "Vikram", "Priya", "Nisha", "Imran"};
        for (int i = 0; i < n; i++) { int ev = 1 + r.nextInt(4); String reg = String.format("19%07d", 2300000 + i / 2 + (i % 2 == 0 ? 0 : 1000000)); if (!keys.add(reg + "|" + ev)) { keys.add(reg + "|" + ev); }
            Event e = events.get(ev - 1); rows.add(new Registration(nextId++, reg, first[i % first.length] + " " + (char) ('A' + i % 26), first[i % first.length].toLowerCase() + i + "@college.edu", ev, e.name(), LocalDateTime.of(2026, 11, 1, 9, 0).plusMinutes(i))); }
        return this;
    }
    private void delay() { if (simulatedLatencyMs > 0) try { Thread.sleep(simulatedLatencyMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
    @Override public synchronized List<Event> events() { delay(); return events; }
    @Override public synchronized boolean exists(String regNo, int eventId) { delay(); return keys.contains(regNo + "|" + eventId); }
    @Override public synchronized void insert(String regNo, String name, String email, int eventId) throws DuplicateRegistrationException {
        delay(); if (!keys.add(regNo + "|" + eventId)) throw new DuplicateRegistrationException(regNo, eventId);
        rows.add(new Registration(nextId++, regNo, name, email, eventId, events.get(eventId - 1).name(), LocalDateTime.now().withNano(0)));
    }
    private List<Registration> filtered(int eventId) { if (eventId <= 0) return rows; List<Registration> l = new ArrayList<>(); for (Registration r : rows) if (r.eventId() == eventId) l.add(r); return l; }
    @Override public synchronized long count(int eventId) { delay(); return eventId <= 0 ? rows.size() : filtered(eventId).size(); }
    @Override public synchronized List<Registration> page(int eventId, int limit, int offset) { delay(); List<Registration> f = filtered(eventId); return new ArrayList<>(f.subList(Math.min(offset, f.size()), Math.min(f.size(), offset + limit))); }
    @Override public synchronized List<Registration> all(int eventId) { delay(); return new ArrayList<>(filtered(eventId)); }
}
