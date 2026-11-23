import java.nio.file.*;
import java.util.*;

/** B2 - damaged bus-pass file: base PassStore.find (TODO 1 only) versus the strict BusPassStore. */
public class DamagedFileDemo {
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("damaged", ".csv");
        String content =
            "P2001,192312001,Arun,12,4500.0\n" +      // valid
            "P2002,192312002,Meena,7\n" +             // wrong number of fields (4)
            "P2003,192312003,Karthik,12,FIVE-HUNDRED\n" + // non-numeric amount
            "\n" +                                    // empty line
            "P2004,192312004,Divya,abc,3800.0\n" +    // non-numeric route
            "P2005,192312005,Ravi,5,-100.0\n" +       // negative amount
            "P2006,192312006,Lakshmi,9,3900.0\n";     // valid, after all the damage
        Files.writeString(f, content);
        System.out.println("File content:\n" + content.replace("\n\n", "\n<EMPTY LINE>\n"));

        PassStore base = new PassStore(f);
        BusPassStore strict = new BusPassStore(f);
        System.out.printf("%-8s | %-45s | %s%n", "Pass ID", "Base find() (TODO 1: only counts fields)", "Strict findStrict() (parses values)");
        for (String id : new String[]{"P2001", "P2002", "P2003", "P2004", "P2005", "P2006"})
            System.out.printf("%-8s | %-45s | %s%n", id, Arrays.toString(base.find(id)), Arrays.toString(strict.findStrict(id)));

        System.out.println("\nReasons the strict reader rejects each line:");
        int n = 0;
        for (String line : Files.readAllLines(f)) {
            n++;
            String[] why = new String[1];
            String[] r = BusPassStore.parseLine(line, why);
            System.out.printf("  line %d: %s%s%n", n, r != null ? "OK" : "REJECTED - " + why[0], r != null ? "" : "   [" + line + "]");
        }
        Files.delete(f);
    }
}
