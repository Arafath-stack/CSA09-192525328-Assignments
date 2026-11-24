import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Alternatives: parallel stream, and one thread per file. */
public class Alternatives {
    static long rssKb() { try { for (String l : Files.readAllLines(Paths.get("/proc/self/status"))) if (l.startsWith("VmRSS:")) return Long.parseLong(l.replaceAll("[^0-9]", "")); } catch (Exception e) { } return -1; }

    static void parallelStream(Path dir, List<String> ids, int latency, boolean catchInside) {
        ConcurrentHashMap<String, Result> ok = new ConcurrentHashMap<>(); AtomicInteger bad = new AtomicInteger();
        long t0 = System.nanoTime();
        try {
            ids.parallelStream().forEach(id -> {
                try { Result r = ScoreReader.read(dir, id, latency); ok.put(id, r); }
                catch (ScoreFileException e) {
                    if (catchInside) bad.incrementAndGet();                                    // handled inside the lambda
                    else throw new RuntimeException(e);                                        // a checked exception cannot leave a lambda: it must be wrapped
                }
            });
            System.out.printf("  parallel stream, %s: ok=%d, failed=%d, %d ms%n", catchInside ? "failures caught inside the lambda" : "failures NOT caught", ok.size(), bad.get(), (System.nanoTime() - t0) / 1_000_000);
        } catch (RuntimeException e) {
            System.out.printf("  parallel stream, failures NOT caught: stream ABORTED after %d of %d files with %s (%d ms)%n", ok.size(), ids.size(), e.getCause() == null ? e : e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage(), (System.nanoTime() - t0) / 1_000_000);
        }
    }

    static void threadPerFile(Path dir, List<String> ids, int latency) throws Exception {
        java.lang.management.ThreadMXBean mx = ManagementFactory.getThreadMXBean(); mx.resetPeakThreadCount();
        ConcurrentHashMap<String, Result> ok = new ConcurrentHashMap<>(); AtomicInteger bad = new AtomicInteger();
        long rss0 = rssKb(), t0 = System.nanoTime(); long rssPeak = rss0; int created = 0;
        List<Thread> ts = new ArrayList<>();
        try {
            for (String id : ids) {
                Thread t = new Thread(() -> { try { ok.put(id, ScoreReader.read(dir, id, latency)); } catch (ScoreFileException e) { bad.incrementAndGet(); } });
                t.start(); ts.add(t); created++;
                if ((created & 255) == 0) rssPeak = Math.max(rssPeak, rssKb());
            }
        } catch (OutOfMemoryError e) {
            System.out.println("  OutOfMemoryError after creating " + created + " threads: " + e.getMessage());
        }
        for (Thread t : ts) t.join();
        System.out.printf("  one thread per file (%d files, latency %d ms): ok=%d, failed=%d, %d ms, peak live threads %d, memory grew by about %d MB (resident), stacks reserved: %d threads x 1 MB%n",
                ids.size(), latency, ok.size(), bad.get(), (System.nanoTime() - t0) / 1_000_000, mx.getPeakThreadCount(), (rssPeak - rss0) / 1024, mx.getPeakThreadCount());
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args[0]); int latency = Integer.parseInt(args[1]); int limit = Integer.parseInt(args[2]);
        List<String> ids = Files.readAllLines(dir.resolve("ids.txt")); if (limit < ids.size()) ids = ids.subList(0, limit);
        System.out.println("ForkJoinPool.commonPool() parallelism = " + ForkJoinPool.commonPool().getParallelism() + " (available processors " + Runtime.getRuntime().availableProcessors() + ")");
        System.out.println("files: " + ids.size() + ", latency per file " + latency + " ms");
        parallelStream(dir, ids, latency, true);
        parallelStream(dir, ids, latency, false);
        threadPerFile(dir, ids, latency);
        ResultProcessor.Summary s = ResultProcessor.process(dir, ids, 8, latency);
        System.out.printf("  ExecutorService (8 threads): ok=%d, corrupt=%d, missing=%d, %d ms, 8 threads = about 8 MB of stack reserved%n", s.ok(), s.corrupt(), s.missing(), s.millis());
    }
}
