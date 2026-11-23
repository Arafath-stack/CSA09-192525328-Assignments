import java.awt.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.*;

/** Drives the real FeePanel with button clicks, measures whether the event-dispatch thread stays responsive, and renders the screens. */
public class FeePanelTest {
    static long maxGapMs;
    /** Heartbeat on the EDT: records the longest time between two ticks. */
    static Timer heartbeat(AtomicLong max) { AtomicLong last = new AtomicLong(System.nanoTime()); Timer t = new Timer(5, e -> { long now = System.nanoTime(); max.accumulateAndGet((now - last.getAndSet(now)) / 1_000_000, Math::max); }); t.start(); return t; }
    static void waitIdle(FeePanel p) throws Exception { long end = System.currentTimeMillis() + 5000; while (p.isBusy() && System.currentTimeMillis() < end) Thread.sleep(10); Thread.sleep(80); }

    public static void main(String[] args) throws Exception {
        InMemoryFeeRepository repo = new InMemoryFeeRepository(); int DELAY = 600;
        FeePanel[] holder = new FeePanel[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new FeePanel(new FeeService(repo, new SlowBankGateway(DELAY, FeeService.MAX_AMOUNT)))); FeePanel p = holder[0];
        SwingUtilities.invokeAndWait(() -> Shot.layoutAll(p));
        System.out.println("== Screens (rendered off-screen from the real component tree)");
        SwingUtilities.invokeAndWait(() -> { try { Shot.render(p, 420, 170, "Student Fee Payment", "shot_1_initial.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        SwingUtilities.invokeAndWait(() -> { p.regNo.setText("19231100X"); p.amount.setText("500"); p.pay.doClick(); });
        System.out.println("invalid register number  -> status: " + p.status.getText());
        SwingUtilities.invokeAndWait(() -> { try { Shot.render(p, 420, 170, "Student Fee Payment", "shot_1_validation_error.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        SwingUtilities.invokeAndWait(() -> { p.regNo.setText("192311001"); p.amount.setText("12500.50"); p.pay.doClick(); });
        Thread.sleep(150);
        System.out.println("while the bank is working -> status: " + p.status.getText() + " | Pay button enabled: " + p.pay.isEnabled());
        SwingUtilities.invokeAndWait(() -> { try { Shot.render(p, 420, 170, "Student Fee Payment", "shot_1_processing.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        waitIdle(p);
        System.out.println("after the bank answered   -> status: " + p.status.getText() + " | rows stored: " + repo.size());
        SwingUtilities.invokeAndWait(() -> { try { Shot.render(p, 420, 170, "Student Fee Payment", "shot_1_success.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        SwingUtilities.invokeAndWait(() -> p.receipt.doClick());
        System.out.println("Receipt button            -> dialog text: " + UiMessages.recorded.get(UiMessages.recorded.size() - 1));
        SwingUtilities.invokeAndWait(() -> { p.clear.doClick(); });
        System.out.println("Clear button              -> register field = '" + p.regNo.getText() + "', amount = '" + p.amount.getText() + "'");

        System.out.println("\n== Is the window responsive? Longest gap between two EDT heartbeats (ms), bank delay = " + DELAY + " ms");
        // (1) original design: the sleep runs inside the event handler, i.e. ON the event-dispatch thread
        AtomicLong bad = new AtomicLong(); Timer[] t1 = new Timer[1];
        SwingUtilities.invokeAndWait(() -> t1[0] = heartbeat(bad)); Thread.sleep(100); bad.set(0);
        SwingUtilities.invokeAndWait(() -> { try { Thread.sleep(DELAY); } catch (InterruptedException e) { } });        // what the original actionPerformed did
        Thread.sleep(100); t1[0].stop();
        // (2) redesign: the same wait happens in a SwingWorker
        AtomicLong good = new AtomicLong(); Timer[] t2 = new Timer[1];
        SwingUtilities.invokeAndWait(() -> t2[0] = heartbeat(good)); Thread.sleep(100); good.set(0);
        SwingUtilities.invokeAndWait(() -> { p.regNo.setText("192311004"); p.amount.setText("300"); p.pay.doClick(); });
        waitIdle(p); t2[0].stop();
        System.out.printf("original (sleep inside actionPerformed) : %5d ms   -> window frozen for the whole wait%n", bad.get());
        System.out.printf("redesign (SwingWorker)                   : %5d ms   -> window stays responsive%n", good.get());
        System.exit(0);
    }
}
