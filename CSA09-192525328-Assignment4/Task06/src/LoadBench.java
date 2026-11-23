import app.dao.*;
import app.model.Registration;
import app.service.RegistrationService;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/** Alternative concept: all 100,000 rows into the JTable at once (on the EDT) versus one page of 20 at a time in a SwingWorker. */
public class LoadBench {
    static long used() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    static void gc() throws Exception { for (int i = 0; i < 3; i++) { System.gc(); Thread.sleep(50); } }
    static Timer heartbeat(AtomicLong max) { AtomicLong last = new AtomicLong(System.nanoTime()); Timer t = new Timer(5, e -> { long now = System.nanoTime(); max.accumulateAndGet((now - last.getAndSet(now)) / 1_000_000, Math::max); }); t.start(); return t; }
    static Object[] row(Registration r, DateTimeFormatter f) { return new Object[]{r.regNo(), r.name(), r.email(), r.eventName(), r.registeredAt().format(f)}; }

    public static void main(String[] args) throws Exception {
        PrintWriter csv = new PrintWriter("load_times.csv"); csv.println("rows_in_database,approach,rep,load_ms,longest_edt_freeze_ms,heap_kb_model");
        System.out.printf("%-10s %-34s %12s %22s %16s%n", "rows", "approach", "load (ms)", "longest EDT freeze (ms)", "model heap (KB)");
        for (int n : new int[]{1_000, 10_000, 100_000}) {
            InMemoryRegistrationDao dao = new InMemoryRegistrationDao().seed(n); RegistrationService svc = new RegistrationService(dao); DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
            for (String ap : new String[]{"load ALL rows on the EDT", "one page (20 rows) per SwingWorker"}) {
                double[] ms = new double[5], fr = new double[5], kb = new double[5];
                for (int rep = -1; rep < 5; rep++) {
                    gc(); DefaultTableModel model = new DefaultTableModel(new Object[]{"Register No", "Name", "E-mail", "Event", "Registered at"}, 0);
                    AtomicLong gap = new AtomicLong(); Timer[] hb = new Timer[1]; SwingUtilities.invokeAndWait(() -> hb[0] = heartbeat(gap)); Thread.sleep(60); gap.set(0); long u0 = used();
                    long t0 = System.nanoTime();
                    if (ap.startsWith("load ALL")) {
                        SwingUtilities.invokeAndWait(() -> { try { for (Registration r : svc.all(0)) model.addRow(row(r, f)); } catch (DaoException e) { throw new RuntimeException(e); } });     // the whole load blocks the EDT
                    } else {
                        SwingWorker<RegistrationService.Page, Void> w = new SwingWorker<>() { @Override protected RegistrationService.Page doInBackground() throws Exception { return svc.page(0, n / RegistrationService.PAGE_SIZE / 2); }       // a page from the middle
                            @Override protected void done() { try { for (Registration r : get().rows()) model.addRow(row(r, f)); } catch (Exception e) { throw new RuntimeException(e); } } };
                        w.execute(); w.get(); SwingUtilities.invokeAndWait(() -> { });
                    }
                    double load = (System.nanoTime() - t0) / 1e6; Thread.sleep(60); long freeze = gap.get(); SwingUtilities.invokeAndWait(() -> hb[0].stop()); gc(); long u1 = used();
                    if (model.getRowCount() == Integer.MIN_VALUE) System.out.print("");
                    if (rep >= 0) { ms[rep] = load; fr[rep] = freeze; kb[rep] = Math.max(0, u1 - u0) / 1024.0; csv.printf("%d,%s,%d,%.2f,%d,%.0f%n", n, ap, rep, load, freeze, kb[rep]); }
                }
                System.out.printf("%-10d %-34s %12.1f %22.1f %16.0f%n", n, ap, Arrays.stream(ms).average().orElse(0), Arrays.stream(fr).average().orElse(0), Arrays.stream(kb).average().orElse(0));
            }
        }
        csv.close(); System.out.println("(data source: in-memory DAO; a real MySQL server adds network and query time to BOTH approaches)"); System.exit(0);
    }
}
