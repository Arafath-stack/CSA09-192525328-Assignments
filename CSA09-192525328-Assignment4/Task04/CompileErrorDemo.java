import javax.tools.*;
import java.net.URI;
import java.util.List;

/** Task 4.4: compile the misspelled handler with and without @Override and print the compiler's answer. */
public class CompileErrorDemo {
    static void compile(String label, String body) {
        String src = "import java.awt.event.*;\npublic class Probe {\n  MouseAdapter a = new MouseAdapter() {\n" + body + "\n  };\n}\n";
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler(); DiagnosticCollector<JavaFileObject> d = new DiagnosticCollector<>();
        JavaFileObject f = new SimpleJavaFileObject(URI.create("string:///Probe.java"), JavaFileObject.Kind.SOURCE) { public CharSequence getCharContent(boolean b) { return src; } };
        boolean ok = jc.getTask(null, null, d, List.of("-proc:none", "-d", System.getProperty("java.io.tmpdir")), null, List.of(f)).call();
        System.out.println("--- " + label + "\n    source line: " + body.trim() + "\n    compiled successfully: " + ok);
        for (Diagnostic<?> x : d.getDiagnostics()) System.out.println("    compiler message: Probe.java:" + x.getLineNumber() + ": " + x.getKind().toString().toLowerCase() + ": " + x.getMessage(null));
    }
    public static void main(String[] args) {
        compile("Without @Override, misspelled mouseclicked (the original defect)", "    public void mouseclicked(MouseEvent e) { }");
        compile("With @Override, misspelled mouseclicked", "    @Override public void mouseclicked(MouseEvent e) { }");
        compile("With @Override, correct mouseClicked (the repair)", "    @Override public void mouseClicked(MouseEvent e) { }");
    }
}
