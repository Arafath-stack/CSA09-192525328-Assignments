import java.io.*;
import java.nio.file.*;
import java.util.*;

/** A4 alternative: Java serialization vs CSV vs hand-written JSON for n residents. */
public class FormatBench {
    static class R implements Serializable { private static final long serialVersionUID = 1L;
        String name, regNo, phone; R(String n, String r, String p) { name = n; regNo = r; phone = p; } }

    public static void main(String[] args) throws Exception {
        System.out.println("n,format,file_bytes,save_ms,load_ms");
        for (int n : new int[]{1000, 10000, 100000}) {
            List<R> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(new R("Student " + i, "19231" + String.format("%04d", i % 10000), "98" + String.format("%08d", i)));
            Path ser = Files.createTempFile("h", ".ser"), csv = Files.createTempFile("h", ".csv"), json = Files.createTempFile("h", ".json");
            for (int warm = 0; warm < 2; warm++) { saveSer(list, ser); loadSer(ser); saveCsv(list, csv); loadCsv(csv); saveJson(list, json); loadJson(json); }
            long t = System.nanoTime(); saveSer(list, ser); double s1 = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); loadSer(ser); double l1 = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); saveCsv(list, csv); double s2 = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); loadCsv(csv); double l2 = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); saveJson(list, json); double s3 = (System.nanoTime() - t) / 1e6;
            t = System.nanoTime(); loadJson(json); double l3 = (System.nanoTime() - t) / 1e6;
            System.out.printf("%d,Java serialization,%d,%.1f,%.1f%n", n, Files.size(ser), s1, l1);
            System.out.printf("%d,CSV,%d,%.1f,%.1f%n", n, Files.size(csv), s2, l2);
            System.out.printf("%d,JSON,%d,%.1f,%.1f%n", n, Files.size(json), s3, l3);
            Files.delete(ser); Files.delete(csv); Files.delete(json);
        }
    }
    static void saveSer(List<R> l, Path p) throws IOException { try (ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(p)))) { o.writeObject(l); } }
    @SuppressWarnings("unchecked") static List<R> loadSer(Path p) throws Exception { try (ObjectInputStream i = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(p)))) { return (List<R>) i.readObject(); } }
    static void saveCsv(List<R> l, Path p) throws IOException { try (BufferedWriter w = Files.newBufferedWriter(p)) { for (R r : l) { w.write(r.name + "," + r.regNo + "," + r.phone); w.newLine(); } } }
    static List<R> loadCsv(Path p) throws IOException { List<R> o = new ArrayList<>(); try (BufferedReader b = Files.newBufferedReader(p)) { String s; while ((s = b.readLine()) != null) { String[] f = s.split(",", -1); o.add(new R(f[0], f[1], f.length > 2 ? f[2] : null)); } } return o; }
    static void saveJson(List<R> l, Path p) throws IOException { try (BufferedWriter w = Files.newBufferedWriter(p)) { w.write("[\n"); for (int i = 0; i < l.size(); i++) { R r = l.get(i);
        w.write("  {\"name\":\"" + r.name + "\",\"regNo\":\"" + r.regNo + "\",\"phone\":\"" + r.phone + "\"}" + (i < l.size() - 1 ? ",\n" : "\n")); } w.write("]\n"); } }
    static List<R> loadJson(Path p) throws IOException { List<R> o = new ArrayList<>(); try (BufferedReader b = Files.newBufferedReader(p)) { String s;
        while ((s = b.readLine()) != null) { s = s.trim(); if (!s.startsWith("{")) continue; String[] kv = s.replaceAll("[{}\"]", "").replaceAll(",$", "").split(",");
            o.add(new R(kv[0].substring(5), kv[1].substring(6), kv[2].substring(6))); } } return o; }
}
