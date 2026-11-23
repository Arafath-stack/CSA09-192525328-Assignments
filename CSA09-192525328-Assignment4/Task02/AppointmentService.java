import java.util.*;

/** Booking rules and the in-memory directory (Map: department -> doctors). No GUI code. */
public class AppointmentService {
    public static final List<String> SLOTS = List.of("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30");
    public record Result(boolean ok, String message) { }
    private final Map<String, List<Doctor>> byDepartment = new TreeMap<>();
    private final Set<String> booked = new HashSet<>();            // "doctor|slot"

    public AppointmentService(DoctorDirectory dir) { for (Doctor d : dir.loadAll()) byDepartment.computeIfAbsent(d.department(), k -> new ArrayList<>()).add(d); }
    public Set<String> departments() { return byDepartment.keySet(); }
    public List<Doctor> doctorsIn(String dept) { return byDepartment.getOrDefault(dept, List.of()); }
    public boolean isFree(Doctor d, String slot) { return !booked.contains(d.name() + "|" + slot); }

    public Result book(String patientId, String dept, Doctor doctor, String slot) {
        if (patientId == null || !patientId.trim().matches("\\d{6}")) return new Result(false, "Patient ID must be 6 digits.");
        if (dept == null) return new Result(false, "Select a department.");
        if (doctor == null) return new Result(false, "Select a doctor.");
        if (!doctor.department().equals(dept)) return new Result(false, "That doctor does not belong to " + dept + ".");
        if (slot == null) return new Result(false, "Select a time slot.");
        if (!booked.add(doctor.name() + "|" + slot)) return new Result(false, doctor.name() + " is already booked at " + slot + ".");
        return new Result(true, "Booked: patient " + patientId.trim() + " with " + doctor.name() + " (" + dept + ") at " + slot + ".");
    }
}
