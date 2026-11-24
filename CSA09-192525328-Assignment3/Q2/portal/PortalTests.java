import java.time.LocalDateTime;

/** Student B: one scenario per exception, with the observed behaviour. */
public class PortalTests {
    static final LocalDateTime OPEN = LocalDateTime.of(2026, 11, 20, 10, 0), LATE = LocalDateTime.of(2026, 11, 20, 12, 1);
    static int n = 0;
    static void row(String scenario, String expected, Controller c, String s, String course, String file, long kb, LocalDateTime t) {
        String got = c.handle(s, course, file, kb, t);
        System.out.printf("%d. %-34s student sees: %-62s | log: %-60s | %s%n", ++n, scenario, got, c.lastLog, got.startsWith(expected) ? "PASS" : "FAIL");
    }
    public static void main(String[] args) {
        Controller c = new Controller(new ExamService(new Storage(500)));
        row("valid submission", "Submission received", c, "S1", "CSA09", "a1.pdf", 120, OPEN);
        row("exam window closed", "Not accepted: The exam window", c, "S1", "CSA09", "a2.pdf", 100, LATE);
        row("student not registered", "Not accepted: Student S9", c, "S9", "CSA09", "a3.pdf", 100, OPEN);
        row("file larger than 500 KB", "Not accepted: Answer file", c, "S1", "CSA09", "big.pdf", 600, OPEN);
        row("storage quota exceeded (IOException)", "We could not save", c, "S1", "CSA09", "a4.pdf", 450, OPEN);
        row("blank student id (unchecked)", "Invalid request", c, " ", "CSA09", "a5.pdf", 10, OPEN);
        row("unknown course", "Not accepted: No exam window", c, "S1", "XYZ99", "a6.pdf", 10, OPEN);
    }
}
