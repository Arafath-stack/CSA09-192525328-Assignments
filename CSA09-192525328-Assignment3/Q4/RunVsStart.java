public class RunVsStart {
    static class Job extends Thread {
        Job(String n) { super(n); }
        @Override public void run() {
            System.out.println("  run() executes in thread: " + Thread.currentThread().getName());
            try { Thread.sleep(100); } catch (InterruptedException e) { }
        }
    }
    public static void main(String[] args) throws InterruptedException {
        System.out.println("main thread is: " + Thread.currentThread().getName());
        long t0 = System.nanoTime();
        Job a = new Job("scanner-A"), b = new Job("scanner-B");
        System.out.println("calling a.run() and b.run() (plain method calls):");
        a.run(); b.run();
        long t1 = System.nanoTime();
        System.out.println("  took " + (t1 - t0) / 1_000_000 + " ms (sequential: 100 + 100), a.isAlive()=" + a.isAlive() + ", state=" + a.getState());
        Job c = new Job("scanner-C"), d = new Job("scanner-D");
        System.out.println("calling c.start() and d.start():");
        c.start(); d.start(); c.join(); d.join();
        long t2 = System.nanoTime();
        System.out.println("  took " + (t2 - t1) / 1_000_000 + " ms (parallel: about 100), c state after join = " + c.getState());
        try { c.start(); } catch (IllegalThreadStateException e) { System.out.println("starting a finished thread again -> " + e.getClass().getSimpleName()); }
    }
}
