import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/** Stage 2 design: ExecutorService + Callable + Future; ConcurrentHashMap for shared results; one failure never stops the run. */
public class ResultProcessor {
    record Summary(int total, int ok, int corrupt, int missing, int other, long sumTotals, Map<String, Long> grades, Map<String, String> failures, long millis) { }

    static Summary process(Path dir, List<String> ids, int threads, int latencyMs) throws InterruptedException {
        ConcurrentHashMap<String, Result> results = new ConcurrentHashMap<>();            // written by the worker threads
        ConcurrentHashMap<String, LongAdder> grades = new ConcurrentHashMap<>();           // grade distribution, low-contention counters
        Map<String, String> failures = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long t0 = System.nanoTime();
        List<Future<Result>> futures = new ArrayList<>(ids.size());
        for (String id : ids) {
            futures.add(pool.submit(() -> {                                                 // Callable<Result>
                Result r = ScoreReader.read(dir, id, latencyMs);                            // may throw a checked ScoreFileException
                if (results.putIfAbsent(r.id(), r) != null) throw new ScoreFileException(id + ": duplicate result");
                grades.computeIfAbsent(r.grade(), g -> new LongAdder()).increment();
                return r;
            }));
        }
        int corrupt = 0, missing = 0, other = 0;
        for (int i = 0; i < futures.size(); i++) {
            String id = ids.get(i);
            try {
                futures.get(i).get(60, TimeUnit.SECONDS);
            } catch (ExecutionException e) {                                                // the task failed: classify, record, CONTINUE
                Throwable cause = e.getCause();
                if (cause instanceof MissingFileException) { missing++; failures.put(id, "MISSING"); }
                else if (cause instanceof CorruptFileException) { corrupt++; failures.put(id, "CORRUPT: " + cause.getMessage()); }
                else { other++; failures.put(id, "OTHER: " + cause); }                      // unexpected exception types are still counted, never lost
            } catch (TimeoutException e) {
                other++; failures.put(id, "TIMEOUT"); futures.get(i).cancel(true);
            }
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long sum = 0; for (Result r : results.values()) sum += r.total();
        Map<String, Long> g = new TreeMap<>(); grades.forEach((k, v) -> g.put(k, v.sum()));
        return new Summary(ids.size(), results.size(), corrupt, missing, other, sum, g, failures, ms);
    }

    static void print(Summary s) {
        System.out.println("files expected      : " + s.total());
        System.out.println("processed OK        : " + s.ok() + "   (sum of totals " + s.sumTotals() + ")");
        System.out.println("failed - corrupt    : " + s.corrupt());
        System.out.println("failed - missing    : " + s.missing());
        System.out.println("failed - other      : " + s.other());
        System.out.println("check ok+failed = total: " + (s.ok() + s.corrupt() + s.missing() + s.other() == s.total()));
        System.out.println("grade distribution  : " + s.grades());
        System.out.println("time                : " + s.millis() + " ms");
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        int threads = args.length > 1 ? Integer.parseInt(args[1]) : 4, latency = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        List<String> ids = Files.readAllLines(dir.resolve("ids.txt"));
        Summary s = process(dir, ids, threads, latency);
        print(s);
        if (args.length > 3 && args[3].equals("list")) new TreeMap<>(s.failures()).forEach((id, why) -> System.out.println("  " + id + " -> " + why));
        Path exp = dir.resolve("expected.txt");
        if (Files.exists(exp)) System.out.println("expected (from generator): " + Files.readString(exp).trim() + "   | actual: ok=" + s.ok() + " corrupt=" + s.corrupt() + " missing=" + s.missing() + " totalSum=" + s.sumTotals());
    }
}
