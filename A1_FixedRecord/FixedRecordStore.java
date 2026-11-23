import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Task A1 - Alternative concept: RandomAccessFile with fixed-length records.
 * Record layout (92 bytes):
 *   passId 8 | regNo 12 | name 30 | route int 4 | stop 20 | validity 10 | amount double 8
 * Record i starts at byte i * 92, so find-by-index is one seek: O(1).
 */
public class FixedRecordStore {
    static final int ID = 8, REG = 12, NAME = 30, STOP = 20, DATE = 10;
    static final int RECORD = ID + REG + NAME + 4 + STOP + DATE + 8;   // 92

    static void putFixed(RandomAccessFile raf, String s, int len) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[len];                       // zero-padded
        System.arraycopy(b, 0, out, 0, Math.min(b.length, len));
        raf.write(out);
    }
    static String getFixed(RandomAccessFile raf, int len) throws IOException {
        byte[] b = new byte[len]; raf.readFully(b);
        int end = len; while (end > 0 && b[end - 1] == 0) end--;
        return new String(b, 0, end, StandardCharsets.UTF_8);
    }
    static void write(RandomAccessFile raf, long index, String id, String reg, String name, int route,
                      String stop, String date, double amt) throws IOException {
        raf.seek(index * RECORD);
        putFixed(raf, id, ID); putFixed(raf, reg, REG); putFixed(raf, name, NAME);
        raf.writeInt(route); putFixed(raf, stop, STOP); putFixed(raf, date, DATE); raf.writeDouble(amt);
    }
    static String[] read(RandomAccessFile raf, long index) throws IOException {
        raf.seek(index * RECORD);                          // the O(1) seek
        return new String[]{ getFixed(raf, ID), getFixed(raf, REG), getFixed(raf, NAME),
            String.valueOf(raf.readInt()), getFixed(raf, STOP), getFixed(raf, DATE), String.valueOf(raf.readDouble()) };
    }

    public static void main(String[] args) throws Exception {
        int n = 100_000;
        Path bin = Files.createTempFile("passes", ".bin"), txt = Files.createTempFile("passes", ".csv");
        try (RandomAccessFile raf = new RandomAccessFile(bin.toFile(), "rw");
             BufferedWriter w = Files.newBufferedWriter(txt)) {
            for (int i = 0; i < n; i++) {
                String id = String.format("P%06d", i + 1), reg = "19231" + String.format("%04d", i % 10000);
                String name = "Student " + i, stop = "Stop" + (i % 50), date = "2027-06-30";
                write(raf, i, id, reg, name, 1 + i % 40, stop, date, 3500 + i % 1000);
                w.write(String.join(",", id, reg, name, String.valueOf(1 + i % 40), stop, date, String.valueOf(3500.0 + i % 1000)));
                w.newLine();
            }
        }
        long binSize = Files.size(bin), txtSize = Files.size(txt);
        System.out.println("Records: " + n);
        System.out.println("Fixed-length file : " + binSize + " bytes (" + RECORD + " bytes/record)");
        System.out.println("Delimited text    : " + txtSize + " bytes (" + String.format("%.1f", (double) txtSize / n) + " bytes/record average)");
        System.out.printf("Padding overhead of fixed layout vs text: %.1f%%%n", 100.0 * (binSize - txtSize) / txtSize);

        // Warm-up then measure: find LAST pass (worst case for scan)
        long target = n - 1;
        String targetId = String.format("P%06d", n);
        double seekUs = 0, scanMs = 0;
        try (RandomAccessFile raf = new RandomAccessFile(bin.toFile(), "r")) {
            for (int i = 0; i < 2000; i++) read(raf, target);
            long t = System.nanoTime();
            String[] r = null;
            for (int i = 0; i < 10000; i++) r = read(raf, target);
            seekUs = (System.nanoTime() - t) / 1e3 / 10000;
            System.out.println("Seek read of record " + (target + 1) + ": " + Arrays.toString(r));
        }
        for (int i = 0; i < 5; i++) scan(txt, targetId);
        long t = System.nanoTime();
        for (int i = 0; i < 15; i++) scan(txt, targetId);
        scanMs = (System.nanoTime() - t) / 1e6 / 15;
        System.out.printf("Find last pass by ID  -> RandomAccessFile seek: %.2f microseconds | text-file scan: %.2f ms%n", seekUs, scanMs);
        System.out.printf("Speed-up: about %.0f times%n", scanMs * 1000 / seekUs);
        Files.delete(bin); Files.delete(txt);
    }
    static String scan(Path p, String id) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(p)) {
            String l; while ((l = br.readLine()) != null) if (l.startsWith(id + ",")) return l;
        }
        return null;
    }
}
