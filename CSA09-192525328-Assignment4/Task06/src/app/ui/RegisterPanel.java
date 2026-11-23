package app.ui;

import app.model.Event;
import app.service.RegistrationService;
import java.awt.*;
import javax.swing.*;

/** Screen 1: the registration form. Collects input, shows results; the database work runs in a SwingWorker. */
public class RegisterPanel extends JPanel {
    final JTextField regNo = new JTextField(14), name = new JTextField(20), email = new JTextField(20);
    final JComboBox<Event> event = new JComboBox<>();
    final JButton register = new JButton("Register"), clear = new JButton("Clear");
    final JLabel status = new JLabel(" ");
    private final RegistrationService service; private final Runnable onRegistered; private volatile boolean busy;

    public RegisterPanel(RegistrationService service, Runnable onRegistered) {
        super(new BorderLayout(8, 8)); this.service = service; this.onRegistered = onRegistered;
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel title = new JLabel("New event registration"); title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JPanel form = new JPanel(new GridLayout(4, 2, 6, 8));
        form.add(new JLabel("Register number")); form.add(regNo); form.add(new JLabel("Student name")); form.add(name);
        form.add(new JLabel("E-mail")); form.add(email); form.add(new JLabel("Event")); form.add(event);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4)); buttons.add(register); buttons.add(clear);
        JPanel south = new JPanel(new BorderLayout()); south.add(buttons, BorderLayout.NORTH); south.add(status, BorderLayout.SOUTH);
        add(title, BorderLayout.NORTH); add(form, BorderLayout.CENTER); add(south, BorderLayout.SOUTH);
        register.addActionListener(e -> onRegister());
        clear.addActionListener(e -> onClear());
        loadEvents();
    }
    private void loadEvents() {
        new SwingWorker<java.util.List<Event>, Void>() {
            @Override protected java.util.List<Event> doInBackground() throws Exception { return service.events(); }
            @Override protected void done() { try { event.addItem(null); for (Event e : get()) event.addItem(e); } catch (Exception ex) { show("Could not load events: " + ex.getMessage(), true); } }
        }.execute();
    }
    public boolean isBusy() { return busy; }

    private void onRegister() {
        Event ev = (Event) event.getSelectedItem();
        String error = service.validate(regNo.getText(), name.getText(), email.getText(), ev);          // validation on the EDT (cheap)
        if (error != null) { show(error, true); return; }
        busy = true; register.setEnabled(false); show("Saving...", false);
        new SwingWorker<RegistrationService.Result, Void>() {
            @Override protected RegistrationService.Result doInBackground() { return service.register(regNo.getText(), name.getText(), email.getText(), ev); }   // duplicate check + INSERT: background
            @Override protected void done() {
                try {
                    RegistrationService.Result r = get(); show(r.message(), !r.ok());
                    if (r.ok()) { onClear0(); onRegistered.run(); }
                    else if (r.message().contains("already registered")) UiMessages.show(RegisterPanel.this, "Duplicate registration", r.message(), JOptionPane.WARNING_MESSAGE);
                } catch (Exception ex) { show("Unexpected error: " + ex.getMessage(), true); }
                finally { busy = false; register.setEnabled(true); }
            }
        }.execute();
    }
    private void onClear() { onClear0(); show(" ", false); }
    private void onClear0() { regNo.setText(""); name.setText(""); email.setText(""); event.setSelectedIndex(0); }
    private void show(String text, boolean error) { status.setText(text); status.setForeground(error ? new Color(176, 0, 32) : new Color(20, 110, 40)); }
}
