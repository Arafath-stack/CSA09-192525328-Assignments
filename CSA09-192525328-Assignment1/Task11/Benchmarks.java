import java.util.*;

/** Measurements used in the "Alternative Concept and Complexity Analysis" sections (Tasks 1, 3, 4, 7, 11). */
public class Benchmarks {
    static long sink;   // prevents the JIT from removing the work

    // ---- Task 1: parallel arrays vs objects (search by registration number)
    static class Rec { String regNo; String type; long entry; Rec(String r, String t, long e) { regNo = r; type = t; entry = e; } }
    static void task1(boolean show) {
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            String[] reg = new String[n], type = new String[n]; long[] entry = new long[n]; Rec[] objs = new Rec[n];
            for (int i = 0; i < n; i++) { reg[i] = "TN" + i; type[i] = "CAR"; entry[i] = i; objs[i] = new Rec(reg[i], type[i], i); }
            String target = "TN" + (n - 1); int reps = 200;
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (int i = 0; i < n; i++) if (reg[i].equals(target)) { sink += i; break; }
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (int i = 0; i < n; i++) if (objs[i].regNo.equals(target)) { sink += i; break; }
            long t2 = System.nanoTime();
            if (show) System.out.printf("Task1  n=%-7d parallel arrays %8.3f ms/search | object array %8.3f ms/search%n", n, (t1 - t0) / 1e6 / reps, (t2 - t1) / 1e6 / reps);
        }
    }

    // ---- Task 3: scan vs free-slot stack, worst case (only the LAST slot is free)
    static void task3() {
        for (int[] fs : new int[][]{{5, 100}, {50, 1000}}) {
            int F = fs[0], S = fs[1]; String[][] slots = new String[F][S];
            for (String[] row : slots) Arrays.fill(row, "X"); slots[F - 1][S - 1] = null;
            int reps = 2000; long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) {
                outer: for (int f = 0; f < F; f++) for (int s = 0; s < S; s++) if (slots[f][s] == null) { slots[f][s] = "Y"; sink += f; break outer; }
                slots[F - 1][S - 1] = null;
            }
            long t1 = System.nanoTime();
            ArrayDeque<int[]> stack = new ArrayDeque<>(); stack.push(new int[]{F - 1, S - 1});
            for (int r = 0; r < reps; r++) { int[] p = stack.pop(); sink += p[0]; stack.push(p); }
            long t2 = System.nanoTime();
            System.out.printf("Task3  F=%-3d S=%-5d scan allocate+release %9.4f ms | stack allocate+release %9.4f ms%n", F, S, (t1 - t0) / 1e6 / reps, (t2 - t1) / 1e6 / reps);
        }
    }

    // ---- Task 4: creating n tickets three ways
    static class T1 { final int no; final String reg; T1(int n, String r) { no = n; reg = r; } }
    static class T2 { int no; String reg; T2() { } void setNo(int n) { if (n < 0) throw new IllegalArgumentException(); no = n; } void setReg(String r) { if (r == null) throw new IllegalArgumentException(); reg = r; } }
    static class T3 { final int no; final String reg; private T3(int n, String r) { no = n; reg = r; } static T3 of(int n, String r) { return new T3(n, r); } }
    static void task4() {
        int n = 1_000_000; String r = "TN01AB1111"; Object[] keep = new Object[n];
        double b1 = 1e18, b2 = 1e18, b3 = 1e18;
        for (int round = 0; round < 7; round++) {
            long t0 = System.nanoTime(); for (int i = 0; i < n; i++) keep[i] = new T1(i, r);
            long t1 = System.nanoTime(); for (int i = 0; i < n; i++) { T2 t = new T2(); t.setNo(i); t.setReg(r); keep[i] = t; }
            long t2 = System.nanoTime(); for (int i = 0; i < n; i++) keep[i] = T3.of(i, r);
            long t3 = System.nanoTime();
            b1 = Math.min(b1, (t1 - t0) / 1e6); b2 = Math.min(b2, (t2 - t1) / 1e6); b3 = Math.min(b3, (t3 - t2) / 1e6);
        }
        sink += keep.length;
        System.out.printf("Task4  n=%d (best of 7)  constructor %.1f ms | default+setters %.1f ms | static factory %.1f ms%n", n, b1, b2, b3);
    }

    // ---- Task 7: overriding vs switch on a type field (k = 3 types)
    static abstract class V { abstract double fee(int h); }
    static class A extends V { double fee(int h) { return h * 10.0; } }
    static class B extends V { double fee(int h) { return h * 30.0; } }
    static class C extends V { double fee(int h) { return h * 30.0; } }
    static double sw(int type, int h) { switch (type) { case 0: return h * 10.0; case 1: return h * 30.0; default: return h * 30.0; } }
    static void task7() {
        V[] vs = {new A(), new B(), new C()}; int n = 30_000_000; double sum = 0;
        for (int warm = 0; warm < 2; warm++) {
            long t0 = System.nanoTime(); for (int i = 0; i < n; i++) sum += vs[i % 3].fee(3);
            long t1 = System.nanoTime(); for (int i = 0; i < n; i++) sum += sw(i % 3, 3);
            long t2 = System.nanoTime();
            if (warm == 1) System.out.printf("Task7  %d fee calls  dynamic dispatch %.1f ms | switch on type %.1f ms%n", n, (t1 - t0) / 1e6, (t2 - t1) / 1e6);
        }
        sink += (long) sum;
    }

    // ---- Task 11: array vs ArrayList vs HashMap, find ticket on exit (worst case = last inserted)
    static class Tk { final String reg; Tk(String r) { reg = r; } }
    static void task11(boolean show) {
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            Tk[] arr = new Tk[n]; ArrayList<Tk> list = new ArrayList<>(); HashMap<String, Tk> map = new HashMap<>();
            for (int i = 0; i < n; i++) { Tk t = new Tk("TN" + i); arr[i] = t; list.add(t); map.put(t.reg, t); }
            String key = "TN" + (n - 1); int reps = 300;
            long t0 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (int i = 0; i < n; i++) if (arr[i].reg.equals(key)) { sink += i; break; }
            long t1 = System.nanoTime();
            for (int r = 0; r < reps; r++) for (int i = 0; i < list.size(); i++) if (list.get(i).reg.equals(key)) { sink += i; break; }
            long t2 = System.nanoTime();
            for (int r = 0; r < reps; r++) sink += map.get(key).reg.length();
            long t3 = System.nanoTime();
            if (show) System.out.printf("Task11 n=%-7d array %9.4f ms | ArrayList %9.4f ms | HashMap %9.6f ms (per lookup)%n", n, (t1 - t0) / 1e6 / reps, (t2 - t1) / 1e6 / reps, (t3 - t2) / 1e6 / reps);
        }
    }

    public static void main(String[] args) {
        task1(false); task1(true); task3(); task4(); task7(); task11(false); task11(true);
        System.out.println("(checksum " + sink + ")");
    }
}
