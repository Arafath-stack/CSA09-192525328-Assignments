import java.io.*;
import java.util.*;

/** Q6: own design of an emergency-triage queue: correctness check, growth, starvation, fairness, alternatives. */
public class TriageDesign {
    record Case(String name, int severity, long seq, long arrivalMinute) { }

    /** Strict rule: lowest severity number first, then earliest arrival (seq = arrival order). */
    static final Comparator<Case> STRICT = Comparator.comparingInt(Case::severity).thenComparingLong(Case::seq);
    static final int[] ALLOWED_WAIT = {0, 0, 15, 30, 60, 120};      // minutes a patient of severity s may wait before counting as "due" (index = severity)
    /** Fair rule: serve the earliest DEADLINE (arrival + allowed wait); the key never changes, so the heap stays valid. */
    static final Comparator<Case> FAIR = Comparator.comparingLong((Case c) -> c.arrivalMinute() + ALLOWED_WAIT[c.severity()]).thenComparingInt(Case::severity).thenComparingLong(Case::seq);

    /** Hybrid: critical patients (S1) always first; everyone else is served by deadline, so S2-S5 cannot starve. */
    static final Comparator<Case> HYBRID = Comparator.comparingInt((Case c) -> c.severity() == 1 ? 0 : 1).thenComparing(FAIR);

    static class TriageQueue {
        private final PriorityQueue<Case> pq; private long seq;
        TriageQueue(Comparator<Case> rule) { pq = new PriorityQueue<>(rule); }
        void admit(String name, int severity, long minute) { pq.add(new Case(name, severity, seq++, minute)); }
        Case serveNext() { return pq.poll(); }
        int waiting() { return pq.size(); }
        List<Case> waitingInTreatmentOrder(Comparator<Case> rule) { List<Case> l = new ArrayList<>(pq); l.sort(rule); return l; }   // display = sorted COPY
        String rawQueueString() { return pq.toString(); }
    }

    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static long sink;

    public static void main(String[] args) throws Exception {
        // ---------- 1. hand-checkable instance (8 patients)
        String[][] arrivals = {{"Ravi", "3"}, {"Meena", "1"}, {"John", "3"}, {"Asha", "5"}, {"Kumar", "1"}, {"Lata", "2"}, {"Divya", "2"}, {"Arun", "4"}};
        String[] byHand = {"Meena", "Kumar", "Lata", "Divya", "Ravi", "John", "Arun", "Asha"};
        TriageQueue q = new TriageQueue(STRICT);
        for (int i = 0; i < arrivals.length; i++) q.admit(arrivals[i][0], Integer.parseInt(arrivals[i][1]), i);
        System.out.println("Arrival order : Ravi(3) Meena(1) John(3) Asha(5) Kumar(1) Lata(2) Divya(2) Arun(4)");
        System.out.println("Hand-worked treatment order : " + String.join(" ", byHand));
        System.out.println("PriorityQueue printed directly (heap array, NOT treatment order): " + q.rawQueueString());
        System.out.println("Waiting list, sorted copy for display : " + q.waitingInTreatmentOrder(STRICT));
        List<String> served = new ArrayList<>(); Case c; while ((c = q.serveNext()) != null) served.add(c.name());
        System.out.println("Program treatment order     : " + String.join(" ", served) + "   matches hand result: " + served.equals(Arrays.asList(byHand)));

        // ---------- 2. growth: cost of admit and serveNext, and alternatives
        System.out.println("\n=== Growth: n admits then n serves (random severity 1-5). Mean of 5 runs, microseconds per operation");
        try (PrintWriter csv = new PrintWriter("data/q6_growth.csv")) {
            csv.println("design,n,rep,admit_ns_per_op,serve_ns_per_op");
            for (int n : new int[]{1_000, 10_000, 40_000, 100_000, 1_000_000}) {
                Random rr = new Random(5); int[] sev = new int[n]; for (int i = 0; i < n; i++) sev[i] = 1 + rr.nextInt(5);
                for (String design : new String[]{"PriorityQueue", "Sorted LinkedList", "5 FIFO queues"}) {
                    if (design.equals("Sorted LinkedList") && n > 40_000) continue;     // O(n) insertion makes larger sizes impractical
                    double[] ad = new double[5], sv = new double[5];
                    for (int rep = -1; rep < 5; rep++) {
                        long t0, t1, t2;
                        if (design.equals("PriorityQueue")) {
                            PriorityQueue<Case> pq = new PriorityQueue<>(STRICT); t0 = System.nanoTime();
                            for (int i = 0; i < n; i++) pq.add(new Case("p", sev[i], i, i)); t1 = System.nanoTime();
                            while (!pq.isEmpty()) sink += pq.poll().seq(); t2 = System.nanoTime();
                        } else if (design.equals("Sorted LinkedList")) {
                            LinkedList<Case> ll = new LinkedList<>(); t0 = System.nanoTime();
                            for (int i = 0; i < n; i++) { Case x = new Case("p", sev[i], i, i); ListIterator<Case> it = ll.listIterator();
                                while (it.hasNext()) { if (STRICT.compare(it.next(), x) > 0) { it.previous(); break; } } it.add(x); } t1 = System.nanoTime();
                            while (!ll.isEmpty()) sink += ll.removeFirst().seq(); t2 = System.nanoTime();
                        } else {
                            ArrayDeque<Case>[] fifo = new ArrayDeque[6]; for (int s = 1; s <= 5; s++) fifo[s] = new ArrayDeque<>(); t0 = System.nanoTime();
                            for (int i = 0; i < n; i++) fifo[sev[i]].addLast(new Case("p", sev[i], i, i)); t1 = System.nanoTime();
                            int left = n; while (left > 0) { for (int s = 1; s <= 5; s++) if (!fifo[s].isEmpty()) { sink += fifo[s].pollFirst().seq(); left--; break; } } t2 = System.nanoTime();
                        }
                        if (rep >= 0) { ad[rep] = (double) (t1 - t0) / n; sv[rep] = (double) (t2 - t1) / n; csv.printf("%s,%d,%d,%.1f,%.1f%n", design, n, rep, ad[rep], sv[rep]); }
                    }
                    System.out.printf("  %-18s n=%-8d admit %10.3f us/op   serve %10.3f us/op%n", design, n, mean(ad) / 1000, mean(sv) / 1000);
                }
            }
        }

        // ---------- 3. starvation and a fair rule (simulation, 1 minute per step)
        System.out.println("\n=== Starvation simulation: 2,400 minutes, 1.1 arrivals per minute (mostly severe), one doctor serves 1 patient per minute");
        for (String rule : new String[]{"STRICT priority", "FAIR (earliest deadline for all)", "HYBRID (S1 always first, deadline for S2-S5)"}) {
            TriageQueue tq = new TriageQueue(rule.startsWith("STRICT") ? STRICT : rule.startsWith("FAIR") ? FAIR : HYBRID); Random rr = new Random(42);
            int[] servedBySev = new int[6]; long[] waitSum = new long[6]; long[] maxWait = new long[6]; double carry = 0;
            for (long minute = 0; minute < 2400; minute++) {
                carry += 1.1; while (carry >= 1) { carry -= 1; int u = rr.nextInt(100); int s = u < 15 ? 1 : u < 40 ? 2 : u < 65 ? 3 : u < 85 ? 4 : 5; tq.admit("p", s, minute); }
                Case x = tq.serveNext(); if (x != null) { long w = minute - x.arrivalMinute(); servedBySev[x.severity()]++; waitSum[x.severity()] += w; maxWait[x.severity()] = Math.max(maxWait[x.severity()], w); }
            }
            long[] stillWaiting = new long[6]; long[] oldest = new long[6];
            for (Case x : tq.waitingInTreatmentOrder(STRICT)) { stillWaiting[x.severity()]++; oldest[x.severity()] = Math.max(oldest[x.severity()], 2400 - x.arrivalMinute()); }
            System.out.println("  Rule: " + rule);
            for (int s = 1; s <= 5; s++) System.out.printf("    S%d: served %4d  mean wait %7.1f min  max wait %5d min | still waiting at the end %4d (oldest has waited %d min)%n", s, servedBySev[s], servedBySev[s] == 0 ? 0.0 : (double) waitSum[s] / servedBySev[s], maxWait[s], stillWaiting[s], oldest[s]);
        }
        System.out.println("\n  (checksum " + sink + ")");
    }
}
