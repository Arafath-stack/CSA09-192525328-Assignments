import java.util.InputMismatchException;
import java.util.NoSuchElementException;

public class ErrMultiCatch {
    public static void main(String[] args) {
        try {
            new java.util.Scanner("abc").nextInt();
        } catch (InputMismatchException | NoSuchElementException e) {   // InputMismatchException IS-A NoSuchElementException
            System.out.println("input problem");
        }
    }
}
