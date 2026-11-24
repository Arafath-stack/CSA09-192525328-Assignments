import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Alternatives: ArrayBlockingQueue(5) for the counter, tryLock with timeout for transfers. */
public class Alternatives {
    static long allocated() { return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().threadId()); }

    static double blockingQueue(int tokens) throws Exception {
        ArrayBlockingQueue<Integer> q = new ArrayBlockingQueue<>(5);
        Thread cook = new Thread(() -> { try { for (int t = 1; t <= tokens; t++) q.put(t); } catch (InterruptedException e) { } });
        long[] sum = new long[2];
        Thread[] st = new Thread[2];
        for (int s = 0; s < 2; s++) { final int id = s; st[s] = new Thread(() -> { try { for (int k = 0; k < tokens / 2; k++) sum[id] += q.take(); } catch (InterruptedException e) { } }); }
        long t0 = System.nanoTime(); cook.start(); for (Thread t : st) t.start(); cook.join(); for (Thread t : st) t.join();
        if (sum[0] + sum[1] != (long) tokens * (tokens + 1) / 2) throw new IllegalStateException("wrong sum");
        return (System.nanoTime() - t0) / (double) tokens;
    }
    static double waitNotify(int tokens) throws Exception {
        BoundedCounter c = new BoundedCounter(5, true, true);
        long t0 = System.nanoTime();
        int[] r = Variants.scenario(c, 1, 2, tokens, 60000);
        if (r[0] != (long) tokens * (tokens + 1) / 2) throw new IllegalStateException("wrong sum " + r[0]);
        return (System.nanoTime() - t0) / (double) tokens;
    }

    static void ordered(Account from, Account to, int amt) {            // lock ordering, no pause
        Account f = from.id < to.id ? from : to, s = f == from ? to : from;
        synchronized (f) { synchronized (s) { from.balance -= amt; to.balance += amt; } }
    }
    static class Acc { final int id; int balance = 1000; final ReentrantLock lock = new ReentrantLock(); Acc(int id) { this.id = id; } }
    static int failedAttempts;
    static boolean tryTransfer(Acc from, Acc to, int amt, long timeoutMs) throws InterruptedException {
        if (from.lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            try {
                if (to.lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
                    try { from.balance -= amt; to.balance += amt; return true; } finally { to.lock.unlock(); }
                }
            } finally { from.lock.unlock(); }
        }
        return false;                                                  // gave up: the caller must retry or report failure
    }
    public static void main(String[] args) throws Exception {
        System.out.println("--- Producer-consumer: 1 cook, 2 students, capacity 5, 60,000 tokens ---");
        for (int w = 0; w < 3; w++) {
            double a = waitNotify(60_000), b = blockingQueue(60_000);
            if (w == 2) System.out.printf("wait/notifyAll counter %.0f ns per token, ArrayBlockingQueue %.0f ns per token%n", a, b);
        }
        new TokenCounter(); new ArrayBlockingQueue<Integer>(5);   // load the classes first so that class loading is not counted
        long b0 = allocated(); long b1 = allocated(); long base = b1 - b0;          // cost of the measuring call itself
        long a0 = allocated(); new TokenCounter(); long a1 = allocated() - base; new ArrayBlockingQueue<Integer>(5); long a2 = allocated() - 2 * base;
        System.out.printf("space of the empty structure: wait/notify TokenCounter %d bytes (int[5] + 3 ints), ArrayBlockingQueue %d bytes (Object[5] + ReentrantLock + 2 Conditions)%n", a1 - a0, a2 - a1);

        System.out.println("\n--- Transfers: 4 accounts, 4 threads x 20,000 random transfers, both locks needed ---");
        for (int w = 0; w < 3; w++) {
            Account[] acc = new Account[4]; Acc[] lk = new Acc[4]; for (int i = 0; i < 4; i++) { acc[i] = new Account(i, 1000); lk[i] = new Acc(i); }
            failedAttempts = 0;
            Thread[] ts = new Thread[4]; long t0 = System.nanoTime();
            for (int t = 0; t < 4; t++) { final int seed = t; ts[t] = new Thread(() -> { java.util.Random r = new java.util.Random(seed);
                for (int k = 0; k < 20000; k++) { int x = r.nextInt(4), y = (x + 1 + r.nextInt(3)) % 4; ordered(acc[x], acc[y], 1); } }); ts[t].start(); }
            for (Thread t : ts) t.join();
            long t1 = System.nanoTime();
            int[] failed = new int[1];
            for (int t = 0; t < 4; t++) { final int seed = t; ts[t] = new Thread(() -> { java.util.Random r = new java.util.Random(seed); int f = 0;
                try { for (int k = 0; k < 20000; k++) { int x = r.nextInt(4), y = (x + 1 + r.nextInt(3)) % 4; if (!tryTransfer(lk[x], lk[y], 1, 20)) f++; } } catch (InterruptedException e) { }
                synchronized (failed) { failed[0] += f; } }); ts[t].start(); }
            for (Thread t : ts) t.join();
            long t2 = System.nanoTime();
            int total = 0; for (Acc a : lk) total += a.balance;
            if (w == 2) System.out.printf("lock ordering: %.0f ns per transfer;  tryLock(20 ms): %.0f ns per transfer, %d transfers had to give up (money conserved: %d)%n", (t1 - t0) / 80000.0, (t2 - t1) / 80000.0, failed[0], total);
        }
        System.out.println("tryLock can itself deadlock-free fail: a transfer that times out must be retried or reported (livelock risk if every thread retries at the same moment).");
    }
}
