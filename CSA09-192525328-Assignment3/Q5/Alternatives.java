import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Alternatives: synchronized, AtomicBoolean[] + compareAndSet, ReentrantLock, ConcurrentHashMap.putIfAbsent. */
public class Alternatives {
    interface Booker { boolean book(int seat); }
    static class Sync implements Booker { final boolean[] b; Sync(int s) { b = new boolean[s]; }
        public synchronized boolean book(int seat) { if (b[seat]) return false; b[seat] = true; return true; } }
    static class Cas implements Booker { final AtomicBoolean[] b; Cas(int s) { b = new AtomicBoolean[s]; for (int i = 0; i < s; i++) b[i] = new AtomicBoolean(); }
        public boolean book(int seat) { return b[seat].compareAndSet(false, true); } }
    static class Lock implements Booker { final boolean[] b; final ReentrantLock l = new ReentrantLock(); Lock(int s) { b = new boolean[s]; }
        public boolean book(int seat) { l.lock(); try { if (b[seat]) return false; b[seat] = true; return true; } finally { l.unlock(); } } }
    static class Chm implements Booker { final ConcurrentHashMap<Integer, Boolean> m = new ConcurrentHashMap<>(); Chm(int s) { }
        public boolean book(int seat) { return m.putIfAbsent(seat, Boolean.TRUE) == null; } }

    static long allocated() { return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().threadId()); }

    static double timePerBooking(Booker bk, int threads, int seats, int perThread, AtomicInteger sold) throws Exception {
        CountDownLatch go = new CountDownLatch(1); sold.set(0);
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) { final int id = t; ts[t] = new Thread(() -> { try { go.await(); } catch (InterruptedException e) { }
            int local = 0; for (int k = 0; k < perThread; k++) if (bk.book((int) (((long) k * 31 + id * 7) % seats))) local++; sold.addAndGet(local); }); ts[t].start(); }
        long t0 = System.nanoTime(); go.countDown(); for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / (double) ((long) threads * perThread);
    }
    public static void main(String[] args) throws Exception {
        String[] names = {"synchronized", "AtomicBoolean CAS", "ReentrantLock", "ConcurrentHashMap"};
        System.out.println("time per booking attempt in ns, 8 threads x 500,000 attempts; correct = every seat sold exactly once");
        for (int seats : new int[]{10, 100000}) {
            System.out.println("\n" + (seats == 10 ? "HIGH contention: 10 seats" : "LOW contention: 100,000 seats"));
            for (int v = 0; v < 4; v++) {
                double best = 1e18; int soldAtBest = 0;
                for (int rep = 0; rep < 4; rep++) {
                    Booker b = v == 0 ? new Sync(seats) : v == 1 ? new Cas(seats) : v == 2 ? new Lock(seats) : new Chm(seats);
                    AtomicInteger sold = new AtomicInteger();
                    double ns = timePerBooking(b, 8, seats, 500_000, sold);
                    if (ns < best) { best = ns; soldAtBest = sold.get(); }
                }
                System.out.printf("  %-18s %8.1f ns per attempt, seats sold %d of %d%n", names[v], best, soldAtBest, seats);
            }
        }
        System.out.println("\nExtra space for s = 100,000 seats (exact bytes allocated while building the structure):");
        int s = 100000; long a0 = allocated(); new Sync(s); long a1 = allocated(); new Cas(s); long a2 = allocated(); new Lock(s); long a3 = allocated();
        Chm c = new Chm(s); for (int i = 0; i < s; i++) c.book(i); long a4 = allocated();
        System.out.printf("  synchronized      : %8d bytes (boolean[], %.1f bytes per seat)%n", a1 - a0, (a1 - a0) / (double) s);
        System.out.printf("  AtomicBoolean CAS : %8d bytes (array of references + one AtomicBoolean object per seat, %.1f bytes per seat)%n", a2 - a1, (a2 - a1) / (double) s);
        System.out.printf("  ReentrantLock     : %8d bytes (boolean[] + one lock object, %.1f bytes per seat)%n", a3 - a2, (a3 - a2) / (double) s);
        System.out.printf("  ConcurrentHashMap : %8d bytes after booking all seats (one entry per booked seat, %.1f bytes per seat)%n", a4 - a3, (a4 - a3) / (double) s);
    }
}
