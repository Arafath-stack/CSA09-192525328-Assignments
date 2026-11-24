import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/** Alternative: no exceptions - check everything first and return error codes. */
public class AtmCodes {
    static final int OK = 0, BAD_NUMBER = -1, NEGATIVE = -2, NO_FUNDS = -3, NO_ACCOUNT = -4, NO_FILE = -5, NO_INPUT = -6;
    static Map<Integer, Integer> balances = new HashMap<>();
    static { balances.put(1001, 5000); balances.put(1002, 150); }

    static int withdraw(Scanner in) {
        if (!in.hasNextInt()) return in.hasNext() ? BAD_NUMBER : NO_INPUT;      // check 1
        int acc = in.nextInt();
        if (!in.hasNextInt()) return in.hasNext() ? BAD_NUMBER : NO_INPUT;      // check 2
        int amt = in.nextInt();
        if (amt <= 0) return NEGATIVE;                                           // check 3
        Integer bal = balances.get(acc);
        if (bal == null) return NO_ACCOUNT;                                      // check 4
        if (amt > bal) return NO_FUNDS;                                          // check 5
        return OK;
    }
    static int openCardFile(String path) { return new File(path).exists() ? OK : NO_FILE; }   // check 6 (the file may still vanish afterwards)

    public static void main(String[] args) {
        String[] inputs = {"1001 500", "abc 500", "1001 -50", "1002 500", "9999 100", "1001", ""};
        for (String s : inputs) System.out.printf("%-12s -> code %d%n", "\"" + s + "\"", withdraw(new Scanner(s)));
        System.out.println("missing file -> code " + openCardFile("nofile.txt"));
        System.out.println("Every caller must test the returned code after every call (7 distinct codes here); forgetting one silently ignores the failure.");
    }
}
