import java.util.Scanner;

/** Student B: ten inputs, each aimed at one path; records the catch block and whether finally ran. */
public class AtmTests {
    static int n = 0;
    static void run(String label, String input) {
        Atm atm = new Atm(); String[] t = new String[2];
        String st = Atm.start(atm, "cardreader.txt", t);
        String result = Atm.session(atm, new Scanner(input), t);
        System.out.printf("%2d %-34s input=%-12s -> %-46s | %-58s | finally ran: %s%n", ++n, label, "\"" + input + "\"", result, t[0], t[1]);
    }
    static void runStart(String label, String file) {
        Atm atm = new Atm(); String[] t = new String[2];
        String result = Atm.start(atm, file, t);
        System.out.printf("%2d %-34s file=%-26s -> %-46s | %-58s | finally ran: %s%n", ++n, label, file, result, t[0], t[1]);
    }
    public static void main(String[] args) {
        run("valid withdrawal", "1001 500");
        run("exact balance", "1002 150");
        run("letters instead of account", "abc 500");
        run("letters instead of amount", "1001 5x0");
        run("negative amount", "1001 -50");
        run("zero amount", "1001 0");
        run("more than the balance", "1002 500");
        run("unknown account", "9999 100");
        run("amount missing", "1001");
        run("empty input", "");
        runStart("card-reader file missing", "nofile.txt");
        runStart("card-reader file corrupt", "cardreader_corrupt.txt");
    }
}
