import java.nio.file.*;
import java.util.*;

/** B1.1 - test table: normal case, missing id, damaged lines, list by route, delimiter in name. */
public class PassStoreTest {
    static int pass = 0, fail = 0;
    static void check(String id, String what, boolean ok, String detail) {
        System.out.printf("%-4s %-34s %-5s %s%n", id, what, ok ? "PASS" : "FAIL", detail);
        if (ok) pass++; else fail++;
    }
    public static void main(String[] a) throws Exception {
        Path f = Files.createTempFile("pass_test", ".csv");
        BusPassStore s = new BusPassStore(f);
        s.addSafe("P1001", "192311001", "Arun", 12, 4500);
        s.addSafe("P1002", "192311002", "Meena", 7, 3800);
        s.addSafe("P1003", "192311003", "Kumar, Ravi", 12, 4200);      // name contains the delimiter
        // damaged lines appended by hand
        Files.writeString(f, "P1004,192311004,Bad,12\n", StandardOpenOption.APPEND);          // 4 fields
        Files.writeString(f, "P1005,192311005,Lakshmi,7,abc\n", StandardOpenOption.APPEND);   // non-numeric amount
        Files.writeString(f, "\n", StandardOpenOption.APPEND);                                  // empty line
        Files.writeString(f, "P1008,192311008,Farah,7,3900.0\n", StandardOpenOption.APPEND);   // valid pass AFTER the damaged lines
        Files.writeString(f, "P1006,19231", StandardOpenOption.APPEND);                         // power-cut: half line, no newline
        System.out.println("File content:\n" + Files.readString(f));
        System.out.println("ID   Test                               Result Details");
        String[] r;
        r = s.findStrict("P1001");
        check("T1", "find existing pass (normal)", r != null && r[2].equals("Arun"), Arrays.toString(r));
        r = s.findStrict("P9999");
        check("T2", "find pass that does not exist", r == null, "returned null");
        r = s.findStrict("P1004");
        check("T3", "damaged line: 4 fields", r == null, "skipped, null");
        r = s.findStrict("P1005");
        check("T4", "damaged line: amount 'abc'", r == null, "skipped, null (base PassStore.find would return it!)");
        r = s.findStrict("P1006");
        check("T5", "half-written last line (power cut)", r == null, "skipped, null");
        r = s.findStrict("P1008");
        check("T6", "damaged lines do not stop search", r != null && r[2].equals("Farah"), "P1008 lies AFTER 3 damaged lines and is still found: " + Arrays.toString(r));
        List<String[]> l = s.listByRoute(12);
        check("T7", "list route 12", l.size() == 2, l.size() + " passes: " + l.get(0)[0] + ", " + l.get(1)[0]);
        check("T8", "list route with no passes (99)", s.listByRoute(99).isEmpty(), "empty list");
        r = s.findStrict("P1003");
        check("T9", "name with comma round-trips", r != null && BusPassStore.decodeName(r[2]).equals("Kumar, Ravi"),
              "stored as " + (r == null ? "null" : r[2]) + " -> decoded " + (r == null ? "null" : BusPassStore.decodeName(r[2])));
        boolean threw = false;
        try { s.addSafe("P1007", "abc", "X", 3, 10); } catch (IllegalArgumentException e) { threw = true; }
        check("T10", "addSafe rejects invalid record", threw, "IllegalArgumentException");
        BusPassStore missing = new BusPassStore(Paths.get("no_such_file.csv"));
        check("T11", "missing file handled", missing.findStrict("P1") == null, "returned null, no crash");
        System.out.println("\nSummary: " + pass + " passed, " + fail + " failed");
        Files.delete(f);
    }
}
