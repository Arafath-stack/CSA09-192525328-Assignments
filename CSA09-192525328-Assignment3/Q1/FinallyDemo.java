/** Student A: what finally guarantees, and what it does not. */
public class FinallyDemo {
    static int returnInTry() {
        try { System.out.println("  try: returning 1"); return 1; }
        finally { System.out.println("  finally ran although try executed return"); }
    }
    static void exceptionInTry() {
        try { throw new IllegalStateException("boom"); }
        finally { System.out.println("  finally ran although the exception is not caught here"); }
    }
    @SuppressWarnings("finally")
    static int finallyOverrides() {
        try { return 1; }
        finally { return 2; }                          // a return in finally replaces the result: avoid
    }
    public static void main(String[] args) {
        System.out.println("1) return inside try:"); returnInTry();
        System.out.println("2) uncaught exception inside try:");
        try { exceptionInTry(); } catch (IllegalStateException e) { System.out.println("  caught later: " + e.getMessage()); }
        System.out.println("3) return in finally replaces the result: " + finallyOverrides());
        System.out.println("4) System.exit() inside try: finally does NOT run");
        try { System.out.println("  try: calling System.exit(0)"); System.exit(0); }
        finally { System.out.println("  finally (you will not see this line)"); }
    }
}
