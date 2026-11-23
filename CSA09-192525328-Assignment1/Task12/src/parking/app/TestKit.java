package parking.app;

import java.util.function.Supplier;

/** Tiny test harness that prints an "expected vs actual" table. */
final class TestKit {
    private int pass, fail;
    void header() { System.out.printf("%-5s %-52s %-24s %-24s %s%n", "ID", "Test case", "Expected", "Actual", "Result"); }
    void check(String id, String desc, Object expected, Supplier<Object> actual) {
        String a; try { a = String.valueOf(actual.get()); } catch (Exception e) { a = "EXC " + e.getClass().getSimpleName(); }
        row(id, desc, String.valueOf(expected), a);
    }
    void throwsEx(String id, String desc, Class<? extends Exception> ex, Runnable r) {
        String a = "no exception";
        try { r.run(); } catch (Exception e) { a = e.getClass().getSimpleName(); }
        row(id, desc, ex.getSimpleName(), a);
    }
    private void row(String id, String d, String e, String a) {
        boolean ok = e.equals(a); if (ok) pass++; else fail++;
        System.out.printf("%-5s %-52s %-24s %-24s %s%n", id, d, e, a, ok ? "PASS" : "FAIL");
    }
    void summary() { System.out.println("\nTotal: " + (pass + fail) + "   Passed: " + pass + "   Failed: " + fail); }
}
