import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** Alternative: the scanning work as tasks in a fixed thread pool. n scripts, p threads. */
public class PoolStations {
    public static void main(String[] args) throws Exception {
        int n = 400, delay = 5;                                         // 400 scripts, 5 ms each
        System.out.println("n = " + n + " scripts x " + delay + " ms; ideal time = n/p x " + delay + " ms (a single CPU core is enough because the work is waiting)");
        System.out.printf("%-10s %12s %12s%n", "threads p", "measured ms", "ideal ms");
        for (int p : new int[]{1, 2, 4, 8, 16, 32}) {
            long best = Long.MAX_VALUE;
            for (int rep = 0; rep < 3; rep++) {
                ExecutorService pool = Executors.newFixedThreadPool(p);
                List<Future<Integer>> fs = new ArrayList<>();
                long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) fs.add(pool.submit(() -> { Thread.sleep(delay); return 1; }));
                int done = 0; for (Future<Integer> f : fs) done += f.get();
                long ms = (System.nanoTime() - t0) / 1_000_000;
                pool.shutdown(); pool.awaitTermination(5, TimeUnit.SECONDS);
                if (done == n) best = Math.min(best, ms);
            }
            System.out.printf("%-10d %12d %12d%n", p, best, (long) Math.ceil(n / (double) p) * delay);
        }
        System.out.println("\nFour stations as tasks: pool of 2 threads -> two stations run, then two more:");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        long t0 = System.nanoTime();
        List<Future<String>> fs = new ArrayList<>();
        for (int s = 1; s <= 4; s++) { final int id = s; fs.add(pool.submit(() -> { Thread.sleep(100); return "Station-" + id + " on " + Thread.currentThread().getName(); })); }
        for (Future<String> f : fs) System.out.println("  " + f.get() + " at " + (System.nanoTime() - t0) / 1_000_000 + " ms");
        pool.shutdown();
    }
}
