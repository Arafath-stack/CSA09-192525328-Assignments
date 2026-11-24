import java.nio.file.*;
import java.util.concurrent.CountDownLatch;

/** Memory used per thread: resident memory of the process before/after starting N parked threads. */
public class ThreadMemory {
    static long rssKb() throws Exception { for (String l : Files.readAllLines(Paths.get("/proc/self/status"))) if (l.startsWith("VmRSS:")) return Long.parseLong(l.replaceAll("[^0-9]", "")); return -1; }
    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]); int stackKb = args.length > 1 ? Integer.parseInt(args[1]) : 1024;
        CountDownLatch release = new CountDownLatch(1), started = new CountDownLatch(n);
        Thread.sleep(300); long before = rssKb();
        Thread[] ts = new Thread[n];
        for (int i = 0; i < n; i++) { ts[i] = new Thread(() -> { started.countDown(); try { release.await(); } catch (InterruptedException e) { } }); ts[i].start(); }
        started.await(); Thread.sleep(300);
        long after = rssKb();
        System.out.printf("%d threads (stack size %d KB): resident memory grew by %d KB = %.1f KB per thread; virtual stack address space reserved: %d MB%n", n, stackKb, after - before, (after - before) / (double) n, (long) n * stackKb / 1024);
        release.countDown(); for (Thread t : ts) t.join();
    }
}
