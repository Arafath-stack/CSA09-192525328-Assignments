package app.ui;

import app.model.Event;
import app.model.Registration;
import app.service.RegistrationService;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Screen 2: registrations in a JTable, filtered by event, loaded ONE PAGE AT A TIME by a SwingWorker. */
public class RegistrationsPanel extends JPanel {
    final JComboBox<Event> filter = new JComboBox<>();
    final DefaultTableModel model = new DefaultTableModel(new Object[]{"Register No", "Name", "E-mail", "Event", "Registered at"}, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    final JTable table = new JTable(model);
    final JButton prev = new JButton("< Prev"), next = new JButton("Next >"), refresh = new JButton("Refresh");
    final JLabel pageLabel = new JLabel(" "), status = new JLabel(" ");
    private final RegistrationService service; private int pageIndex; private int pages = 1; private volatile boolean busy, populating;

    public RegistrationsPanel(RegistrationService service) {
        super(new BorderLayout(6, 6)); this.service = service;
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT)); top.add(new JLabel("Filter by event:")); top.add(filter); top.add(refresh);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2)); nav.add(prev); nav.add(pageLabel); nav.add(next);
        JPanel south = new JPanel(new BorderLayout()); south.add(nav, BorderLayout.NORTH); south.add(status, BorderLayout.SOUTH);
        table.setAutoCreateRowSorter(false); table.setFillsViewportHeight(true);
        add(top, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER); add(south, BorderLayout.SOUTH);
        filter.addActionListener(e -> { if (!populating) { pageIndex = 0; loadPage(); } });
        prev.addActionListener(e -> { if (pageIndex > 0) { pageIndex--; loadPage(); } });
        next.addActionListener(e -> { if (pageIndex < pages - 1) { pageIndex++; loadPage(); } });
        refresh.addActionListener(e -> loadPage());
        populating = true; filter.addItem(Event.ALL);
        new SwingWorker<java.util.List<Event>, Void>() {
            @Override protected java.util.List<Event> doInBackground() throws Exception { return service.events(); }
            @Override protected void done() { try { for (Event e : get()) filter.addItem(e); } catch (Exception ex) { status.setText("Could not load events: " + ex.getMessage()); } finally { populating = false; } }
        }.execute();
    }
    public boolean isBusy() { return busy; }
    public int selectedEventId() { Event e = (Event) filter.getSelectedItem(); return e == null ? 0 : e.id(); }

    /** Fetches the page on a worker thread; only the final model update runs on the EDT. */
    public void loadPage() {
        if (busy) return;
        busy = true; setControls(false); status.setText("Loading..."); status.setForeground(Color.DARK_GRAY);
        final int ev = selectedEventId(), idx = pageIndex;
        new SwingWorker<RegistrationService.Page, Void>() {
            @Override protected RegistrationService.Page doInBackground() throws Exception { return service.page(ev, idx); }          // COUNT + SELECT ... LIMIT ? OFFSET ?
            @Override protected void done() {
                try {
                    RegistrationService.Page p = get(); pageIndex = p.pageIndex(); pages = p.pages();
                    model.setRowCount(0); DateTimeFormatter f = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                    for (Registration r : p.rows()) model.addRow(new Object[]{r.regNo(), r.name(), r.email(), r.eventName(), r.registeredAt().format(f)});
                    pageLabel.setText("Page " + (pageIndex + 1) + " of " + pages + "  (" + p.total() + " registrations)"); status.setText("Showing " + p.rows().size() + " rows");
                } catch (Exception ex) { status.setText("Could not load registrations: " + ex.getMessage()); status.setForeground(new Color(176, 0, 32)); }
                finally { busy = false; setControls(true); }
            }
        }.execute();
    }
    private void setControls(boolean on) { prev.setEnabled(on); next.setEnabled(on); refresh.setEnabled(on); filter.setEnabled(on); }
}
