import java.time.LocalDateTime;

/** Student B: exception chaining - getCause(), the chained stack trace, and what is lost without the cause. */
public class ChainDemo {
    public static void main(String[] args) {
        LocalDateTime t = LocalDateTime.of(2026, 11, 20, 10, 0);
        for (boolean keep : new boolean[]{true, false}) {
            Storage st = new Storage(500); ExamService svc = new ExamService(st); svc.keepCause = keep;
            System.out.println("================ cause " + (keep ? "KEPT" : "DROPPED (re-thrown without the cause)") + " ================");
            try {
                svc.submit("S1", "CSA09", "a1.pdf", 300, t);
                svc.submit("S1", "CSA09", "a2.pdf", 300, t);
            } catch (ExamException e) {
                System.out.println("getMessage()   = " + e.getMessage());
                System.out.println("getCause()     = " + e.getCause());
                Throwable root = e; int depth = 0; while (root.getCause() != null) { root = root.getCause(); depth++; }
                System.out.println("chain depth    = " + depth + ", root cause = " + root.getClass().getSimpleName());
                System.out.println("--- e.printStackTrace() ---");
                e.printStackTrace(System.out);
            }
        }
    }
}
