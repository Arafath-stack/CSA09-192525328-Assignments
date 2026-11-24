import java.io.*;
import java.nio.file.*;

/** Creates 10,000 score files: about 1.9% corrupt (5 kinds) and 1.0% missing. Writes expected.txt with the true counts. */
public class GenerateData {
    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data"); Files.createDirectories(dir);
        int n = 10000, ok = 0, corrupt = 0, missing = 0; long totalSum = 0;
        StringBuilder ids = new StringBuilder();
        for (int i = 1; i <= n; i++) {
            String id = String.format("S%05d", i); ids.append(id).append('\n');
            Path f = dir.resolve("score_" + id + ".txt");
            if (i % 97 == 0) { missing++; Files.deleteIfExists(f); continue; }              // missing file
            StringBuilder sb = new StringBuilder(id).append('\n'); int total = 0;
            int[] m = new int[5]; for (int k = 0; k < 5; k++) { m[k] = (i * (7 + k * 3) + k * 11) % 101; total += m[k]; }
            String[] sub = ScoreReader.SUBJECTS;
            if (i % 53 == 0) {                                                                // corrupt file, five kinds
                corrupt++;
                switch ((i / 53) % 5) {
                    case 0: for (int k = 0; k < 5; k++) sb.append(sub[k]).append('=').append(k == 0 ? "7x" : String.valueOf(m[k])).append('\n'); break;   // not a number
                    case 1: for (int k = 0; k < 5; k++) sb.append(sub[k]).append('=').append(k == 2 ? 105 : m[k]).append('\n'); break;                  // out of range
                    case 2: for (int k = 0; k < 4; k++) sb.append(sub[k]).append('=').append(m[k]).append('\n'); break;                                  // line missing
                    case 3: sb.setLength(0); break;                                                                                                       // empty file
                    default: Files.write(f, new byte[]{(byte) 0xFF, (byte) 0xFE, 0x00, (byte) 0xC3, 0x28, (byte) 0x9F}); continue;                       // binary garbage
                }
                Files.writeString(f, sb.toString());
                continue;
            }
            for (int k = 0; k < 5; k++) sb.append(sub[k]).append('=').append(m[k]).append('\n');
            Files.writeString(f, sb.toString());
            ok++; totalSum += total;
        }
        Files.writeString(dir.resolve("ids.txt"), ids.toString());
        Files.writeString(dir.resolve("expected.txt"), "ok=" + ok + " corrupt=" + corrupt + " missing=" + missing + " totalSum=" + totalSum + "\n");
        System.out.println("generated " + n + " ids in " + dir + ": expected ok=" + ok + " corrupt=" + corrupt + " missing=" + missing + " sum of valid totals=" + totalSum);
    }
}
