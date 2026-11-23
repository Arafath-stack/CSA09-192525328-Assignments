import java.io.*;
import java.nio.file.*;
import java.util.List;

/** A2 / B1.3 - four ways to count lines in a file. */
public class LineCounter {

    /** Counts calls that reach the underlying FileInputStream (i.e. calls into the OS). */
    static class CountingStream extends FilterInputStream {
        long calls = 0;
        CountingStream(InputStream in) { super(in); }
        @Override public int read() throws IOException { calls++; return super.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException { calls++; return super.read(b, off, len); }
    }

    // 1. One byte per read() call (the slow program of Task A2)
    static long unbuffered(Path p, long[] calls) throws IOException {
        long lines = 0, c = 0; int b;
        try (FileInputStream in = new FileInputStream(p.toFile())) {
            while ((b = in.read()) != -1) { c++; if (b == '\n') lines++; }
            c++; // final read that returned -1
        }
        calls[0] = c;
        return lines;
    }

    // 2. BufferedInputStream (8 KB default buffer) but still reading byte by byte from the program's side
    static long bufferedStream(Path p, long[] calls) throws IOException {
        long lines = 0; int b;
        try (CountingStream cs = new CountingStream(new FileInputStream(p.toFile()));
             BufferedInputStream in = new BufferedInputStream(cs)) {
            while ((b = in.read()) != -1) if (b == '\n') lines++;
            calls[0] = cs.calls;
        }
        return lines;
    }

    // 3. Own byte[] buffer of 64 KB
    static long byteArray(Path p, long[] calls) throws IOException {
        long lines = 0; byte[] buf = new byte[64 * 1024]; int n;
        try (CountingStream cs = new CountingStream(new FileInputStream(p.toFile()))) {
            while ((n = cs.read(buf)) != -1) for (int i = 0; i < n; i++) if (buf[i] == '\n') lines++;
            calls[0] = cs.calls;
        }
        return lines;
    }

    // 4. BufferedReader.readLine()
    static long bufferedReader(Path p) throws IOException {
        long lines = 0;
        try (BufferedReader br = Files.newBufferedReader(p)) { while (br.readLine() != null) lines++; }
        return lines;
    }

    // 5. Alternative concept: read everything into memory at once
    static long readAllLines(Path p) throws IOException {
        List<String> all = Files.readAllLines(p);
        return all.size();
    }

    /** Creates a file of about 'mb' megabytes made of attendance-log lines. */
    static Path generate(Path dir, int mb) throws IOException {
        Path p = dir.resolve("attendance_" + mb + "MB.log");
        long target = mb * 1024L * 1024L;
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            long written = 0; int i = 0;
            while (written < target) {
                String line = "2026-11-" + String.format("%02d", 1 + i % 28) + ",19231" + String.format("%04d", i % 10000) + ",PRESENT";
                w.write(line); w.write('\n');
                written += line.length() + 1; i++;
            }
        }
        return p;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("attlog");
        int[] sizes = {1, 10, 50};
        System.out.println("size_MB,bytes,lines,method,read_calls,time_ms,memory_note");
        for (int mb : sizes) {
            Path p = generate(dir, mb);
            long bytes = Files.size(p);
            long[] c = new long[1];
            long t, lines;

            t = System.nanoTime(); lines = unbuffered(p, c);
            System.out.printf("%d,%d,%d,unbuffered FileInputStream.read(),%d,%.1f,~0%n", mb, bytes, lines, c[0], (System.nanoTime()-t)/1e6);

            t = System.nanoTime(); lines = bufferedStream(p, c);
            System.out.printf("%d,%d,%d,BufferedInputStream(8KB),%d,%.1f,8 KB%n", mb, bytes, lines, c[0], (System.nanoTime()-t)/1e6);

            t = System.nanoTime(); lines = byteArray(p, c);
            System.out.printf("%d,%d,%d,byte[64KB] buffer,%d,%.1f,64 KB%n", mb, bytes, lines, c[0], (System.nanoTime()-t)/1e6);

            t = System.nanoTime(); lines = bufferedReader(p);
            System.out.printf("%d,%d,%d,BufferedReader.readLine(),n/a,%.1f,8K chars%n", mb, bytes, lines, (System.nanoTime()-t)/1e6);

            Runtime rt = Runtime.getRuntime(); System.gc();
            long before = rt.totalMemory() - rt.freeMemory();
            t = System.nanoTime();
            List<String> all = Files.readAllLines(p);
            double ms = (System.nanoTime()-t)/1e6;
            long after = rt.totalMemory() - rt.freeMemory();
            System.out.printf("%d,%d,%d,Files.readAllLines(),n/a,%.1f,~%d MB heap%n", mb, bytes, all.size(), ms, Math.max(0,(after-before))/1024/1024);
            all = null;
        }
    }
}
