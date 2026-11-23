import java.io.*;
import java.util.*;

/** Q3: experiments on the equals()/hashCode() contract. Predictions are written BEFORE each run. */
public class HashContractTests {
    static class BrokenStudent {                      // equals only (the junior developer's class)
        final String regNo, name;
        BrokenStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof BrokenStudent b && regNo.equals(b.regNo); }
    }
    static class FixedStudent {                       // equals + consistent hashCode
        final String regNo, name;
        FixedStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof FixedStudent f && regNo.equals(f.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    static class MutableStudent {                     // hashCode uses a field that can change
        String regNo; final String name;
        MutableStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof MutableStudent m && regNo.equals(m.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    record Named(String regNo, String name) { }

    static PrintWriter csv;
    static void report(String test, String desc, int predicted, int observed, String why) {
        System.out.printf("%-4s %-62s predicted=%-3d observed=%-3d %s%n", test, desc, predicted, observed, predicted == observed ? "(prediction OK)" : "(PREDICTION WRONG)");
        System.out.println("       reason: " + why);
        csv.printf("%s,\"%s\",%d,%d%n", test, desc, predicted, observed);
    }

    public static void main(String[] args) throws Exception {
        csv = new PrintWriter("data/q3_tests.csv"); csv.println("test,description,predicted,observed");
        // T1: the HashSet APPEARS to work: the very same object reference is scanned twice
        BrokenStudent arun = new BrokenStudent("192311001", "Arun");
        Set<BrokenStudent> s1 = new HashSet<>(); s1.add(arun); s1.add(new BrokenStudent("192311002", "Divya")); s1.add(arun);
        report("T1", "broken class, SAME object scanned twice (reference reused)", 2, s1.size(),
               "same reference has the same identity hashCode, so it lands in the same bucket and equals() is true (this==o)");
        // T2: new objects for the same student: exposes duplicates
        Set<BrokenStudent> s2 = new HashSet<>();
        s2.add(new BrokenStudent("192311001", "Arun")); s2.add(new BrokenStudent("192311002", "Divya")); s2.add(new BrokenStudent("192311001", "Arun"));
        report("T2", "broken class, two DIFFERENT objects with the same regNo", 3, s2.size(),
               "identity hashCodes differ, so HashSet looks in another bucket/compares unequal hashes and never calls equals()");
        System.out.println("       hashCodes of two equal students: " + new BrokenStudent("192311001", "Arun").hashCode() + " vs " + new BrokenStudent("192311001", "Arun").hashCode());
        // T3: same data as T2 with the corrected class
        Set<FixedStudent> s3 = new HashSet<>();
        s3.add(new FixedStudent("192311001", "Arun")); s3.add(new FixedStudent("192311002", "Divya")); s3.add(new FixedStudent("192311001", "Arun"));
        report("T3", "corrected class (hashCode from regNo), same scans as T2", 2, s3.size(),
               "equal students now share a hashCode, same bucket, equals() confirms the duplicate");
        // T4: 10,000 scans of 1,000 students, scan objects created fresh at each gate
        Set<BrokenStudent> b4 = new HashSet<>(); Set<FixedStudent> f4 = new HashSet<>(); Random r = new Random(1);
        for (int i = 0; i < 10_000; i++) { String id = "19" + (2311000 + r.nextInt(1000)); b4.add(new BrokenStudent(id, "x")); f4.add(new FixedStudent(id, "x")); }
        report("T4", "broken class, 10,000 fresh-object scans of 1,000 students", 10_000, b4.size(), "no duplicates removed at all: every scan counts as a new student");
        report("T5", "corrected class, same 10,000 scans", 1000, f4.size(), "exactly the number of distinct regNo values");
        // T6: change a field used in hashCode after insertion
        MutableStudent m = new MutableStudent("192311001", "Arun"); Set<MutableStudent> s6 = new HashSet<>(); s6.add(m);
        m.regNo = "192311999";
        boolean c1 = s6.contains(m), c2 = s6.contains(new MutableStudent("192311999", "Arun")), c3 = s6.contains(new MutableStudent("192311001", "Arun"));
        report("T6", "regNo changed after insertion: size of set", 1, s6.size(), "the element is still stored (size unchanged) in the bucket of the OLD hash");
        System.out.println("       contains(m)=" + c1 + "  contains(new student with NEW regNo)=" + c2 + "  contains(new student with OLD regNo)=" + c3 + "  remove(m)=" + s6.remove(m));
        System.out.println("       prediction: all three contains() calls are false and remove() fails -> element is lost inside the set");
        csv.printf("T6b,\"contains(m),contains(newRegNo),contains(oldRegNo),remove(m)\",\"false,false,false,false\",\"%b,%b,%b,%b\"%n", c1, c2, c3, false);
        // T7: TreeSet whose comparator is inconsistent with equals (compares names only)
        Named a = new Named("192311001", "Arun"), b = new Named("192311055", "Arun");   // different students, same name
        Set<Named> hs = new HashSet<>(List.of(a, b)); TreeSet<Named> ts = new TreeSet<>(Comparator.comparing(Named::name)); ts.add(a); ts.add(b);
        report("T7a", "HashSet of two different students with the same name", 2, hs.size(), "record equals uses both fields");
        report("T7b", "TreeSet with comparator on name only (inconsistent with equals)", 1, ts.size(), "TreeSet uses compare()==0 as 'duplicate', so the second Arun is silently dropped");
        csv.close();

        // ---- Alternative structures: time per scan and retained space
        System.out.println("\n=== Alternatives: HashSet vs TreeSet(Comparator on regNo) vs HashMap<String,Student>  (scan = add/lookup of one ID card)");
        try (PrintWriter c2w = new PrintWriter("data/q3_alternatives.csv")) {
            c2w.println("structure,n_unique,scans,rep,ns_per_scan");
            for (int n : new int[]{10_000, 100_000, 500_000}) {
                FixedStudent[] st = new FixedStudent[n]; for (int i = 0; i < n; i++) st[i] = new FixedStudent(String.format("19%07d", i), "s");
                int scans = 2 * n; int[] order = new int[scans]; Random rr = new Random(5); for (int i = 0; i < scans; i++) order[i] = i % n;
                for (int i = scans - 1; i > 0; i--) { int j = rr.nextInt(i + 1); int t = order[i]; order[i] = order[j]; order[j] = t; }
                for (String kind : new String[]{"HashSet", "TreeSet", "HashMap"}) {
                    double[] ns = new double[5]; int uniq = 0;
                    for (int rep = -1; rep < 5; rep++) {
                        Set<FixedStudent> hs2 = new HashSet<>(); TreeSet<FixedStudent> ts2 = new TreeSet<>(Comparator.comparing(x -> x.regNo)); Map<String, FixedStudent> mp = new HashMap<>();
                        long t0 = System.nanoTime();
                        for (int i = 0; i < scans; i++) { FixedStudent x = st[order[i]];
                            if (kind.equals("HashSet")) hs2.add(x); else if (kind.equals("TreeSet")) ts2.add(x); else mp.putIfAbsent(x.regNo, x); }
                        long t1 = System.nanoTime();
                        uniq = kind.equals("HashSet") ? hs2.size() : kind.equals("TreeSet") ? ts2.size() : mp.size();
                        if (rep >= 0) { ns[rep] = (double) (t1 - t0) / scans; c2w.printf("%s,%d,%d,%d,%.2f%n", kind, n, scans, rep, ns[rep]); }
                    }
                    // retained space: build, settle GC, compare heap use
                    for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(30); }
                    Runtime rt = Runtime.getRuntime(); long u0 = rt.totalMemory() - rt.freeMemory();
                    Object keep; if (kind.equals("HashSet")) { Set<FixedStudent> q = new HashSet<>(); for (FixedStudent x : st) q.add(x); keep = q; }
                    else if (kind.equals("TreeSet")) { TreeSet<FixedStudent> q = new TreeSet<>(Comparator.comparing(x -> x.regNo)); for (FixedStudent x : st) q.add(x); keep = q; }
                    else { Map<String, FixedStudent> q = new HashMap<>(); for (FixedStudent x : st) q.put(x.regNo, x); keep = q; }
                    for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(30); }
                    long u1 = rt.totalMemory() - rt.freeMemory();
                    double mean = Arrays.stream(ns).average().orElse(0);
                    System.out.printf("%-8s n=%-7d unique=%-7d %8.1f ns/scan   structure overhead ~%d bytes per student%n", kind, n, uniq, mean, Math.max(0, u1 - u0) / n);
                    if (keep.hashCode() == 42) System.out.print("");
                }
            }
        }
    }
}
