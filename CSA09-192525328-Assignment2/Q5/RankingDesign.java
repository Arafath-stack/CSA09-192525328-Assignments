import java.io.*;
import java.util.*;
import java.util.function.Function;

/** Q5: own design - generic Leaderboard with bounded types / wildcards, tests, faulty comparators, alternatives. */
public class RankingDesign {
    // ---------- reusable generic component ----------
    /** Anything that can be ranked is described by a Comparator, not baked into the class. */
    static class Board<T> {
        private final List<T> items = new ArrayList<>(); private final Comparator<? super T> order;
        Board(Comparator<? super T> order) { this.order = Objects.requireNonNull(order); }
        void add(T t) { items.add(t); }
        void addAll(Collection<? extends T> more) { items.addAll(more); }           // wildcard: accepts List<SubTeam>, etc.
        List<T> ranking() { List<T> c = new ArrayList<>(items); c.sort(order); return c; }
        List<T> top(int k) { List<T> r = ranking(); return r.subList(0, Math.min(k, r.size())); }
    }
    /** Bounded type: works for any type that has a natural order, including subclasses of a Comparable superclass. */
    static <T extends Comparable<? super T>> T best(Collection<? extends T> c) { return Collections.min(c); }
    /** Reusable factory: turn "score high first" + "tie-breaker" into one consistent comparator for ANY type. */
    static <T> Comparator<T> highFirst(java.util.function.ToIntFunction<? super T> key) { return (a, b) -> Integer.compare(key.applyAsInt(b), key.applyAsInt(a)); }

    record Team(String name, int points, int goalDiff) { }
    record Player(String name, int goals) { }
    // Comparable alternative: one fixed natural order built into the class
    record TeamC(String name, int points, int goalDiff) implements Comparable<TeamC> {
        public int compareTo(TeamC o) {
            int c = Integer.compare(o.points, points); if (c != 0) return c;
            c = Integer.compare(o.goalDiff, goalDiff); if (c != 0) return c;
            return name.compareTo(o.name); }
    }
    static final Comparator<Team> CORRECT = highFirst((Team t) -> t.points()).thenComparing(highFirst((Team t) -> t.goalDiff())).thenComparing(Team::name);
    static final Comparator<Team> SUBTRACT = (a, b) -> b.points() - a.points();                                  // faulty: overflow
    static final Comparator<Team> NO_NAME = highFirst((Team t) -> t.points()).thenComparing(highFirst((Team t) -> t.goalDiff()));   // faulty: missing tie-breaker
    static final Comparator<Team> GD_ASC = highFirst((Team t) -> t.points()).thenComparingInt(Team::goalDiff).thenComparing(Team::name);   // faulty: wrong direction

    static int transitivityViolations(List<Team> teams, Comparator<Team> c) {
        int bad = 0; int n = teams.size();
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) { int ab = Integer.signum(c.compare(teams.get(i), teams.get(j)));
            if (ab >= 0 && i != j) continue;
            for (int k = 0; k < n; k++) if (ab < 0 && c.compare(teams.get(j), teams.get(k)) < 0 && !(c.compare(teams.get(i), teams.get(k)) < 0)) bad++; }
        return bad;
    }

    static double mean(double[] a) { double s = 0; for (double x : a) s += x; return s / a.length; }

    public static void main(String[] args) throws Exception {
        // ----- data set 1: many ties at every level
        List<Team> tied = List.of(new Team("Lions", 12, 5), new Team("Tigers", 12, 5), new Team("Wolves", 12, 5), new Team("Hawks", 12, 2), new Team("Owls", 12, 2),
                new Team("Sharks", 9, 7), new Team("Rams", 9, 7), new Team("Bulls", 9, 0), new Team("Foxes", 6, 3), new Team("Bears", 6, 3), new Team("Yaks", 3, -2));
        Board<Team> b = new Board<>(CORRECT); b.addAll(tied);
        System.out.println("Data set 1 (ties at every level), correct comparator:\n  " + b.ranking());
        List<Team> shuffled = new ArrayList<>(tied); Collections.shuffle(shuffled, new Random(4));
        Board<Team> b2 = new Board<>(CORRECT); b2.addAll(shuffled);
        System.out.println("  same ranking regardless of input order: " + b.ranking().equals(b2.ranking()));
        // ----- faulty comparators
        System.out.println("\nFaulty comparator 1: points compared by SUBTRACTION, data set 2 with extreme points");
        Team A = new Team("A", 2_000_000_000, 0), B = new Team("B", 0, 0), C = new Team("C", -2_000_000_000, 0);
        System.out.println("  cmp(A,B)=" + SUBTRACT.compare(A, B) + " (A first)  cmp(B,C)=" + SUBTRACT.compare(B, C) + " (B first)  cmp(A,C)=" + SUBTRACT.compare(A, C) + " (positive: C first!)");
        System.out.println("  => A<B, B<C and C<A: a cycle, so no consistent ranking exists");
        List<Team> extreme = new ArrayList<>(); Random r = new Random(2);
        for (int i = 0; i < 60; i++) extreme.add(new Team("T" + i, r.nextBoolean() ? 2_000_000_000 - r.nextInt(100) : -2_000_000_000 + r.nextInt(100), r.nextInt(5)));
        extreme.add(new Team("Z", 0, 0));
        try { List<Team> cp = new ArrayList<>(extreme); cp.sort(SUBTRACT); System.out.println("  sort finished without exception, but transitivity violations among the 61 teams: " + transitivityViolations(extreme, SUBTRACT)); }
        catch (IllegalArgumentException e) { System.out.println("  List.sort threw: " + e.getMessage()); }
        System.out.println("  transitivity violations, correct comparator on the same data: " + transitivityViolations(extreme, CORRECT));
        System.out.println("\nFaulty comparator 2: tie-breaker (name) ignored; two input orders of the SAME teams");
        List<Team> p1 = new ArrayList<>(List.of(new Team("Panthers", 9, 4), new Team("Bisons", 9, 4))), p2 = new ArrayList<>(List.of(new Team("Bisons", 9, 4), new Team("Panthers", 9, 4)));
        p1.sort(NO_NAME); p2.sort(NO_NAME); System.out.println("  input (Panthers, Bisons) -> " + p1 + "    input (Bisons, Panthers) -> " + p2 + "   (result depends on input order: not a ranking)");
        List<Team> q1 = new ArrayList<>(p1), q2 = new ArrayList<>(p2); q1.sort(CORRECT); q2.sort(CORRECT);
        System.out.println("  correct comparator -> " + q1 + " and " + q2);
        List<Team> g = new ArrayList<>(List.of(new Team("X", 9, 1), new Team("Y", 9, 8))); g.sort(GD_ASC);
        System.out.println("Faulty comparator 3: goal difference in the wrong direction -> " + g + " (Y with goalDiff 8 should be first)");
        // ----- wildcard / bounded type demonstration
        List<Player> players = List.of(new Player("Ravi", 7), new Player("Asha", 9), new Player("Kiran", 9));
        Board<Player> pb = new Board<>(highFirst((Player p) -> p.goals()).thenComparing(Player::name)); pb.addAll(players);
        System.out.println("\nSame generic Board reused for players: " + pb.ranking());
        System.out.println("Bounded type best(): " + best(List.of(new TeamC("Eagles", 9, 6), new TeamC("Bisons", 9, 4))));
        // ----- Alternative: Comparator chain (1 sort) vs 3 stable sorts vs Comparable
        System.out.println("\n=== Alternatives: one sort with Comparator chain vs three stable sorts vs Comparable (ms, mean of 5 runs)");
        try (PrintWriter csv = new PrintWriter("data/q5_sort_times.csv")) {
            csv.println("n,approach,rep,ms");
            for (int n : new int[]{10_000, 100_000, 500_000}) {
                Random rr = new Random(8); List<Team> base = new ArrayList<>(); for (int i = 0; i < n; i++) base.add(new Team("T" + rr.nextInt(n / 4 + 1), rr.nextInt(30), rr.nextInt(15) - 7));
                List<TeamC> baseC = new ArrayList<>(); for (Team t : base) baseC.add(new TeamC(t.name(), t.points(), t.goalDiff()));
                double[] c1 = new double[5], c3 = new double[5], c2 = new double[5];
                for (int rep = -1; rep < 5; rep++) {
                    List<Team> x = new ArrayList<>(base); long t0 = System.nanoTime(); x.sort(CORRECT); long t1 = System.nanoTime();
                    List<Team> y = new ArrayList<>(base); y.sort(Comparator.comparing(Team::name)); y.sort(Comparator.comparingInt(Team::goalDiff).reversed()); y.sort(Comparator.comparingInt(Team::points).reversed()); long t2 = System.nanoTime();
                    List<TeamC> z = new ArrayList<>(baseC); Collections.sort(z); long t3 = System.nanoTime();
                    if (rep == 0) System.out.println("  n=" + n + ": three stable sorts give the same order as the chain: " + x.equals(y) + "; Comparable gives the same order: " + (x.get(0).name().equals(z.get(0).name())));
                    if (rep >= 0) { c1[rep] = (t1 - t0) / 1e6; c3[rep] = (t2 - t1) / 1e6; c2[rep] = (t3 - t2) / 1e6;
                        csv.printf("%d,comparator chain (1 sort),%d,%.3f%n%d,three stable sorts,%d,%.3f%n%d,Comparable natural order,%d,%.3f%n", n, rep, c1[rep], n, rep, c3[rep], n, rep, c2[rep]); }
                }
                System.out.printf("  n=%-7d comparator chain %8.2f ms | three stable sorts %8.2f ms | Comparable %8.2f ms%n", n, mean(c1), mean(c3), mean(c2));
            }
        }
    }
}
