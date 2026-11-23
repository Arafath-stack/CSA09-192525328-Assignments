package app.model;

import java.time.LocalDateTime;

public record Registration(long id, String regNo, String name, String email, int eventId, String eventName, LocalDateTime registeredAt) { }
