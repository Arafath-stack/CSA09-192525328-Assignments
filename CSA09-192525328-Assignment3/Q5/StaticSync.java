/** Student B: static synchronization and intrinsic locks. */
public class StaticSync {
    static int totalTickets = 0;                         // shared by ALL screens (static)

    static class Screen {
        synchronized void sellInstanceLock() { int t = totalTickets; Thread.yield(); totalTickets = t + 1; }   // read-modify-write with a switch point                  // locks THIS screen only
        static synchronized void sellStaticLock() { int t = totalTickets; Thread.yield(); totalTickets = t + 1; }             // locks the Class object: one lock for every screen
        void whichLocks() {
            synchronized (this) { System.out.println("  inside synchronized method/block(this): holds this-lock = " + Thread.holdsLock(this) + ", class-lock = " + Thread.holdsLock(Screen.class)); }
            synchronized (Screen.class) { System.out.println("  inside static synchronized / block(Screen.class): holds this-lock = " + Thread.holdsLock(this) + ", class-lock = " + Thread.holdsLock(Screen.class)); }
        }
    }
    static int run(boolean staticLock) throws InterruptedException {
        totalTickets = 0;
        Screen north = new Screen(), south = new Screen();              // two screens = two different objects = two different locks
        Thread[] ts = new Thread[4];
        for (int i = 0; i < 4; i++) {
            final Screen s = i % 2 == 0 ? north : south;
            ts[i] = new Thread(() -> { for (int k = 0; k < 20_000; k++) { if (staticLock) Screen.sellStaticLock(); else s.sellInstanceLock(); } });
            ts[i].start();
        }
        for (Thread t : ts) t.join();
        return totalTickets;
    }
    public static void main(String[] args) throws Exception {
        new Screen().whichLocks();
        System.out.println("shared static counter, 4 threads x 20,000 increments (correct total 80,000):");
        for (int r = 1; r <= 3; r++) System.out.println("  instance lock (2 screens): " + run(false) + "      static lock: " + run(true));
        System.out.println("A synchronized instance method locks only its own object, so two screens do NOT exclude each other when they update a static (shared) variable.");
    }
}
