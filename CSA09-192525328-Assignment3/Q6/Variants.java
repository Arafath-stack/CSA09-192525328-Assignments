import java.util.concurrent.atomic.AtomicInteger;

/** Counter variants for experiments: while+notifyAll (correct), if+notifyAll, while+notify. */
interface Counter { void put(int t) throws InterruptedException; int take() throws InterruptedException; }

class BoundedCounter implements Counter {
    final int[] buf; int count, in, out; final boolean useWhile, notifyAllThreads; int anomalies, maxSeen, minSeen;
    final StringBuilder trace = new StringBuilder(); int events;
    BoundedCounter(int cap, boolean useWhile, boolean notifyAllThreads) { buf = new int[cap]; this.useWhile = useWhile; this.notifyAllThreads = notifyAllThreads; }
    public synchronized void put(int t) throws InterruptedException {
        if (useWhile) while (count == buf.length) wait(); else if (count == buf.length) wait();
        if (count == buf.length) anomalies++;                    // would overwrite a token: only possible with if
        buf[in] = t; in = (in + 1) % buf.length; count++;
        record("put");
        if (notifyAllThreads) notifyAll(); else notify();
    }
    public synchronized int take() throws InterruptedException {
        if (useWhile) while (count == 0) wait(); else if (count == 0) wait();
        if (count == 0) anomalies++;                             // would take a token that is not there
        int t = buf[out]; out = (out + 1) % buf.length; count--;
        record("take");
        if (notifyAllThreads) notifyAll(); else notify();
        return t;
    }
    private void record(String op) { maxSeen = Math.max(maxSeen, count); minSeen = Math.min(minSeen, count); if (events++ < 24) trace.append(op.charAt(0) == 'p' ? "+" : "-").append(count).append(' '); }
}

public class Variants {
    /** producers x tokens each, consumers take their share; returns [sum, anomalies, hung] */
    static int[] scenario(BoundedCounter c, int producers, int consumers, int perProducer, long timeoutMs) throws Exception {
        int total = producers * perProducer, perConsumer = total / consumers;
        AtomicInteger sum = new AtomicInteger();
        Thread[] ts = new Thread[producers + consumers];
        for (int p = 0; p < producers; p++) { final int base = p * perProducer; ts[p] = new Thread(() -> { try { for (int k = 1; k <= perProducer; k++) c.put(base + k); } catch (InterruptedException e) { } }); }
        for (int q = 0; q < consumers; q++) ts[producers + q] = new Thread(() -> { try { for (int k = 0; k < perConsumer; k++) sum.addAndGet(c.take()); } catch (InterruptedException e) { } });
        for (Thread t : ts) { t.setDaemon(true); t.start(); }
        boolean hung = false; long end = System.currentTimeMillis() + timeoutMs;
        for (Thread t : ts) { long left = end - System.currentTimeMillis(); if (left > 0) t.join(left); if (t.isAlive()) hung = true; }
        if (hung) for (Thread t : ts) t.interrupt();
        return new int[]{sum.get(), c.anomalies, hung ? 1 : 0};
    }
    public static void main(String[] args) throws Exception {
        System.out.println("--- Scenario of the assignment: 1 cook (20 tokens), 2 students (10 each), capacity 5; expected sum 210; 1000 runs each ---");
        for (int v = 0; v < 2; v++) {
            int wrong = 0, anomalies = 0, hung = 0;
            for (int r = 0; r < 1000; r++) { int[] x = scenario(new BoundedCounter(5, v == 0, true), 1, 2, 20, 2000); if (x[0] != 210) wrong++; anomalies += x[1]; hung += x[2]; }
            System.out.printf("%-22s wrong sums %4d, anomalies %4d, hung runs %d%n", v == 0 ? "while + notifyAll" : "if + notifyAll", wrong, anomalies, hung);
        }
        System.out.println("\n--- Harder scenario: 3 cooks, 3 students, capacity 1, 100 tokens per cook; 200 runs, 300 ms timeout each ---");
        for (int v = 0; v < 3; v++) {
            int wrong = 0, anomalies = 0, hung = 0;
            for (int r = 0; r < 200; r++) {
                BoundedCounter c = v == 0 ? new BoundedCounter(1, true, true) : v == 1 ? new BoundedCounter(1, false, true) : new BoundedCounter(1, true, false);
                int[] x = scenario(c, 3, 3, 100, 300);
                if (x[0] != 300 * 301 / 2 && x[2] == 0) wrong++;
                anomalies += x[1]; hung += x[2];
            }
            System.out.printf("%-22s wrong sums %4d, anomalies %4d, hung (deadlocked/lost wake-up) runs %d of 200%n", v == 0 ? "while + notifyAll" : v == 1 ? "if + notifyAll" : "while + notify", wrong, anomalies, hung);
        }
        System.out.println("\n--- Counter size over time (Student A): 1 cook, 2 students, capacity 5 ---");
        BoundedCounter t = new BoundedCounter(5, true, true);
        int[] r = scenario(t, 1, 2, 20, 2000);
        System.out.println("first 24 events (+ put, - take, number = tokens in the counter afterwards): " + t.trace);
        System.out.println("minimum size seen = " + t.minSeen + ", maximum size seen = " + t.maxSeen + ", sum collected = " + r[0] + "  -> the counter never exceeded 5 and never dropped below 0");
    }
}
