import java.nio.file.*;
import java.util.*;

/** B1 Alternative - memory used by a HashMap index of n passes versus the streaming scan. */
public class MemoryBench {
    static long used() { Runtime r = Runtime.getRuntime(); for (int i = 0; i < 3; i++) System.gc(); return r.totalMemory() - r.freeMemory(); }
    public static void main(String[] args) throws Exception {
        System.out.println("n,file_MB,hashmap_heap_MB,scan_buffer_KB");
        for (int n : new int[]{1000, 10000, 100000}) {
            Path f = Files.createTempFile("mem", ".csv");
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= n; i++) sb.append("P").append(String.format("%06d", i)).append(",19231").append(String.format("%04d", i % 10000)).append(",Student").append(i).append(",").append(1 + i % 40).append(",").append(3500 + i % 1000).append(".0\n");
            Files.writeString(f, sb.toString()); sb = null;
            BusPassStore s = new BusPassStore(f);
            long before = used();
            Map<String, String[]> idx = s.loadIndex();
            long after = used();
            System.out.printf("%d,%.1f,%.1f,8 (one BufferedReader buffer + one line)%n", n, Files.size(f) / 1048576.0, (after - before) / 1048576.0);
            if (idx.size() != n) System.out.println("!!");
            Files.delete(f);
        }
    }
}
