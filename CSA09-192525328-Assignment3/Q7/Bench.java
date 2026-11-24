import java.nio.file.*;
import java.util.*;

/** Stage 3: total time for pools of 1..64 threads. Warm-up first; repetitions are interleaved so JIT/cache drift hits every pool size equally. Raw data -> timings_latency<L>.csv */
public class Bench {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]); int latency = Integer.parseInt(args[1]);
        List<String> ids = Files.readAllLines(dir.resolve("ids.txt"));
        int[] pools = {1, 2, 4, 8, 16, 32, 64};
        for (int w = 0; w < 6; w++) ResultProcessor.process(dir, ids, 4, 0);                   // warm-up: JIT compiler and page cache
        long[][] t = new long[pools.length][5];
        for (int r = 0; r < 5; r++) for (int i = 0; i < pools.length; i++) t[i][r] = ResultProcessor.process(dir, ids, pools[i], latency).millis();
        System.out.println("latency per file = " + latency + " ms, " + ids.size() + " files, 5 interleaved repetitions per pool size (ms)");
        System.out.printf("%-8s %7s %7s %7s %7s %7s | %7s %8s%n", "threads", "run1", "run2", "run3", "run4", "run5", "median", "speedup");
        StringBuilder csv = new StringBuilder("latency_ms,threads,run1,run2,run3,run4,run5,median\n");
        double base = 0;
        for (int i = 0; i < pools.length; i++) {
            long[] s = t[i].clone(); Arrays.sort(s); long med = s[2];
            if (i == 0) base = med;
            System.out.printf("%-8d %7d %7d %7d %7d %7d | %7d %8.2f%n", pools[i], t[i][0], t[i][1], t[i][2], t[i][3], t[i][4], med, base / med);
            csv.append(latency).append(',').append(pools[i]); for (long x : t[i]) csv.append(',').append(x); csv.append(',').append(med).append('\n');
        }
        Files.writeString(Paths.get("timings_latency" + latency + ".csv"), csv.toString());
    }
}
