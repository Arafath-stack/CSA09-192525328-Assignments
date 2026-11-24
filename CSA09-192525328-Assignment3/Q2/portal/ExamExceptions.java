import java.time.LocalDateTime;

/** Base class: checked, because every one of these is an expected, recoverable business or I/O failure. */
class ExamException extends Exception {
    private static final long serialVersionUID = 1L;
    ExamException(String message) { super(message); }
    ExamException(String message, Throwable cause) { super(message, cause); }
}
class ExamClosedException extends ExamException {
    private static final long serialVersionUID = 1L;
    ExamClosedException(String course, LocalDateTime closedAt) { super(closedAt == null ? "No exam window exists for " + course : "The exam window for " + course + " closed at " + closedAt.toLocalTime()); }
}
class NotRegisteredException extends ExamException {
    private static final long serialVersionUID = 1L;
    NotRegisteredException(String student, String course) { super("Student " + student + " is not registered for " + course); }
}
class FileTooLargeException extends ExamException {
    private static final long serialVersionUID = 1L;
    FileTooLargeException(long sizeKb, long limitKb) { super("Answer file is " + sizeKb + " KB; the limit is " + limitKb + " KB"); }
}
/** Storage-layer failure: hides the IOException from the upper layers but keeps it as the cause. */
class SubmissionStorageException extends ExamException {
    private static final long serialVersionUID = 1L;
    SubmissionStorageException(String message, Throwable cause) { super(message, cause); }
}
