/** Student B: closing order and suppressed exceptions. */
public class SuppressedDemo {
    static class Res implements AutoCloseable {
        final String name; final boolean failOnClose;
        Res(String name, boolean failOnClose) { this.name = name; this.failOnClose = failOnClose; System.out.println("  open  " + name); }
        @Override public void close() { System.out.println("  close " + name); if (failOnClose) throw new IllegalStateException("close of " + name + " failed"); }
    }
    @SuppressWarnings("finally")
    static void manual() {
        Res r = new Res("manual", true);
        try { throw new RuntimeException("READ FAILED (the real problem)"); }
        finally { r.close(); }                                   // this exception REPLACES the one above
    }
    public static void main(String[] args) {
        System.out.println("1) closing order: resources are closed in REVERSE order of opening");
        try (Res a = new Res("A (opened first)", false); Res b = new Res("B", false); Res c = new Res("C (opened last)", false)) {
            System.out.println("  body runs");
        }
        System.out.println("\n2) body throws AND close() throws: the close exception is SUPPRESSED, the body exception is kept");
        try (Res a = new Res("A", true); Res b = new Res("B", true)) {
            throw new RuntimeException("READ FAILED (the real problem)");
        } catch (RuntimeException e) {
            System.out.println("  caught: " + e.getMessage());
            for (Throwable s : e.getSuppressed()) System.out.println("  suppressed: " + s.getMessage());
        }
        System.out.println("\n3) the same with manual finally: the original exception is LOST");
        try { manual(); } catch (RuntimeException e) { System.out.println("  caught: " + e.getMessage() + "   (suppressed count " + e.getSuppressed().length + ")"); }
    }
}
