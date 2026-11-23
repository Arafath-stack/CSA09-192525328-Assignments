import java.util.*;
public class Gen {
    /** Generates n deterministic bus-pass records: {passId, regNo, name, route, amount}. */
    public static List<String[]> passes(int n) {
        List<String[]> l = new ArrayList<>(n);
        for (int i = 1; i <= n; i++)
            l.add(new String[]{ String.format("P%06d", i), "19231" + String.format("%04d", i % 10000),
                                "Student" + i, String.valueOf(1 + i % 40), String.valueOf(3500 + i % 1000) + ".0" });
        return l;
    }
}
