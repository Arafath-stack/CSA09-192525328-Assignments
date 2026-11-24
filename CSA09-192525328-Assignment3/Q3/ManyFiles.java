import java.io.*;
import java.nio.file.*;

/** Alternative concepts for f files: no close at all, close in finally, try-with-resources. Run with a LOW file-descriptor limit. */
public class ManyFiles {
    static int limit = 1 << 30;
    static int fds() { String[] l = new File("/proc/self/fd").list(); return l == null ? limit : l.length; }   // null: no descriptor left even to list the directory
    static long readNoClose(String f) throws IOException { BufferedReader r = new BufferedReader(new FileReader(f)); long t = 0; String l; while ((l = r.readLine()) != null) t += l.length(); return t; }
    static long readFinally(String f) throws IOException { BufferedReader r = null; try { r = new BufferedReader(new FileReader(f)); long t = 0; String l; while ((l = r.readLine()) != null) t += l.length(); return t; } finally { if (r != null) r.close(); } }
    static long readTwr(String f) throws IOException { try (BufferedReader r = new BufferedReader(new FileReader(f))) { long t = 0; String l; while ((l = r.readLine()) != null) t += l.length(); return t; } }

    public static void main(String[] args) throws Exception {
        int files = Integer.parseInt(args[0]); if (args.length > 1) limit = Integer.parseInt(args[1]);
        Path dir = Paths.get("/tmp/manyfiles"); Files.createDirectories(dir);
        for (int i = 0; i < files; i++) { Path p = dir.resolve("f" + i + ".csv"); if (!Files.exists(p)) { try (PrintWriter w = new PrintWriter(p.toFile())) { for (int k = 0; k < 20; k++) w.println("item" + k + "," + k + ",10"); } } }
        int base = fds();
        for (String mode : new String[]{"no close", "close in finally", "try-with-resources"}) {
            int done = 0, peak = 0; long sum = 0; String error = "none";
            long t0 = System.nanoTime();
            try {
                for (int i = 0; i < files; i++) {
                    String f = dir.resolve("f" + i + ".csv").toString();
                    sum += mode.equals("no close") ? readNoClose(f) : mode.equals("close in finally") ? readFinally(f) : readTwr(f);
                    done++; peak = Math.max(peak, fds() - base);
                }
            } catch (IOException e) { error = e.getClass().getSimpleName() + ": " + e.getMessage(); peak = Math.max(peak, fds() - base); }
            long t1 = System.nanoTime();
            System.out.printf("%-20s processed %5d of %d files, peak open handles %5d, time %7.1f ms, error: %s%n", mode, done, files, peak, (t1 - t0) / 1e6, error);
            if (mode.equals("no close")) { System.gc(); Thread.sleep(500); }      // let the garbage collector release the leaked handles before the next mode
        }
    }
}
