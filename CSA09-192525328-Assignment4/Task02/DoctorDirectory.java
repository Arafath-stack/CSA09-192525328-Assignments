import java.util.List;

/** Source of doctors (a database in production). The desk reads it ONCE at start-up. */
public interface DoctorDirectory { List<Doctor> loadAll(); }
