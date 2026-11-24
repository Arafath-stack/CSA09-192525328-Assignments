import java.io.*;
import java.nio.file.*;

/** Joint decision: try-with-resources closes the file but cannot undo a half-written report. */
public class AtomicWrite {
    static void write(Writer w, boolean failHalfway) throws IOException {
        w.write("store1.csv,1500\n"); w.write("store2.csv,2100\n");
        if (failHalfway) throw new IOException("disk error while writing store3");
        w.write("store3.csv,900\nTOTAL,4500\n");
    }
    public static void main(String[] args) throws Exception {
        Path report = Paths.get("report_direct.txt"), tmp = Paths.get("report_final.txt.tmp"), fin = Paths.get("report_final.txt");
        Files.deleteIfExists(report); Files.deleteIfExists(tmp); Files.deleteIfExists(fin);
        try (Writer w = new FileWriter(report.toFile())) { write(w, true); } catch (IOException e) { System.out.println("direct write failed: " + e.getMessage()); }
        System.out.println("direct write: file exists = " + Files.exists(report) + ", content = " + Files.readAllLines(report) + "   <- closed properly, but INCOMPLETE and looks like a real report");
        try (Writer w = new FileWriter(tmp.toFile())) { write(w, true); Files.move(tmp, fin, StandardCopyOption.ATOMIC_MOVE); } catch (IOException e) { System.out.println("temp-file write failed: " + e.getMessage()); }
        Files.deleteIfExists(tmp);
        System.out.println("temp file + atomic rename: final report exists = " + Files.exists(fin) + "   <- no incomplete report is ever published");
        try (Writer w = new FileWriter(tmp.toFile())) { write(w, false); }
        Files.move(tmp, fin, StandardCopyOption.ATOMIC_MOVE);
        System.out.println("successful run: final report = " + Files.readAllLines(fin));
        Files.deleteIfExists(report); Files.deleteIfExists(fin);
    }
}
