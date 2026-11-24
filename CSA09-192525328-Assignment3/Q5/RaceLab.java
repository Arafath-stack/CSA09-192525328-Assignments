import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.LockSupport;

/** Student A and B: reproduce the race reliably, then measure the strategies. */
public class RaceLab {
    interface Cinema { void book(int seat); }
    static final AtomicIntegerArray sales = new AtomicIntegerArray(20000);   // how many times each seat was SOLD (the truth)
    static final int GATEWAY_NANOS = 20_000;                                  // non-critical work: payment gateway wait (20 microseconds)

    static void paymentCheck() { LockSupport.parkNanos(GATEWAY_NANOS); }       // blocks WITHOUT using the CPU

    /** No synchronization; a short pause between the check and the update models a slow database call. */
    static class Unsafe implements Cinema {
        final boolean[] booked; int made;
        Unsafe(int seats) { booked = new boolean[seats]; }
        public void book(int seat) {
            paymentCheck();
            if (!booked[seat]) { LockSupport.parkNanos(1000); booked[seat] = true; made++; sales.incrementAndGet(seat); }
        }
    }
    /** synchronized method: the payment wait is inside the lock. */
    static class SyncMethod implements Cinema {
        final boolean[] booked; int made;
        SyncMethod(int seats) { booked = new boolean[seats]; }
        public synchronized void book(int seat) {
            paymentCheck();
            if (!booked[seat]) { booked[seat] = true; made++; sales.incrementAndGet(seat); }
        }
    }
    /** synchronized block: only check-and-set (the critical section) is locked. */
    static class SyncBlock implements Cinema {
        final boolean[] booked; int made; private final Object lock = new Object();
        SyncBlock(int seats) { booked = new boolean[seats]; }
        public void book(int seat) {
            paymentCheck();
            synchronized (lock) { if (!booked[seat]) { booked[seat] = true; made++; sales.incrementAndGet(seat); } }
        }
    }

    static long run(Cinema c, int threads, int bookings, int seats) throws InterruptedException {
        for (int i = 0; i < sales.length(); i++) sales.set(i, 0);
        CountDownLatch go = new CountDownLatch(1);
        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int id = t;
            ts[t] = new Thread(() -> {
                try { go.await(); } catch (InterruptedException e) { }
                for (int k = id; k < bookings; k += threads) c.book(k % seats);
            });
            ts[t].start();
        }
        long t0 = System.nanoTime(); go.countDown();
        for (Thread t : ts) t.join();
        return (System.nanoTime() - t0) / 1_000_000;
    }
    static int doubles(int seats) { int d = 0; for (int s = 0; s < seats; s++) if (sales.get(s) > 1) d++; return d; }
    static int totalSales(int seats) { int n = 0; for (int s = 0; s < seats; s++) n += sales.get(s); return n; }

    public static void main(String[] args) throws Exception {
        System.out.println("--- Student A: a test that reliably reproduces double booking (16 threads, all start together, 160 attempts, 10 seats) ---");
        int[] doubleRuns = new int[10];
        for (int r = 0; r < 10; r++) {
            Unsafe u = new Unsafe(10); run(u, 16, 160, 10);
            doubleRuns[r] = doubles(10);
            System.out.printf("run %2d: seats sold %3d (should be 10), seats sold more than once %2d, counter says %3d%n", r + 1, totalSales(10), doubleRuns[r], u.made);
        }
        System.out.println("--- same test with the two synchronized versions ---");
        for (int r = 0; r < 3; r++) {
            SyncMethod m = new SyncMethod(10); run(m, 16, 160, 10);
            SyncBlock b = new SyncBlock(10); run(b, 16, 160, 10);
            System.out.printf("run %d: synchronized method sold %d (doubles %d), synchronized block sold %d (doubles %d)%n", r + 1, m.made, 0, b.made, doubles(10));
        }
        System.out.println("\n--- Student B: time for 10,000 bookings (16 threads, 2,000 seats, 20 microseconds payment wait per booking outside the critical section) ---");
        System.out.printf("%-22s %10s %12s %14s%n", "version", "time ms", "seats sold", "double bookings");
        for (int rep = 0; rep < 3; rep++) {
            Cinema[] cs = {new Unsafe(2000), new SyncMethod(2000), new SyncBlock(2000)};
            String[] names = {"no synchronization", "synchronized method", "synchronized block"};
            for (int i = 0; i < 3; i++) {
                long ms = run(cs[i], 16, 10000, 2000);
                if (rep == 2) System.out.printf("%-22s %10d %12d %14d%n", names[i], ms, totalSales(2000), doubles(2000));
            }
        }
    }
}
