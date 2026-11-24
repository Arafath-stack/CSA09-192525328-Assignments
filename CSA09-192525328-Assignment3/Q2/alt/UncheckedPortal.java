/** Alternative: the same rules with UNCHECKED exceptions (extends RuntimeException). */
class ExamRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    ExamRuntimeException(String m) { super(m); }
    ExamRuntimeException(String m, Throwable cause) { super(m, cause); }
}
class ExamClosedRuntime extends ExamRuntimeException { private static final long serialVersionUID = 1L; ExamClosedRuntime(String m) { super(m); } }
class StorageRuntime extends ExamRuntimeException { private static final long serialVersionUID = 1L; StorageRuntime(String m, Throwable c) { super(m, c); } }

class Svc {
    void submit(boolean open, long sizeKb) {                       // NO throws clause needed
        if (!open) throw new ExamClosedRuntime("window closed");
        try { if (sizeKb > 500) throw new java.io.IOException("Disk quota exceeded"); }
        catch (java.io.IOException e) { throw new StorageRuntime("Could not store file", e); }
    }
}
public class UncheckedPortal {
    public static void main(String[] args) {
        Svc s = new Svc();
        s.submit(true, 100);                                           // compiles WITHOUT try/catch: the compiler no longer forces handling
        System.out.println("call 1 fine");
        try { s.submit(true, 800); } catch (ExamRuntimeException e) { System.out.println("handled by the controller: " + e.getMessage() + " <- " + e.getCause().getMessage()); }
        System.out.println("If a caller forgets the try/catch, the exception travels to main and ends the program:");
        s.submit(false, 100);
    }
}
