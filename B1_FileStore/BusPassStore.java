import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Full bus-pass store designed in Task A1 (text format):
 *   passId,regNo,name,route,amount     (name is percent-encoded for , % and newline)
 * Extends PassStore (the given class) with: strict validation, listByRoute, safe add.
 */
public class BusPassStore extends PassStore {
    private final Path path;

    public BusPassStore(Path path) { super(path); this.path = path; }

    // ---------- name encoding (handles a name that contains the delimiter) ----------
    public static String encodeName(String s) {
        return s.replace("%", "%25").replace(",", "%2C").replace("\n", "%0A").replace("\r", "%0D");
    }
    public static String decodeName(String s) {
        return s.replace("%2C", ",").replace("%0A", "\n").replace("%0D", "\r").replace("%25", "%");
    }

    /** Strict parse: returns the 5 fields, or null if the line is damaged. */
    public static String[] parseLine(String line) {
        return parseLine(line, null);
    }
    /** As above but fills reason[0] with why the line was rejected. */
    public static String[] parseLine(String line, String[] reason) {
        if (line == null || line.isBlank()) return reject(reason, "empty line");
        String[] f = line.split(",", -1);
        if (f.length != 5) return reject(reason, "expected 5 fields but found " + f.length);
        if (f[0].isBlank()) return reject(reason, "empty pass id");
        if (!f[1].matches("\\d{6,15}")) return reject(reason, "register number not numeric");
        if (f[2].isBlank()) return reject(reason, "empty name");
        try { int r = Integer.parseInt(f[3]); if (r <= 0) return reject(reason, "route must be positive"); }
        catch (NumberFormatException e) { return reject(reason, "route not an integer"); }
        try {
            double a = Double.parseDouble(f[4]);
            if (Double.isNaN(a) || Double.isInfinite(a) || a < 0) return reject(reason, "amount invalid");
        } catch (NumberFormatException e) { return reject(reason, "amount not numeric"); }
        return f;
    }
    private static String[] reject(String[] reason, String why) { if (reason != null) reason[0] = why; return null; }

    /** Safe add: validates, encodes the name, writes ONE complete line with flush. */
    public void addSafe(String passId, String regNo, String name, int route, double amount) throws IOException {
        String line = String.join(",", passId, regNo, encodeName(name), String.valueOf(route), String.valueOf(amount));
        if (parseLine(line) == null) throw new IllegalArgumentException("invalid pass record: " + line);
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
            w.flush();
        }
    }

    /** Strict find: skips ANY damaged line (not only wrong field count). Tolerates a missing file. */
    public String[] findStrict(String passId) throws IOException {
        if (!Files.exists(path)) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseLine(line);
                if (f != null && f[0].equals(passId)) return f;
            }
        }
        return null;
    }

    /** List all valid passes on a route. */
    public List<String[]> listByRoute(int route) throws IOException {
        List<String[]> out = new ArrayList<>();
        if (!Files.exists(path)) return out;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseLine(line);
                if (f != null && Integer.parseInt(f[3]) == route) out.add(f);
            }
        }
        return out;
    }

    /** Load the whole file once into a HashMap (Alternative concept for B1). */
    public Map<String, String[]> loadIndex() throws IOException {
        Map<String, String[]> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseLine(line);
                if (f != null) map.putIfAbsent(f[0], f);
            }
        }
        return map;
    }
}
