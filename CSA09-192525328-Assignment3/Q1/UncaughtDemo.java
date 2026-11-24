import java.util.Scanner;

/** Student B: an exception nobody catches: the JVM prints the stack trace and ends the program. */
public class UncaughtDemo {
    static int readAmount(String text) {
        return Integer.parseInt(text);                 // line 6: throws NumberFormatException for "12x"
    }
    static int dispense(String text) {
        int amount = readAmount(text);                 // line 9
        return amount * 2;
    }
    public static void main(String[] args) {
        System.out.println("before");
        int cash = dispense("12x");                    // line 14: no try/catch anywhere
        System.out.println("never printed " + cash);
    }
}
