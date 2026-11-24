import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** STORAGE layer: talks to the disk; translates IOException into the portal's own exception. */
class Storage {
    private final long quotaKb;
    private final Map<String, Long> used = new HashMap<>();
    Storage(long quotaKb) { this.quotaKb = quotaKb; }
    void save(String student, String file, long sizeKb, boolean keepCause) throws SubmissionStorageException {
        try {
            writeToDisk(student, file, sizeKb);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + file + " for " + student, keepCause ? e : null);
        }
    }
    private void writeToDisk(String student, String file, long sizeKb) throws IOException {
        long total = used.getOrDefault(student, 0L) + sizeKb;
        if (total > quotaKb) throw new IOException("Disk quota exceeded: " + total + " KB of " + quotaKb + " KB");
        used.put(student, total);
    }
}

/** SERVICE layer: the exam rules. Does not handle the checked exceptions: it declares them and lets them travel up. */
class ExamService {
    static final long LIMIT_KB = 500;
    private final Map<String, LocalDateTime> windowEnds = new HashMap<>();
    private final Set<String> registered = new HashSet<>();
    private final Storage storage;
    boolean keepCause = true;
    ExamService(Storage storage) {
        this.storage = storage;
        windowEnds.put("CSA09", LocalDateTime.of(2026, 11, 20, 12, 0));
        registered.add("S1/CSA09"); registered.add("S2/CSA09");
    }
    void submit(String student, String course, String file, long sizeKb, LocalDateTime now) throws ExamException {
        if (student == null || student.isBlank() || file == null || sizeKb < 0) throw new IllegalArgumentException("student, file and a non-negative size are required");   // unchecked: caller bug
        LocalDateTime end = windowEnds.get(course);
        if (end == null || now.isAfter(end)) throw new ExamClosedException(course, end);
        if (!registered.contains(student + "/" + course)) throw new NotRegisteredException(student, course);
        if (sizeKb > LIMIT_KB) throw new FileTooLargeException(sizeKb, LIMIT_KB);
        storage.save(student, file, sizeKb, keepCause);
    }
}

/** CONTROLLER layer: the only place that handles the exceptions and decides what the student sees. */
class Controller {
    private final ExamService service;
    String lastLog = "";
    Controller(ExamService service) { this.service = service; }
    String handle(String student, String course, String file, long sizeKb, LocalDateTime now) {
        try {
            service.submit(student, course, file, sizeKb, now);
            return "Submission received: " + file;
        } catch (ExamClosedException | NotRegisteredException | FileTooLargeException e) {   // business rules: tell the student exactly why
            lastLog = e.getClass().getSimpleName();
            return "Not accepted: " + e.getMessage();
        } catch (SubmissionStorageException e) {                                               // technical failure: friendly message, full detail in the log
            lastLog = e.getClass().getSimpleName() + " <- cause: " + e.getCause();
            return "We could not save your answer. Please try again in a few minutes.";
        } catch (ExamException e) {
            lastLog = "ExamException";
            return "Submission failed.";
        } catch (IllegalArgumentException e) {
            lastLog = "IllegalArgumentException";
            return "Invalid request.";
        }
    }
}
