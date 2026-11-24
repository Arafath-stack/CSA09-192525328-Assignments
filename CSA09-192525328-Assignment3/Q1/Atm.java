import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Checked exceptions of the ATM: the caller can recover (tell the customer, offer a smaller amount). */
class AtmException extends Exception { AtmException(String m) { super(m); } }
class InsufficientFundsException extends AtmException { InsufficientFundsException(int shortBy) { super("Insufficient funds: short by Rs " + shortBy); } }
class AccountNotFoundException extends AtmException { AccountNotFoundException(int acc) { super("Account " + acc + " not found"); } }

class Atm {
    private final Map<Integer, Integer> balances = new HashMap<>();   // whole rupees

    /** Reads the card-reader file. FileNotFoundException (checked) when it is missing. */
    void loadCardReaderFile(String path) throws IOException {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split(",");
                balances.put(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
            }
        } catch (NumberFormatException e) {
            throw new IOException("Card-reader file is corrupt", e);      // translate an unchecked failure into the layer's checked one
        } finally {
            if (r != null) r.close();                                      // guaranteed release of the file
        }
    }

    int withdraw(int account, int amount) throws InsufficientFundsException, AccountNotFoundException {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive: " + amount);   // unchecked: programming/input error
        Integer balance = balances.get(account);
        if (balance == null) throw new AccountNotFoundException(account);
        if (amount > balance) throw new InsufficientFundsException(amount - balance);
        balances.put(account, balance - amount);
        return balance - amount;
    }

    /** One ATM session. trace[0] receives the catch block that ran, trace[1] whether finally ran. */
    static String session(Atm atm, Scanner in, String[] trace) {
        trace[0] = "none"; trace[1] = "no";
        try {
            int account = in.nextInt();                                    // InputMismatchException / NoSuchElementException
            int amount = in.nextInt();
            int left = atm.withdraw(account, amount);
            return "Dispensed Rs " + amount + ", balance Rs " + left;
        } catch (InputMismatchException | IllegalArgumentException e) {   // multi-catch: unrelated types, one handler
            trace[0] = "catch 1: InputMismatchException | IllegalArgumentException";
            return "Invalid input: " + e.getClass().getSimpleName() + (e.getMessage() != null ? " (" + e.getMessage() + ")" : "");
        } catch (AccountNotFoundException | InsufficientFundsException e) { // multi-catch of two sibling checked exceptions
            trace[0] = "catch 2: AccountNotFoundException | InsufficientFundsException";
            return "Refused: " + e.getMessage();
        } catch (NoSuchElementException e) {                               // must come AFTER InputMismatchException (its subclass)
            trace[0] = "catch 3: NoSuchElementException";
            return "Input ended too early";
        } finally {
            trace[1] = "yes";
            // in a real ATM: return the card, close the session
        }
    }

    /** Start-up: catch the narrower FileNotFoundException before the broader IOException. */
    static String start(Atm atm, String file, String[] trace) {
        trace[0] = "none"; trace[1] = "no";
        try {
            atm.loadCardReaderFile(file);
            return "ATM ready";
        } catch (java.io.FileNotFoundException e) {
            trace[0] = "catch FileNotFoundException";
            return "ATM out of service: card-reader file missing (" + e.getMessage() + ")";
        } catch (IOException e) {
            trace[0] = "catch IOException";
            return "ATM out of service: " + e.getMessage() + " <- cause: " + e.getCause();
        } finally {
            trace[1] = "yes";
        }
    }

    public static void main(String[] args) {
        Atm atm = new Atm();
        String[] t = new String[2];
        String status = start(atm, args.length > 0 ? args[0] : "cardreader.txt", t);
        System.out.println(status);
        if (!status.equals("ATM ready")) return;
        Scanner in = new Scanner(System.in);
        System.out.println(session(atm, in, t));
    }
}
