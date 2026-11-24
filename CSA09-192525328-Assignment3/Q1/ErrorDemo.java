/** Errors are not exceptions of the program: the ATM must not try to recover from them. */
public class ErrorDemo {
    static int depth = 0;
    static void recurse() { depth++; recurse(); }
    public static void main(String[] args) {
        try { recurse(); }
        catch (StackOverflowError e) { System.out.println("StackOverflowError after " + depth + " nested calls (an Error, not an Exception)"); }
        try { long[] huge = new long[Integer.MAX_VALUE]; System.out.println(huge.length); }
        catch (OutOfMemoryError e) { System.out.println("OutOfMemoryError: " + e.getMessage()); }
        System.out.println("Error is a sibling of Exception under Throwable: " + (Error.class.getSuperclass() == Throwable.class) + ", " + (Exception.class.getSuperclass() == Throwable.class));
    }
}
