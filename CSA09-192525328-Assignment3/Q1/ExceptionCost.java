/** Time and space of creating and throwing an exception versus returning an error code. */
public class ExceptionCost {
    static class Plain extends Exception { Plain(String m) { super(m); } }
    static class NoTrace extends Exception { NoTrace(String m) { super(m, null, false, false); } }   // no stack trace captured
    static long allocated() { return ((com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean()).getThreadAllocatedBytes(Thread.currentThread().threadId()); }

    static int viaCode(int depth, int x) { return depth == 0 ? (x < 0 ? -1 : x) : viaCode(depth - 1, x); }
    static int viaException(int depth, int x) throws Plain { if (depth == 0) { if (x < 0) throw new Plain("negative"); return x; } return viaException(depth - 1, x); }
    static int viaNoTrace(int depth, int x) throws NoTrace { if (depth == 0) { if (x < 0) throw new NoTrace("negative"); return x; } return viaNoTrace(depth - 1, x); }

    public static void main(String[] args) throws Exception {
        int N = 300_000; long sink = 0;
        for (int depth : new int[]{5, 50}) {
            for (int warm = 0; warm < 3; warm++) {
                long t0 = System.nanoTime(); for (int i = 0; i < N; i++) sink += viaCode(depth, -1);
                long t1 = System.nanoTime(); for (int i = 0; i < N; i++) { try { sink += viaException(depth, -1); } catch (Plain e) { sink--; } }
                long t2 = System.nanoTime(); for (int i = 0; i < N; i++) { try { sink += viaNoTrace(depth, -1); } catch (NoTrace e) { sink--; } }
                long t3 = System.nanoTime();
                if (warm == 2) System.out.printf("call depth %2d: error code %7.1f ns | exception with stack trace %9.1f ns | exception without stack trace %7.1f ns  (per failed call)%n",
                        depth, (t1 - t0) / (double) N, (t2 - t1) / (double) N, (t3 - t2) / (double) N);
            }
        }
        System.out.println();
        for (int depth : new int[]{5, 50}) {
            long a0 = allocated(); for (int i = 0; i < 1000; i++) { try { viaException(depth, -1); } catch (Plain e) { sink++; } }
            long a1 = allocated(); for (int i = 0; i < 1000; i++) { try { viaNoTrace(depth, -1); } catch (NoTrace e) { sink++; } }
            long a2 = allocated(); for (int i = 0; i < 1000; i++) sink += viaCode(depth, -1);
            long a3 = allocated();
            System.out.printf("call depth %2d: bytes allocated per failure: exception %6.1f, exception without trace %5.1f, error code %4.1f%n", depth, (a1 - a0) / 1000.0, (a2 - a1) / 1000.0, (a3 - a2) / 1000.0);
        }
        Plain p = new Plain("x"); long b0 = allocated(); StackTraceElement[] st = p.getStackTrace(); long b1 = allocated();
        System.out.println("getStackTrace() materialises " + st.length + " StackTraceElement objects (" + (b1 - b0) + " more bytes) only when the trace is read");
        if (sink == 7) System.out.println();
    }
}
