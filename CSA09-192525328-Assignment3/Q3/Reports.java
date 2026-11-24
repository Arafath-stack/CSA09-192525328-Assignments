import java.io.*;
import java.util.logging.*;

/** A reader/writer that counts itself, so leaks can be measured. */
class TrackedReader extends BufferedReader {
    static int open = 0;
    TrackedReader(String path) throws FileNotFoundException { super(new FileReader(path)); open++; }
    @Override public void close() throws IOException { super.close(); open--; }
}
class TrackedWriter extends PrintWriter {
    static int open = 0;
    TrackedWriter(String path) throws IOException { super(new BufferedWriter(new FileWriter(path))); open++; }
    @Override public void close() { super.close(); open--; }
}

public class Reports {
    static final Logger LOG = Logger.getLogger("reports");
    static {
        LOG.setUseParentHandlers(false);
        StreamHandler h = new StreamHandler(System.out, new SimpleFormatter() {
            @Override public String format(LogRecord r) { return "   [" + r.getLevel() + "] " + r.getMessage() + System.lineSeparator(); }
        }) { @Override public synchronized void publish(LogRecord r) { super.publish(r); flush(); } };
        LOG.addHandler(h);
    }
    static int fds() { return new File("/proc/self/fd").list().length; }
    static long parse(String line) {                                   // item,qty,price -> qty * price
        String[] p = line.split(",");
        return Long.parseLong(p[1].trim()) * Long.parseLong(p[2].trim());
    }

    /** Version 1a: manual close() written the way the bug appears: close() only on the normal path. */
    static void manualBuggy(String dir, int stores, String out) throws IOException {
        TrackedWriter w = new TrackedWriter(out);
        long grand = 0;
        for (int i = 1; i <= stores; i++) {
            TrackedReader r = new TrackedReader(dir + "/store" + i + ".csv");
            r.readLine();                                               // header
            long total = 0; String line;
            while ((line = r.readLine()) != null) total += parse(line);   // NumberFormatException here leaks r and w
            w.println("store" + i + ".csv," + total);
            grand += total;
            r.close();                                                  // never reached after an exception
        }
        w.println("TOTAL," + grand);
        w.close();                                                      // never reached: the buffered summary is lost
    }

    /** Version 1b: manual, correct: try-catch-finally with close() in finally. */
    static int manualFinally(String dir, int stores, String out) throws IOException {
        TrackedWriter w = null;
        int failed = 0;
        try {
            w = new TrackedWriter(out);
            long grand = 0;
            for (int i = 1; i <= stores; i++) {
                String f = dir + "/store" + i + ".csv";
                TrackedReader r = null;
                int lineNo = 0;
                try {
                    r = new TrackedReader(f);
                    r.readLine(); lineNo++;
                    long total = 0; String line;
                    while ((line = r.readLine()) != null) { lineNo++; total += parse(line); }
                    w.println("store" + i + ".csv," + total);
                    grand += total;
                } catch (IOException | NumberFormatException e) {
                    failed++;
                    LOG.severe("file=" + f + " line=" + lineNo + " cause=" + e);       // which file caused the failure
                    w.println("store" + i + ".csv,FAILED");
                } finally {
                    if (r != null) {
                        try { r.close(); } catch (IOException e) { LOG.warning("close failed for " + f + ": " + e); }
                    }
                }
            }
            w.println("TOTAL," + grand);
        } finally {
            if (w != null) w.close();
        }
        return failed;
    }

    /** Version 2: try-with-resources. */
    static int withResources(String dir, int stores, String out) throws IOException {
        int failed = 0;
        try (TrackedWriter w = new TrackedWriter(out)) {
            long grand = 0;
            for (int i = 1; i <= stores; i++) {
                String f = dir + "/store" + i + ".csv";
                int lineNo = 0;
                try (TrackedReader r = new TrackedReader(f)) {
                    r.readLine(); lineNo++;
                    long total = 0; String line;
                    while ((line = r.readLine()) != null) { lineNo++; total += parse(line); }
                    w.println("store" + i + ".csv," + total);
                    grand += total;
                } catch (IOException | NumberFormatException e) {
                    failed++;
                    LOG.severe("file=" + f + " line=" + lineNo + " cause=" + e);
                    w.println("store" + i + ".csv,FAILED");
                }
            }
            w.println("TOTAL," + grand);
        }
        return failed;
    }

    static String slurp(String path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(path))) { String l; while ((l = r.readLine()) != null) sb.append(l).append(" | "); }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        int base = fds();
        System.out.println("--- Good data: the two correct versions must give the same report ---");
        manualFinally("data_ok", 5, "sum_manual_ok.txt");
        withResources("data_ok", 5, "sum_twr_ok.txt");
        String a = slurp("sum_manual_ok.txt"), b = slurp("sum_twr_ok.txt");
        System.out.println("manual finally : " + a);
        System.out.println("try-with-res   : " + b);
        System.out.println("identical: " + a.equals(b) + ";  files open afterwards: manual " + (TrackedReader.open + TrackedWriter.open) + ", real descriptors above start: " + (fds() - base));

        System.out.println("\n--- Failing data (store3.csv has 'abc' as a quantity on line 4) ---");
        System.out.println("1a) manual close(), naive version:");
        try { manualBuggy("data_bad", 5, "sum_buggy_bad.txt"); } catch (Exception e) { System.out.println("   exception: " + e); }
        System.out.println("   handles still open: readers " + TrackedReader.open + ", writers " + TrackedWriter.open + "   (real descriptors above start: " + (fds() - base) + ")");
        System.out.println("   summary file content: [" + slurp("sum_buggy_bad.txt") + "]   <- empty: the buffered lines were never flushed");
        int leakedReaders = TrackedReader.open, leakedWriters = TrackedWriter.open;
        TrackedReader.open = 0; TrackedWriter.open = 0;

        System.out.println("1b) manual try-catch-finally:");
        manualFinally("data_bad", 5, "sum_manual_bad.txt");
        System.out.println("   handles still open: " + (TrackedReader.open + TrackedWriter.open) + "   summary: " + slurp("sum_manual_bad.txt"));
        System.out.println("2) try-with-resources:");
        withResources("data_bad", 5, "sum_twr_bad.txt");
        System.out.println("   handles still open: " + (TrackedReader.open + TrackedWriter.open) + "   summary: " + slurp("sum_twr_bad.txt"));
        System.out.println("same report for the failing input: " + slurp("sum_manual_bad.txt").equals(slurp("sum_twr_bad.txt")));
        System.out.println("\nFiles left open for the same failing input: naive manual = " + (leakedReaders + leakedWriters) + " (1 reader + 1 writer), manual finally = 0, try-with-resources = 0");
    }
}
