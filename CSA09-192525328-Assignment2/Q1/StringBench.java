import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

/** Q1: String (+), StringBuilder, StringBuffer (and join / StringJoiner / format) for e-ticket text. */
public class StringBench {
    static final com.sun.management.ThreadMXBean TB = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long alloc() { return TB.getThreadAllocatedBytes(Thread.currentThread().getId()); }
    static long used() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    static void settle() { for (int i = 0; i < 3; i++) { System.gc(); try { Thread.sleep(30); } catch (InterruptedException e) { } } }
    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static double sd(double[] a) { double m = mean(a), s = 0; for (double x : a) s += (x - m) * (x - m); return Math.sqrt(s / a.length); }

    static String[] fields(int n) { String[] f = new String[n]; for (int i = 0; i < n; i++) f[i] = "F" + i + "=value" + (i % 97); return f; }

    static String viaPlus(String[] f) { String s = ""; for (String x : f) s = s + x + ";"; return s; }
    static String viaSB(String[] f) { StringBuilder b = new StringBuilder(); for (String x : f) b.append(x).append(';'); return b.toString(); }
    static String viaSBuf(String[] f) { StringBuffer b = new StringBuffer(); for (String x : f) b.append(x).append(';'); return b.toString(); }
    static String viaJoin(String[] f) { return String.join(";", f) + ";"; }
    static String viaJoiner(String[] f) { StringJoiner j = new StringJoiner(";", "", ";"); for (String x : f) j.add(x); return j.toString(); }
    static String viaFormat(String[] f) { return String.format("%s;".repeat(f.length), (Object[]) f); }

    public static void main(String[] args) throws Exception {
        LinkedHashMap<String, Function<String[], String>> methods = new LinkedHashMap<>();
        methods.put("String+", StringBench::viaPlus); methods.put("StringBuilder", StringBench::viaSB);
        methods.put("StringBuffer", StringBench::viaSBuf); methods.put("String.join", StringBench::viaJoin);
        methods.put("StringJoiner", StringBench::viaJoiner); methods.put("String.format", StringBench::viaFormat);
        String[] chk = fields(5); String ref = viaPlus(chk);
        for (var e : methods.entrySet()) if (!e.getValue().apply(chk).equals(ref)) throw new AssertionError(e.getKey() + " differs");
        System.out.println("All six approaches produce identical text (checked).");

        // workload name, tickets, fields per ticket
        Object[][] workloads = { {"W1 short ticket", 100_000, 8}, {"W2 bulk tickets", 10_000, 50}, {"W3 one huge ticket", 1, 20_000} };
        int REPS = 7, WARM = 2;
        try (PrintWriter csv = new PrintWriter("data/q1_raw_times.csv")) {
            csv.println("workload,method,rep,total_ms,us_per_ticket,alloc_bytes_per_ticket");
            System.out.printf("%-20s %-14s %12s %9s %14s %16s %14s%n", "Workload", "Method", "us/ticket", "(sd)", "alloc B/ticket", "retained B/ticket", "est. interm.");
            for (Object[] w : workloads) {
                String wn = (String) w[0]; int tickets = (Integer) w[1], nf = (Integer) w[2];
                String[] f = fields(nf);
                for (var e : methods.entrySet()) {
                    String mn = e.getKey(); var fn = e.getValue();
                    if (mn.equals("String.format") && nf > 1000) continue;     // format string of 20,000 specifiers: not a realistic use
                    String[] keep = new String[tickets];
                    for (int i = 0; i < WARM; i++) for (int t = 0; t < tickets; t++) keep[t] = fn.apply(f);
                    double[] us = new double[REPS], by = new double[REPS];
                    for (int r = 0; r < REPS; r++) {
                        long a0 = alloc(), t0 = System.nanoTime();
                        for (int t = 0; t < tickets; t++) keep[t] = fn.apply(f);
                        long t1 = System.nanoTime(), a1 = alloc();
                        double ms = (t1 - t0) / 1e6; us[r] = ms * 1000.0 / tickets; by[r] = (double) (a1 - a0) / tickets;
                        csv.printf("%s,%s,%d,%.3f,%.3f,%.0f%n", wn, mn, r, ms, us[r], by[r]);
                    }
                    keep = null; settle(); long u0 = used();
                    String[] kept = new String[tickets]; for (int t = 0; t < tickets; t++) kept[t] = fn.apply(f);
                    settle(); long u1 = used(); long retained = Math.max(0, (u1 - u0)) / tickets;
                    String est = mn.equals("String+") ? nf + "" : mn.equals("String.format") ? ">=" + nf : "1";
                    System.out.printf("%-20s %-14s %12.3f %9.3f %14.0f %16d %14s%n", wn, mn, mean(us), sd(us), mean(by), retained, est);
                    if (kept.length == 0) System.out.println();
                }
            }
        }
        threadTests();
    }

    static void threadTests() throws Exception {
        System.out.println("\n--- Thread-safety: 4 threads x 100,000 appends of \"x\" (expected length 400000), 10 trials");
        int T = 4, per = 100_000;
        try (PrintWriter csv = new PrintWriter("data/q1_thread_trials.csv")) {
            csv.println("class,trial,final_length,expected,thread_exceptions");
            for (String cls : new String[]{"StringBuilder", "StringBuffer"}) {
                int wrong = 0, exc = 0;
                for (int trial = 0; trial < 10; trial++) {
                    final Appendable[] holder = new Appendable[1];
                    final StringBuilder sb = new StringBuilder(); final StringBuffer sf = new StringBuffer();
                    final int[] ex = {0};
                    Thread[] th = new Thread[T];
                    for (int i = 0; i < T; i++) {
                        th[i] = new Thread(() -> {
                            try { for (int k = 0; k < per; k++) { if (cls.equals("StringBuilder")) sb.append('x'); else sf.append('x'); } }
                            catch (Throwable t) { synchronized (ex) { ex[0]++; } }
                        });
                        th[i].start();
                    }
                    for (Thread t : th) t.join();
                    int len = cls.equals("StringBuilder") ? sb.length() : sf.length();
                    if (len != T * per) wrong++; exc += ex[0];
                    csv.printf("%s,%d,%d,%d,%d%n", cls, trial, len, T * per, ex[0]);
                }
                System.out.printf("%-14s trials with wrong length: %d / 10   threads that crashed: %d%n", cls, wrong, exc);
            }
        }
        System.out.println("\n--- Entry integrity in a shared log: 4 threads x 20,000 entries of the form [T<id>-<n>]");
        int E = 20_000; Pattern ok = Pattern.compile("\\[T\\d+-\\d+\\]");
        try (PrintWriter csv = new PrintWriter("data/q1_log_integrity.csv")) {
            csv.println("variant,well_formed,expected");
            for (String variant : new String[]{"StringBuffer, 3 appends per entry", "StringBuffer, 1 append per entry", "StringBuilder in synchronized block"}) {
                StringBuffer log = new StringBuffer(); StringBuilder log2 = new StringBuilder(); Object lock = new Object();
                Thread[] th = new Thread[T];
                for (int i = 0; i < T; i++) {
                    final int id = i;
                    th[i] = new Thread(() -> {
                        for (int k = 0; k < E; k++) {
                            if (variant.startsWith("StringBuffer, 3")) log.append('[').append("T" + id + "-" + k).append(']');
                            else if (variant.startsWith("StringBuffer, 1")) log.append("[T" + id + "-" + k + "]");
                            else synchronized (lock) { log2.append('[').append("T" + id + "-" + k).append(']'); }
                        }
                    });
                    th[i].start();
                }
                for (Thread t : th) t.join();
                String text = variant.startsWith("StringBuilder") ? log2.toString() : log.toString();
                Matcher m = ok.matcher(text); int good = 0; while (m.find()) good++;
                System.out.printf("%-40s well-formed entries: %d / %d%n", variant, good, T * E);
                csv.printf("\"%s\",%d,%d%n", variant, good, T * E);
            }
        }
    }
}
