/** Cost of capturing a stack trace per exception, and the effect of chaining on memory. */
public class StackCost {
    static class Chk extends Exception { private static final long serialVersionUID = 1L; Chk(String m, Throwable c) { super(m, c); } Chk(String m) { super(m); } }
    static class Unchk extends RuntimeException { private static final long serialVersionUID = 1L; Unchk(String m) { super(m); } }
    static long allocated() { return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().threadId()); }
    static Throwable at(int depth, int chain) {
        if (depth > 0) return at(depth - 1, chain);
        Throwable t = new java.io.IOException("root");
        for (int i = 0; i < chain; i++) t = new Chk("level " + i, t);
        return t;
    }
    static Object atKind(int depth, boolean checked) {
        if (depth > 0) return atKind(depth - 1, checked);
        return checked ? new Chk("x") : new Unchk("x");
    }
    public static void main(String[] args) {
        int N = 200_000; Object sink = null;
        System.out.println("--- Checked versus unchecked: same cost ---");
        for (int w = 0; w < 3; w++) {
            long t0 = System.nanoTime(); for (int i = 0; i < N; i++) sink = atKind(20, true);
            long t1 = System.nanoTime(); for (int i = 0; i < N; i++) sink = atKind(20, false);
            long t2 = System.nanoTime();
            if (w == 2) System.out.printf("create at depth 20: checked %.0f ns, unchecked %.0f ns%n", (t1 - t0) / (double) N, (t2 - t1) / (double) N);
        }
        System.out.println("\n--- Time and space to create ONE exception, by stack depth ---");
        for (int depth : new int[]{1, 10, 50, 200}) {
            for (int w = 0; w < 3; w++) {
                long a0 = allocated(); long t0 = System.nanoTime();
                for (int i = 0; i < 20000; i++) sink = at(depth, 0);
                long t1 = System.nanoTime(); long a1 = allocated();
                if (w == 2) System.out.printf("depth %3d: %8.0f ns per exception, %7.0f bytes per exception%n", depth, (t1 - t0) / 20000.0, (a1 - a0) / 20000.0);
            }
        }
        System.out.println("\n--- Effect of chaining (depth 20): every link has its own stack trace ---");
        for (int chain : new int[]{0, 1, 2, 4}) {
            long a0 = allocated(); for (int i = 0; i < 20000; i++) sink = at(20, chain); long a1 = allocated();
            System.out.printf("%d wrapping exception(s) + root IOException: %7.0f bytes in total (%d objects with a stack trace)%n", chain, (a1 - a0) / 20000.0, chain + 1);
        }
        if (sink == null) System.out.println();
    }
}
