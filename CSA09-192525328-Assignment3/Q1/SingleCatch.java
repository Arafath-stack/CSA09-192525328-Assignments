import java.util.Scanner;
/** Joint design question: the ATM with ONE catch (Exception e). */
public class SingleCatch {
    static String session(String input, int balance) {
        try {
            Scanner in = new Scanner(input);
            int acc = in.nextInt(), amt = in.nextInt();
            if (amt <= 0) throw new IllegalArgumentException("negative");
            if (amt > balance) throw new Exception("insufficient");
            int[] accounts = new int[1];
            accounts[acc - 1000] = amt;                 // BUG: acc 1001 -> index 1, ArrayIndexOutOfBounds
            return "ok";
        } catch (Exception e) {
            return "Sorry, something went wrong";       // the same message for every failure
        }
    }
    public static void main(String[] args) {
        System.out.println("letters          -> " + session("abc 5", 100));
        System.out.println("negative amount  -> " + session("1000 -5", 100));
        System.out.println("too much         -> " + session("1000 500", 100));
        System.out.println("programming bug  -> " + session("1001 50", 100) + "   <- an ArrayIndexOutOfBoundsException is hidden as well");
    }
}
