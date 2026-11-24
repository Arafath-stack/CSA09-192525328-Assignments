import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Way 1: extend Thread. */
class StationThread extends Thread {
    static final List<String> finishOrder = Collections.synchronizedList(new ArrayList<>());
    private final int scripts, delayMs;
    volatile int scanned;
    StationThread(String name, int scripts, int delayMs) { super(name); this.scripts = scripts; this.delayMs = delayMs; }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { return; }     // simulate scanning one script
            scanned++;
        }
        finishOrder.add(getName());
        System.out.println("  " + getName() + " finished, scanned " + scanned);
    }
}
/** Way 2: implement Runnable (the task) and give it to a Thread (the worker). */
class StationTask implements Runnable {
    private final String name; private final int scripts, delayMs;
    volatile int scanned;
    StationTask(String name, int scripts, int delayMs) { this.name = name; this.scripts = scripts; this.delayMs = delayMs; }
    @Override public void run() {
        for (int i = 0; i < scripts; i++) {
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { return; }
            scanned++;
        }
        StationThread.finishOrder.add(name);
        System.out.println("  " + name + " finished, scanned " + scanned);
    }
}

public class Stations {
    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "thread";             // thread | runnable | priority | daemon
        int this_scripts = 10, delay = 10;
        long t0 = System.nanoTime();
        Thread[] workers = new Thread[4]; int[] counts = new int[4];
        StationThread[] ts = new StationThread[4]; StationTask[] tasks = new StationTask[4];
        for (int s = 0; s < 4; s++) {
            String name = "Station-" + (s + 1);
            int scripts = (mode.equals("daemon") && s == 2) ? 40 : this_scripts;
            if (mode.equals("runnable")) { tasks[s] = new StationTask(name, scripts, delay); workers[s] = new Thread(tasks[s], name + "(thread)"); }
            else { ts[s] = new StationThread(name, scripts, delay); workers[s] = ts[s]; }
        }
        if (mode.equals("priority")) { workers[0].setPriority(Thread.MIN_PRIORITY); workers[3].setPriority(Thread.MAX_PRIORITY); }
        if (mode.equals("daemon")) { workers[2].setDaemon(true); }
        for (Thread w : workers) w.start();
        System.out.println("started; alive: " + workers[0].isAlive() + " " + workers[1].isAlive() + " " + workers[2].isAlive() + " " + workers[3].isAlive());
        Thread.sleep(35);                                               // main sleeps: stations are still running
        int stillAlive = 0; for (Thread w : workers) if (w.isAlive()) stillAlive++;
        System.out.println("after 35 ms main sees " + stillAlive + " station(s) alive");
        for (Thread w : workers) { if (mode.equals("daemon") && w.isDaemon()) continue; w.join(); }   // wait for (non-daemon) stations
        int total = 0;
        for (int s = 0; s < 4; s++) total += mode.equals("runnable") ? tasks[s].scanned : ts[s].scanned;
        System.out.println("TOTAL scanned = " + total + "   finish order: " + StationThread.finishOrder + "   (" + (System.nanoTime() - t0) / 1_000_000 + " ms)");
    }
}
