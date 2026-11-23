import java.io.*;
import java.util.*;

/**
 * Q7: Diagnose, redesign and validate the Q4 pharmacy store.
 * V1 = Q4 deployment: HashMap + sort the keys for every code-order report.
 * V3 = TreeMap only (alternative baseline).
 * V2 = REDESIGN: LinkedHashMap for lookup/insertion-order + a sorted code index that is maintained incrementally
 *      (binary search + one shift per NEW medicine) and handed out as an unmodifiable view.
 */
public class MedicineStoreRedesign {
    record Medicine(String code, String name, int stock) { }
    static long comparisons, copied;                       // operation counters
    static final Comparator<String> COUNTING = (a, b) -> { comparisons++; return a.compareTo(b); };

    interface Store { void add(Medicine m); Medicine get(String code); void updateStock(String code, int stock); List<String> codeOrderReport(); }

    static class V1 implements Store {                      // original
        final Map<String, Medicine> map = new HashMap<>();
        public void add(Medicine m) { map.put(m.code(), m); }
        public Medicine get(String c) { return map.get(c); }
        public void updateStock(String c, int s) { Medicine m = map.get(c); map.put(c, new Medicine(c, m.name(), s)); }
        public List<String> codeOrderReport() { String[] k = map.keySet().toArray(new String[0]); copied += k.length; Arrays.sort(k, COUNTING); return Arrays.asList(k); }
    }
    static class V3 implements Store {                      // TreeMap only
        final TreeMap<String, Medicine> map = new TreeMap<>(COUNTING);
        public void add(Medicine m) { map.put(m.code(), m); }
        public Medicine get(String c) { return map.get(c); }
        public void updateStock(String c, int s) { Medicine m = map.get(c); map.put(c, new Medicine(c, m.name(), s)); }
        public List<String> codeOrderReport() { List<String> l = new ArrayList<>(map.keySet()); copied += l.size(); return l; }
    }
    static class V2 implements Store {                      // redesign
        final LinkedHashMap<String, Medicine> map = new LinkedHashMap<>();
        final ArrayList<String> sortedCodes = new ArrayList<>();
        private List<String> view = Collections.unmodifiableList(sortedCodes);
        V2() { }
        void bulkLoad(Collection<Medicine> all) { for (Medicine m : all) map.put(m.code(), m); sortedCodes.addAll(map.keySet()); sortedCodes.sort(COUNTING); }   // one sort at start-up
        public void add(Medicine m) {
            if (map.put(m.code(), m) == null) { int pos = Collections.binarySearch(sortedCodes, m.code(), COUNTING); if (pos < 0) pos = -pos - 1; sortedCodes.add(pos, m.code()); }
        }
        public Medicine get(String c) { return map.get(c); }
        public void updateStock(String c, int s) { Medicine m = map.get(c); map.put(c, new Medicine(c, m.name(), s)); }   // keys unchanged -> index untouched
        public List<String> codeOrderReport() { return view; }                                                           // O(1), no copy
    }

    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static long sink;

    public static void main(String[] args) throws Exception {
        try (PrintWriter csv = new PrintWriter("data/q7_raw.csv")) {
            csv.println("design,n,reports_per_write,writes,rep,total_ms,comparisons,elements_copied");
            System.out.printf("%-26s %8s %6s | %12s %14s %16s%n", "design", "n", "R", "total ms", "comparisons", "elements copied");
            int W = 10;
            for (int n : new int[]{1_000, 10_000, 100_000}) {
                Random rr = new Random(1);
                List<Integer> ids = new ArrayList<>(); for (int i = 0; i < n; i++) ids.add(i); Collections.shuffle(ids, rr);
                List<Medicine> base = new ArrayList<>(); for (int i = 0; i < n; i++) base.add(new Medicine(String.format("MED%07d", 2 * ids.get(i)), "d" + i, i % 400));
                List<Medicine> fresh = new ArrayList<>(); Random r2 = new Random(77); Set<Integer> used = new HashSet<>();
                while (fresh.size() < W) { int x = 2 * r2.nextInt(n) + 1; if (used.add(x)) fresh.add(new Medicine(String.format("MED%07d", x), "new", 1)); }
                for (int R : (n >= 100_000 ? new int[]{1, 10} : new int[]{1, 10, 100})) {
                    List<String> reference = null;
                    for (String design : new String[]{"V1 HashMap + sort per report", "V3 TreeMap only", "V2 redesign: cached index"}) {
                        double[] ms = new double[3]; long cmp = 0, cp = 0;
                        for (int rep = -1; rep < 3; rep++) {
                            Store s; if (design.startsWith("V1")) s = new V1(); else if (design.startsWith("V3")) s = new V3(); else s = new V2();
                            if (s instanceof V2 v2) { comparisons = 0; v2.bulkLoad(base); } else for (Medicine m : base) s.add(m);
                            comparisons = 0; copied = 0; List<String> last = null; long t0 = System.nanoTime();
                            for (Medicine m : fresh) { s.add(m); for (int k = 0; k < R; k++) last = s.codeOrderReport(); s.updateStock(base.get(0).code(), 5); }
                            long t1 = System.nanoTime(); sink += last.size();
                            if (rep >= 0) { ms[rep] = (t1 - t0) / 1e6; cmp = comparisons; cp = copied; csv.printf("%s,%d,%d,%d,%d,%.3f,%d,%d%n", design, n, R, W, rep, ms[rep], cmp, cp); }
                            if (rep == 2) { if (reference == null) reference = new ArrayList<>(last); else if (!reference.equals(last)) throw new AssertionError("reports differ: " + design); }
                        }
                        System.out.printf("%-26s %8d %6d | %12.3f %14d %16d%n", design, n, R, mean(ms), cmp, cp);
                    }
                }
            }
            System.out.println("(all three designs produced identical code-order reports in every run)");
        }
        // lookup cost must not get worse: 100,000 lookups at n = 100,000
        int n = 100_000; Random rr = new Random(3); List<Medicine> base = new ArrayList<>(); for (int i = 0; i < n; i++) base.add(new Medicine(String.format("MED%07d", 2 * i), "d", i));
        String[] probe = new String[100_000]; for (int i = 0; i < probe.length; i++) probe[i] = base.get(rr.nextInt(n)).code();
        System.out.println("\n100,000 lookups, n = 100,000 (ms, mean of 5):");
        try (PrintWriter csv = new PrintWriter("data/q7_lookup.csv")) {
            csv.println("design,rep,ms");
            for (String design : new String[]{"V1", "V2", "V3"}) {
                Store s = design.equals("V1") ? new V1() : design.equals("V2") ? new V2() : new V3();
                if (s instanceof V2 v2) v2.bulkLoad(base); else for (Medicine m : base) s.add(m);
                double[] ms = new double[5];
                for (int rep = -1; rep < 5; rep++) { long t0 = System.nanoTime(); long a = 0; for (String c : probe) a += s.get(c).stock(); sink += a; if (rep >= 0) { ms[rep] = (System.nanoTime() - t0) / 1e6; csv.printf("%s,%d,%.3f%n", design, rep, ms[rep]); } }
                System.out.printf("  %s : %.2f ms%n", design, mean(ms));
            }
        }
        // retained memory per medicine
        System.out.println("\nRetained heap for n = 100,000 medicines (approximate, after GC):");
        for (String design : new String[]{"V1", "V2", "V3"}) {
            for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(40); }
            Runtime rt = Runtime.getRuntime(); long u0 = rt.totalMemory() - rt.freeMemory();
            Store s = design.equals("V1") ? new V1() : design.equals("V2") ? new V2() : new V3();
            if (s instanceof V2 v2) v2.bulkLoad(base); else for (Medicine m : base) s.add(m);
            for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(40); }
            long u1 = rt.totalMemory() - rt.freeMemory(); System.out.printf("  %s : about %d bytes per medicine (includes the shared Medicine objects)%n", design, Math.max(0, u1 - u0) / n);
            sink += s.get(base.get(0).code()).stock();
        }
        System.out.println("(checksum " + sink + ")");
    }
}
