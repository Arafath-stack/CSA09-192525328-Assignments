public class SleepVsWait {
    static final Object lock = new Object();
    static long run(boolean useWait) throws Exception {
        long[] enteredAfter = new long[1];
        Thread holder = new Thread(() -> { synchronized (lock) { try { if (useWait) lock.wait(300); else Thread.sleep(300); } catch (InterruptedException e) { } } });
        holder.start(); Thread.sleep(50);              // holder has the lock and is now sleeping / waiting
        long t0 = System.nanoTime();
        synchronized (lock) { enteredAfter[0] = (System.nanoTime() - t0) / 1_000_000; }
        holder.join();
        return enteredAfter[0];
    }
    public static void main(String[] args) throws Exception {
        System.out.println("thread H holds the lock and pauses for 300 ms; thread main tries to enter synchronized(lock) 50 ms later");
        System.out.println("  H uses Thread.sleep(300): main had to wait " + run(false) + " ms   (sleep keeps the lock)");
        System.out.println("  H uses lock.wait(300)   : main entered after " + run(true) + " ms   (wait releases the lock)");
    }
}
