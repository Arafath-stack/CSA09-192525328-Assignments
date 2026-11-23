package app.model;

import java.time.LocalDate;

/** An event students can register for. id 0 is used by the screens for "All events". */
public record Event(int id, String name, LocalDate date) {
    public static final Event ALL = new Event(0, "All events", null);
    @Override public String toString() { return name; }
}
