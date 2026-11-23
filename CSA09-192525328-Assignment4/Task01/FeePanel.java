import java.awt.*;
import javax.swing.*;

/**
 * The window only COLLECTS INPUT and SHOWS RESULTS. Every button has its own small handler method; the payment itself
 * runs in a SwingWorker (background thread) so the event-dispatch thread is never blocked.
 */
public class FeePanel extends JPanel {
    final JTextField regNo = new JTextField(12), amount = new JTextField(10);
    final JButton pay = new JButton("Pay"), clear = new JButton("Clear"), receipt = new JButton("Receipt");
    final JLabel status = new JLabel(" ");
    private final FeeService service; private volatile boolean busy;

    public FeePanel(FeeService service) {
        super(new BorderLayout(8, 8)); this.service = service;
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.add(new JLabel("Register no.")); form.add(regNo); form.add(new JLabel("Amount (Rs)")); form.add(amount);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4)); buttons.add(pay); buttons.add(clear); buttons.add(receipt);
        add(form, BorderLayout.NORTH); add(buttons, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        pay.addActionListener(e -> onPay());
        clear.addActionListener(e -> onClear());
        receipt.addActionListener(e -> onReceipt());
    }
    public boolean isBusy() { return busy; }

    private void onPay() {
        String error = service.validate(regNo.getText(), amount.getText());      // cheap: stays on the EDT
        if (error != null) { show(error, true); return; }
        setBusy(true); show("Processing payment, please wait...", false);
        new SwingWorker<FeeService.Result, Void>() {
            @Override protected FeeService.Result doInBackground() { return service.pay(regNo.getText(), amount.getText()); }   // bank wait + database: background thread
            @Override protected void done() {                                                                                      // back on the EDT
                try { FeeService.Result r = get(); show(r.message(), !r.ok()); if (r.ok()) amount.setText(""); }
                catch (Exception ex) { show("Unexpected error: " + ex.getMessage(), true); }
                finally { setBusy(false); }
            }
        }.execute();
    }
    private void onClear() { regNo.setText(""); amount.setText(""); show(" ", false); }
    private void onReceipt() { UiMessages.info(this, "Fee receipt", service.receipt(regNo.getText())); }

    private void setBusy(boolean b) { busy = b; pay.setEnabled(!b); clear.setEnabled(!b); receipt.setEnabled(!b); regNo.setEnabled(!b); amount.setEnabled(!b); }
    private void show(String text, boolean error) { status.setText(text); status.setForeground(error ? new Color(176, 0, 32) : new Color(20, 110, 40)); }

    public static void main(String[] args) {
        FeeRepository repo = args.length >= 3 ? new JdbcFeeRepository(args[0], args[1], args[2]) : new InMemoryFeeRepository();
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Student Fee Payment");
            f.setContentPane(new FeePanel(new FeeService(repo, new SlowBankGateway(1500, FeeService.MAX_AMOUNT))));
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.addWindowListener(new java.awt.event.WindowAdapter() { @Override public void windowClosing(java.awt.event.WindowEvent e) { f.dispose(); } });
            f.pack(); f.setVisible(true);
        });
    }
}
