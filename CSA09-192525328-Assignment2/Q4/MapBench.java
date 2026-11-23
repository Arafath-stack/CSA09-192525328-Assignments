import java.io.*;
import java.util.*;

/** Q4: HashMap vs TreeMap vs LinkedHashMap (vs Hashtable, vs sorted array + binarySearch) for a pharmacy inventory. */
public class MapBench {
    record Medicine(String code, String name, int stock) { }
    static final int REPS = 5, LOOKUPS = 100_000;
    static long sink;
    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static double sd(double[] a) { double m = mean(a), s = 0; for (double x : a) s += (x - m) * (x - m); return Math.sqrt(s / a.length); }
    static PrintWriter csv;
    static long cmpCount;

    interface Body { void run(); }
    static double time(String map, String what, int n, Body prep, Body body) {
        double[] ms = new double[REPS];
        for (int r = -1; r < REPS; r++) { prep.run(); long t0 = System.nanoTime(); body.run(); double d = (System.nanoTime() - t0) / 1e6; if (r >= 0) { ms[r] = d; csv.printf("%s,%s,%d,%d,%.4f%n", map, what, n, r, d); } }
        return mean(ms);
    }

    public static void main(String[] args) throws Exception {
        csv = new PrintWriter("data/q4_raw_times.csv"); csv.println("structure,measure,n,rep,ms");
        PrintWriter ops = new PrintWriter("data/q4_operation_counts.csv"); ops.println("n,sort_comparisons,n_log2_n");
        System.out.printf("%-14s %8s | %12s %14s %16s %16s | %s%n", "structure", "n", "build ms", "100k lookups ms", "code-order rpt ms", "insert-order rpt ms", "order correct?");
        for (int n : new int[]{1_000, 10_000, 100_000, 500_000}) {
            // medicine codes in a random (insertion) order
            List<Integer> ids = new ArrayList<>(); for (int i = 0; i < n; i++) ids.add(i); Collections.shuffle(ids, new Random(11));
            Medicine[] meds = new Medicine[n]; for (int i = 0; i < n; i++) meds[i] = new Medicine(String.format("MED%07d", ids.get(i)), "drug" + i, i % 500);
            String[] insertionOrder = new String[n]; for (int i = 0; i < n; i++) insertionOrder[i] = meds[i].code();
            String[] codeOrder = insertionOrder.clone(); Arrays.sort(codeOrder);
            Random rr = new Random(3); String[] probe = new String[LOOKUPS]; for (int i = 0; i < LOOKUPS; i++) probe[i] = insertionOrder[rr.nextInt(n)];
            Map<String, Medicine>[] m = new Map[1]; List<String>[] side = new List[1];
            String[] kinds = {"HashMap", "TreeMap", "LinkedHashMap", "Hashtable"};
            for (String kind : kinds) {
                Body fresh = () -> { m[0] = switch (kind) { case "HashMap" -> new HashMap<>(); case "TreeMap" -> new TreeMap<>(); case "LinkedHashMap" -> new LinkedHashMap<>(); default -> new Hashtable<>(); }; };
                Body fill = () -> { fresh.run(); for (Medicine x : meds) m[0].put(x.code(), x); };
                double build = time(kind, "build (n puts)", n, fresh, () -> { for (Medicine x : meds) m[0].put(x.code(), x); });
                fill.run();
                double look = time(kind, "100000 lookups", n, () -> { }, () -> { long s = 0; for (String c : probe) s += m[0].get(c).stock(); sink += s; });
                // code-order report
                String[][] got = new String[1][];
                double rep1 = time(kind, "report code order", n, () -> { }, () -> {
                    if (kind.equals("TreeMap")) { got[0] = m[0].keySet().toArray(new String[0]); }
                    else { String[] k = m[0].keySet().toArray(new String[0]); Arrays.sort(k); got[0] = k; } });
                boolean ok1 = Arrays.equals(got[0], codeOrder);
                // insertion-order report
                String ok2s; double rep2;
                if (kind.equals("LinkedHashMap")) { rep2 = time(kind, "report insertion order", n, () -> { }, () -> got[0] = m[0].keySet().toArray(new String[0])); ok2s = Arrays.equals(got[0], insertionOrder) + ""; }
                else { rep2 = Double.NaN; ok2s = "n/a (no order kept)"; }
                System.out.printf("%-14s %8d | %12.2f %14.2f %16.2f %16s | code-order %s, insertion-order %s%n", kind, n, build, look, rep1, Double.isNaN(rep2) ? "-" : String.format("%.2f", rep2), ok1, ok2s);
            }
            // extra: a side ArrayList that preserves insertion order next to a HashMap (what is needed if the "wrong" map is used)
            double sideRep = time("HashMap+ArrayList", "report insertion order", n, () -> { side[0] = new ArrayList<>(Arrays.asList(insertionOrder)); }, () -> got0(side[0]));
            // sorted array + binarySearch
            Medicine[][] arr = new Medicine[1][]; Comparator<Medicine> byCode = Comparator.comparing(Medicine::code);
            double sbuild = time("SortedArray", "build (sort n)", n, () -> arr[0] = meds.clone(), () -> Arrays.sort(arr[0], byCode));
            Arrays.sort(arr[0], byCode);
            Medicine key0 = new Medicine("", "", 0);
            double slook = time("SortedArray", "100000 lookups", n, () -> { }, () -> { long s = 0; for (String c : probe) { int i = Arrays.binarySearch(arr[0], new Medicine(c, "", 0), byCode); s += arr[0][i].stock(); } sink += s; });
            double srep = time("SortedArray", "report code order", n, () -> { }, () -> { long s = 0; for (Medicine x : arr[0]) s += x.stock(); sink += s; });
            // 1,000 insertions into a sorted array (shifting)
            Random r2 = new Random(9); int K = 1000;
            double sins = time("SortedArray", "1000 insertions", n, () -> { Medicine[] big = Arrays.copyOf(arr[0], n + K); arr[0] = big; sizeBox[0] = n; }, () -> {
                for (int i = 0; i < K; i++) { Medicine nm = new Medicine(String.format("MED%07d", r2.nextInt(n)), "n", 1); int pos = Arrays.binarySearch(arr[0], 0, sizeBox[0], nm, byCode); if (pos < 0) pos = -pos - 1;
                    System.arraycopy(arr[0], pos, arr[0], pos + 1, sizeBox[0] - pos); arr[0][pos] = nm; sizeBox[0]++; } });
            System.out.printf("%-14s %8d | %12.2f %14.2f %16.2f %16s | sorted by construction; 1000 insertions: %.2f ms%n", "SortedArray", n, sbuild, slook, srep, "-", sins);
            // operation count for sorting keys (comparisons) -- evidence for Q7
            String[] k = insertionOrder.clone(); cmpCount = 0; Arrays.sort(k, (a, b) -> { cmpCount++; return a.compareTo(b); });
            ops.printf("%d,%d,%.0f%n", n, cmpCount, n * (Math.log(n) / Math.log(2)));
            System.out.printf("   comparisons to sort %d keys: %d  (n*log2 n = %.0f)%n", n, cmpCount, n * (Math.log(n) / Math.log(2)));
        }
        csv.close(); ops.close(); System.out.println("(checksum " + sink + ")");
    }
    static int[] sizeBox = new int[1];
    static void got0(List<String> l) { sink += l.size(); }
}
