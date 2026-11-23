import java.nio.file.*;
import java.util.*;

/** B1.4 - time to find a pass as the file grows; plus the HashMap alternative. */
public class GrowthBench {
    static double median(double[] a) { Arrays.sort(a); return a[a.length / 2]; }

    public static void main(String[] args) throws Exception {
        int[] sizes = {1000, 5000, 10000, 25000, 50000, 100000};
        // global JIT warm-up so that the first (smallest) size is not penalised
        { Path w = Files.createTempFile("warm", ".csv"); BusPassStore ws = new BusPassStore(w);
          StringBuilder b = new StringBuilder();
          for (int i = 1; i <= 20000; i++) b.append("P").append(String.format("%06d", i)).append(",192310001,S,1,100.0\n");
          Files.writeString(w, b.toString());
          for (int i = 0; i < 150; i++) { ws.find("P020000"); ws.findStrict("P020000"); }
          for (int i = 0; i < 20; i++) ws.loadIndex();
          Files.delete(w); }
        System.out.println("n,scan_find_last_ms,scan_find_missing_ms,hashmap_load_ms,hashmap_lookup_us_per_query,1000_searches_scan_ms,1000_searches_hashmap_total_ms");
        for (int n : sizes) {
            Path f = Files.createTempFile("bench", ".csv");
            BusPassStore s = new BusPassStore(f);
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= n; i++)
                sb.append("P").append(String.format("%06d", i)).append(",19231").append(String.format("%04d", i % 10000))
                  .append(",Student").append(i).append(",").append(1 + i % 40).append(",").append(3500 + i % 1000).append(".0\n");
            Files.writeString(f, sb.toString());
            String last = "P" + String.format("%06d", n);

            for (int w = 0; w < 20; w++) { s.find(last); s.findStrict(last); }      // JIT warm-up
            double[] tl = new double[31], tm = new double[31];
            for (int r = 0; r < 31; r++) {
                long t = System.nanoTime(); s.find(last);      tl[r] = (System.nanoTime() - t) / 1e6;
                t = System.nanoTime();      s.find("P999999"); tm[r] = (System.nanoTime() - t) / 1e6;
            }
            long t = System.nanoTime();
            Map<String, String[]> idx = s.loadIndex();
            double load = (System.nanoTime() - t) / 1e6;

            Random rnd = new Random(42); String[] ids = new String[1000];
            for (int i = 0; i < 1000; i++) ids[i] = "P" + String.format("%06d", 1 + rnd.nextInt(n));
            int hit = 0;
            for (int rep = 0; rep < 50; rep++) for (String id : ids) if (idx.get(id) != null) hit++; // warm-up
            t = System.nanoTime();
            for (int rep = 0; rep < 100; rep++) for (String id : ids) if (idx.get(id) != null) hit++;
            double lookupPerQueryUs = (System.nanoTime() - t) / 1e3 / 100000.0;     // microseconds per lookup
            double lookupTotalMs = lookupPerQueryUs * 1000 / 1000.0;                  // ms for 1000 lookups

            // 1000 scans would be slow for large n; measure 20 scans and extrapolate
            t = System.nanoTime();
            for (int i = 0; i < 20; i++) s.find(ids[i]);
            double scan1000 = (System.nanoTime() - t) / 1e6 / 20 * 1000;

            System.out.printf("%d,%.3f,%.3f,%.1f,%.4f,%.0f,%.1f%n", n, median(tl), median(tm), load,
                              lookupPerQueryUs, scan1000, load + lookupTotalMs);
            Files.delete(f);
        }
    }
}
