import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Consumer;

/** Alternative concept: one listener with an if-else chain on the command string, versus one listener object per button / a map of actions. */
public class DispatchBench {
    static long sink;
    static final com.sun.management.ThreadMXBean TB = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long alloc() { return TB.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    public static void main(String[] args) throws Exception {
        PrintWriter csv = new PrintWriter("dispatch_times.csv"); csv.println("k,approach,rep,ns_per_dispatch");
        System.out.printf("%-8s %-38s %16s %24s%n", "k", "approach (worst case: the LAST button)", "ns / dispatch", "bytes for k listeners");
        for (int k : new int[]{3, 10, 100, 1000}) {
            String[] cmd = new String[k]; for (int i = 0; i < k; i++) cmd[i] = "Button" + i;                 // distinct String objects, like labels read from a resource file
            String lookup = new String(cmd[k - 1]);                                                         // a different String object with equal contents
            long[] counter = new long[1];
            // (1) one listener, if-else chain: up to k equals() calls
            Consumer<String> chain = c -> { for (int i = 0; i < k; i++) { if (c.equals(cmd[i])) { counter[0] += i; return; } } };
            // (2) one listener object per button: the event already points at its own listener, no search
            long a0 = alloc(); Runnable[] listeners = new Runnable[k]; for (int i = 0; i < k; i++) { final int id = i; listeners[i] = () -> counter[0] += id; } long perListener = alloc() - a0;
            // (3) map from command to action: one hash lookup
            Map<String, Runnable> map = new HashMap<>(); for (int i = 0; i < k; i++) { final int id = i; map.put(cmd[i], () -> counter[0] += id); }
            for (String ap : new String[]{"one listener (if-else chain)", "one listener object per button", "HashMap of actions"}) {
                int reps = 3_000_000 / Math.max(1, k / 10 + 1); double[] ns = new double[7];
                for (int rep = -3; rep < 7; rep++) {
                    long t0 = System.nanoTime();
                    for (int n = 0; n < reps; n++) { if (ap.startsWith("one listener (")) chain.accept(lookup); else if (ap.startsWith("one listener object")) listeners[k - 1].run(); else map.get(lookup).run(); }
                    double d = (double) (System.nanoTime() - t0) / reps; if (rep >= 0) { ns[rep] = d; csv.printf("%d,%s,%d,%.2f%n", k, ap, rep, d); }
                }
                System.out.printf("%-8d %-38s %16.2f %24s%n", k, ap, Arrays.stream(ns).average().orElse(0), ap.startsWith("one listener object") ? perListener + " (about " + perListener / k + " per listener)" : "1 listener object");
            }
            sink += counter[0];
        }
        csv.close(); System.out.println("(checksum " + sink + ")");
    }
}
