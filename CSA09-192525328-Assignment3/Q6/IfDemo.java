/** Deterministic proof that 'if' instead of 'while' is wrong: two consumers wait, ONE token arrives, notifyAll wakes BOTH. */
public class IfDemo {
    final int[] buf = new int[5]; int count = 0, out = 0;
    synchronized int takeIf() throws InterruptedException {
        if (count == 0) wait();                       // 'if': no re-check after waking
        int t = buf[out]; out = (out + 1) % 5; count--;
        return t;
    }
    synchronized void put(int t) { buf[0] = t; count++; notifyAll(); }
    public static void main(String[] args) throws Exception {
        final IfDemo d = new IfDemo(); int[] got = new int[2];
        Thread c1 = new Thread(() -> { try { got[0] = d.takeIf(); } catch (InterruptedException e) { } }, "student-1");
        Thread c2 = new Thread(() -> { try { got[1] = d.takeIf(); } catch (InterruptedException e) { } }, "student-2");
        c1.start(); c2.start(); Thread.sleep(200);    // both students are now waiting in wait()
        d.put(42);                                       // ONE token, notifyAll() wakes both
        c1.join(1000); c2.join(1000);
        System.out.println("student-1 got " + got[0] + ", student-2 got " + got[1] + ", count afterwards = " + d.count);
        System.out.println(d.count < 0 ? "count is NEGATIVE: one student 'took' a token that never existed (stale data, here " + (got[0] == 42 ? got[1] : got[0]) + ")" : "no anomaly this time");
    }
}
