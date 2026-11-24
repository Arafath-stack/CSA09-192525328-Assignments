import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

class Account {
    final int id; int balance;
    Account(int id, int balance) { this.id = id; this.balance = balance; }
}

public class Transfers {
    /** Deadlock-prone: locks FROM then TO, in the order the caller gives. */
    static void transferUnsafe(Account from, Account to, int amt, long pauseMs) throws InterruptedException {
        synchronized (from) {
            Thread.sleep(pauseMs);                          // models the time between the two lock requests
            synchronized (to) { from.balance -= amt; to.balance += amt; }
        }
    }
    /** Safe: always lock the account with the smaller id first (consistent global lock order). */
    static void transferOrdered(Account from, Account to, int amt, long pauseMs) throws InterruptedException {
        Account first = from.id < to.id ? from : to, second = first == from ? to : from;
        synchronized (first) {
            Thread.sleep(pauseMs);
            synchronized (second) { from.balance -= amt; to.balance += amt; }
        }
    }
    interface Transfer { void go(Account a, Account b, int amt, long pause) throws InterruptedException; }

    static boolean twoOppositeTransfers(Transfer t, String label) throws Exception {
        Account a = new Account(1, 1000), b = new Account(2, 1000);
        Thread t1 = new Thread(() -> { try { t.go(a, b, 100, 100); } catch (InterruptedException e) { } }, "T1 (A->B)");
        Thread t2 = new Thread(() -> { try { t.go(b, a, 50, 100); } catch (InterruptedException e) { } }, "T2 (B->A)");
        t1.setDaemon(true); t2.setDaemon(true); t1.start(); t2.start();
        t1.join(1500); t2.join(1500);
        boolean stuck = t1.isAlive() || t2.isAlive();
        System.out.println(label + ": " + (stuck ? "DEADLOCK - both threads still blocked after 1.5 s" : "finished, balances A=" + a.balance + " B=" + b.balance + " (total " + (a.balance + b.balance) + ")"));
        if (stuck) {
            ThreadMXBean mx = ManagementFactory.getThreadMXBean();
            long[] ids = mx.findDeadlockedThreads();
            System.out.println("  ThreadMXBean.findDeadlockedThreads() reports " + (ids == null ? 0 : ids.length) + " threads:");
            if (ids != null) for (ThreadInfo ti : mx.getThreadInfo(ids)) System.out.println("   " + ti.getThreadName() + " is " + ti.getThreadState() + ", waiting for " + ti.getLockName() + " which is owned by " + ti.getLockOwnerName());
        }
        return stuck;
    }
    public static void main(String[] args) throws Exception {
        System.out.println("--- Two opposite transfers (A->B and B->A) with the unsafe order ---");
        twoOppositeTransfers(Transfers::transferUnsafe, "unsafe lock order");
        System.out.println("\n--- The same two transfers with the lock-ordering rule ---");
        twoOppositeTransfers(Transfers::transferOrdered, "ordered locks   ");

        System.out.println("\n--- Stress test: 8 threads x 200 random transfers among 4 accounts, 5 s limit ---");
        for (int mode = 0; mode < 2; mode++) {
            Account[] acc = new Account[4]; for (int i = 0; i < 4; i++) acc[i] = new Account(i, 1000);
            Thread[] ts = new Thread[8];
            final int m = mode;
            for (int t = 0; t < 8; t++) { final int seed = t; ts[t] = new Thread(() -> { java.util.Random r = new java.util.Random(seed);
                try { for (int k = 0; k < 200; k++) { int x = r.nextInt(4), y = (x + 1 + r.nextInt(3)) % 4; if (m == 0) transferUnsafe(acc[x], acc[y], 1, 0); else transferOrdered(acc[x], acc[y], 1, 0); } } catch (InterruptedException e) { } });
                ts[t].setDaemon(true); ts[t].start(); }
            long end = System.currentTimeMillis() + 5000; boolean stuck = false;
            for (Thread t : ts) { long left = end - System.currentTimeMillis(); if (left > 0) t.join(left); if (t.isAlive()) stuck = true; }
            int total = 0; for (Account a : acc) total += a.balance;
            System.out.println((mode == 0 ? "unsafe order : " : "ordered locks: ") + (stuck ? "HUNG (deadlock)" : "completed") + ", total money = " + total + " (must stay 4000 when completed)");
        }
    }
}
