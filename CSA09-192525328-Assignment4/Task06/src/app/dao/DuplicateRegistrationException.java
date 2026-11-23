package app.dao;

public class DuplicateRegistrationException extends Exception {
    public DuplicateRegistrationException(String regNo, int eventId) { super("Student " + regNo + " is already registered for this event."); }
}
