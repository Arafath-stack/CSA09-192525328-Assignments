import java.io.*;
import java.util.*;

/** Q2: ArrayList vs LinkedList vs ArrayDeque (vs manual array) for playlist operations. */
public class ListBench {
    record Song(int id, String title) { }
    static final int REPS = 5;
    static PrintWriter csv;
    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }
    static double sd(double[] a) { double m = mean(a), s = 0; for (double x : a) s += (x - m) * (x - m); return Math.sqrt(s / a.length); }
    static long sink;

    /** plain array with manual shifting */
    static class ManualPlaylist {
        Song[] a; int size;
        ManualPlaylist(int cap) { a = new Song[cap]; }
        void add(Song s) { a[size++] = s; }
        void insert(int i, Song s) { for (int k = size; k > i; k--) a[k] = a[k - 1]; a[i] = s; size++; }
        void remove(int i) { for (int k = i; k < size - 1; k++) a[k] = a[k + 1]; a[--size] = null; }
        Song get(int i) { return a[i]; }
    }

    interface Op { void run(); }
    static void measure(String struct, String op, int n, int m, Op prepare, Op body) {
        double[] ms = new double[REPS];
        for (int r = -1; r < REPS; r++) {                   // r = -1 is a warm-up
            prepare.run(); long t0 = System.nanoTime(); body.run(); long t1 = System.nanoTime();
            if (r >= 0) { ms[r] = (t1 - t0) / 1e6; csv.printf("%s,%s,%d,%d,%d,%.4f%n", struct, op, n, m, r, ms[r]); }
        }
        System.out.printf("%-12s %-22s n=%-7d ops=%-6d %10.3f ms (sd %.3f)%n", struct, op, n, m, mean(ms), sd(ms));
    }

    public static void main(String[] args) throws Exception {
        csv = new PrintWriter("data/q2_raw_times.csv"); csv.println("structure,operation,n,ops,rep,ms");
        Random rnd = new Random(7);
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            int m = 1_000;                                    // number of positional operations per measurement
            Song[] songs = new Song[n + m + 10]; for (int i = 0; i < songs.length; i++) songs[i] = new Song(i, "t" + i);
            int[] idx = new int[m]; for (int i = 0; i < m; i++) idx[i] = rnd.nextInt(n);
            System.out.println("=============== playlist size n = " + n);
            ArrayList<Song>[] al = new ArrayList[1]; LinkedList<Song>[] ll = new LinkedList[1]; ArrayDeque<Song>[] ad = new ArrayDeque[1]; ManualPlaylist[] mp = new ManualPlaylist[1];
            Op fillAL = () -> { al[0] = new ArrayList<>(); for (int i = 0; i < n; i++) al[0].add(songs[i]); };
            Op fillLL = () -> { ll[0] = new LinkedList<>(); for (int i = 0; i < n; i++) ll[0].add(songs[i]); };
            Op fillAD = () -> { ad[0] = new ArrayDeque<>(); for (int i = 0; i < n; i++) ad[0].addLast(songs[i]); };
            Op fillMP = () -> { mp[0] = new ManualPlaylist(n + m + 10); for (int i = 0; i < n; i++) mp[0].add(songs[i]); };
            Op none = () -> { };
            // add at end (n adds)
            measure("ArrayList", "add at end (n adds)", n, n, () -> al[0] = new ArrayList<>(), () -> { for (int i = 0; i < n; i++) al[0].add(songs[i]); });
            measure("LinkedList", "add at end (n adds)", n, n, () -> ll[0] = new LinkedList<>(), () -> { for (int i = 0; i < n; i++) ll[0].add(songs[i]); });
            measure("ArrayDeque", "add at end (n adds)", n, n, () -> ad[0] = new ArrayDeque<>(), () -> { for (int i = 0; i < n; i++) ad[0].addLast(songs[i]); });
            measure("ManualArray", "add at end (n adds)", n, n, () -> mp[0] = new ManualPlaylist(n + 10), () -> { for (int i = 0; i < n; i++) mp[0].add(songs[i]); });
            // insert at front (m ops)
            measure("ArrayList", "insert at front", n, m, fillAL, () -> { for (int i = 0; i < m; i++) al[0].add(0, songs[n + i]); });
            measure("LinkedList", "insert at front", n, m, fillLL, () -> { for (int i = 0; i < m; i++) ll[0].addFirst(songs[n + i]); });
            measure("ArrayDeque", "insert at front", n, m, fillAD, () -> { for (int i = 0; i < m; i++) ad[0].addFirst(songs[n + i]); });
            measure("ManualArray", "insert at front", n, m, fillMP, () -> { for (int i = 0; i < m; i++) mp[0].insert(0, songs[n + i]); });
            // insert in middle
            measure("ArrayList", "insert in middle", n, m, fillAL, () -> { for (int i = 0; i < m; i++) al[0].add(al[0].size() / 2, songs[n + i]); });
            measure("LinkedList", "insert in middle", n, m, fillLL, () -> { for (int i = 0; i < m; i++) ll[0].add(ll[0].size() / 2, songs[n + i]); });
            measure("ManualArray", "insert in middle", n, m, fillMP, () -> { for (int i = 0; i < m; i++) mp[0].insert(mp[0].size / 2, songs[n + i]); });
            // remove at front
            measure("ArrayList", "remove at front", n, m, fillAL, () -> { for (int i = 0; i < m; i++) al[0].remove(0); });
            measure("LinkedList", "remove at front", n, m, fillLL, () -> { for (int i = 0; i < m; i++) ll[0].removeFirst(); });
            measure("ArrayDeque", "remove at front", n, m, fillAD, () -> { for (int i = 0; i < m; i++) ad[0].removeFirst(); });
            measure("ManualArray", "remove at front", n, m, fillMP, () -> { for (int i = 0; i < m; i++) mp[0].remove(0); });
            // remove in middle
            measure("ArrayList", "remove in middle", n, m, fillAL, () -> { for (int i = 0; i < m; i++) al[0].remove(al[0].size() / 2); });
            measure("LinkedList", "remove in middle", n, m, fillLL, () -> { for (int i = 0; i < m; i++) ll[0].remove(ll[0].size() / 2); });
            // get k-th
            measure("ArrayList", "get k-th (random k)", n, m, fillAL, () -> { long s = 0; for (int i = 0; i < m; i++) s += al[0].get(idx[i]).id(); sink += s; });
            measure("LinkedList", "get k-th (random k)", n, m, fillLL, () -> { long s = 0; for (int i = 0; i < m; i++) s += ll[0].get(idx[i]).id(); sink += s; });
            measure("ManualArray", "get k-th (random k)", n, m, fillMP, () -> { long s = 0; for (int i = 0; i < m; i++) s += mp[0].get(idx[i]).id(); sink += s; });
            // iterate whole list (10 passes)
            measure("ArrayList", "iterate (10 passes)", n, 10, fillAL, () -> { long s = 0; for (int p = 0; p < 10; p++) for (Song x : al[0]) s += x.id(); sink += s; });
            measure("LinkedList", "iterate (10 passes)", n, 10, fillLL, () -> { long s = 0; for (int p = 0; p < 10; p++) for (Song x : ll[0]) s += x.id(); sink += s; });
            measure("ArrayDeque", "iterate (10 passes)", n, 10, fillAD, () -> { long s = 0; for (int p = 0; p < 10; p++) for (Song x : ad[0]) s += x.id(); sink += s; });
        }
        // ---- Changed condition: "Up Next" queue: add at both ends, remove from the front
        System.out.println("\n=============== Up Next queue: 20,000 operations (1/3 addFirst, 1/3 addLast, 1/3 removeFirst)");
        int OPS = 20_000;
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            Song[] songs = new Song[n + OPS + 10]; for (int i = 0; i < songs.length; i++) songs[i] = new Song(i, "t" + i);
            ArrayList<Song>[] al = new ArrayList[1]; LinkedList<Song>[] ll = new LinkedList[1]; ArrayDeque<Song>[] ad = new ArrayDeque[1];
            measure("ArrayList", "UpNext 20000 ops", n, OPS, () -> { al[0] = new ArrayList<>(); for (int i = 0; i < n; i++) al[0].add(songs[i]); },
                () -> { for (int i = 0; i < OPS; i++) { int k = i % 3; if (k == 0) al[0].add(0, songs[n + i]); else if (k == 1) al[0].add(songs[n + i]); else al[0].remove(0); } });
            measure("LinkedList", "UpNext 20000 ops", n, OPS, () -> { ll[0] = new LinkedList<>(); for (int i = 0; i < n; i++) ll[0].add(songs[i]); },
                () -> { for (int i = 0; i < OPS; i++) { int k = i % 3; if (k == 0) ll[0].addFirst(songs[n + i]); else if (k == 1) ll[0].addLast(songs[n + i]); else ll[0].removeFirst(); } });
            measure("ArrayDeque", "UpNext 20000 ops", n, OPS, () -> { ad[0] = new ArrayDeque<>(); for (int i = 0; i < n; i++) ad[0].addLast(songs[i]); },
                () -> { for (int i = 0; i < OPS; i++) { int k = i % 3; if (k == 0) ad[0].addFirst(songs[n + i]); else if (k == 1) ad[0].addLast(songs[n + i]); else ad[0].removeFirst(); } });
        }
        csv.close(); System.out.println("(checksum " + sink + ")");
    }
}
