import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Creates the "existing" bus-pass text file (10,000 good lines) with deliberately damaged lines mixed in. */
public class GenerateLegacyFile {
    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "input/passes_legacy.csv");
        Files.createDirectories(out.getParent());
        Random r = new Random(7);
        try (BufferedWriter w = Files.newBufferedWriter(out)) {
            for (int i = 1; i <= 10000; i++) {
                String line = String.format("P%06d", i) + ",19231" + String.format("%04d", i % 10000) + ",Student" + i + "," + (1 + i % 40) + "," + (3500 + i % 1000) + ".0";
                if (i % 997 == 0) line = String.format("P%06d", i) + ",19231" + String.format("%04d", i % 10000) + ",Student" + i + "," + (1 + i % 40);          // 4 fields
                else if (i % 1499 == 0) line = String.format("P%06d", i) + ",19231" + String.format("%04d", i % 10000) + ",Student" + i + ",7,N/A";             // non-numeric amount
                else if (i % 2503 == 0) line = "";                                                                                                               // empty line
                else if (i % 3001 == 0) line = String.format("P%06d", i) + ",ABC," + "Student" + i + ",7,3900.0";                                                // bad register number
                else if (i == 4000)     line = "P000010,19231" + "4000,Duplicate Dan,5,3600.0";                                                                  // duplicate pass id
                w.write(line); w.newLine();
            }
        }
        System.out.println("Created " + out + " with 10,000 lines (some damaged)");
    }
}
