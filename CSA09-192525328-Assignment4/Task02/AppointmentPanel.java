import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Hospital outpatient desk window (view + handlers only; rules are in AppointmentService). */
public class AppointmentPanel extends JPanel {
    static final String PLEASE = "-- select --";
    final JTextField patientId = new JTextField(10);
    final JComboBox<String> department = new JComboBox<>();
    final JComboBox<Doctor> doctor = new JComboBox<>();
    final JComboBox<String> slot = new JComboBox<>();
    final JButton book = new JButton("Book appointment");
    final JLabel status = new JLabel(" ");
    final List<String> eventLog = new ArrayList<>();               // records every event that reaches a handler, in order
    private final AppointmentService service; private boolean refilling;

    public AppointmentPanel(AppointmentService service) {
        super(new BorderLayout(8, 8)); this.service = service;
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JPanel form = new JPanel(new GridLayout(4, 2, 6, 6));
        form.add(new JLabel("Patient ID")); form.add(patientId);
        form.add(new JLabel("Department")); form.add(department);
        form.add(new JLabel("Doctor")); form.add(doctor);
        form.add(new JLabel("Time slot")); form.add(slot);
        JPanel south = new JPanel(new BorderLayout(4, 4)); south.add(book, BorderLayout.NORTH); south.add(status, BorderLayout.SOUTH);
        add(form, BorderLayout.CENTER); add(south, BorderLayout.SOUTH);

        department.addItem(PLEASE); for (String d : service.departments()) department.addItem(d);
        for (String s : AppointmentService.SLOTS) slot.addItem(s);
        slot.setSelectedIndex(-1);

        patientId.getDocument().addDocumentListener(new DocumentListener() {                       // source: JTextField's Document
            public void insertUpdate(DocumentEvent e) { eventLog.add("DocumentEvent INSERT on patientId -> DocumentListener.insertUpdate"); }
            public void removeUpdate(DocumentEvent e) { eventLog.add("DocumentEvent REMOVE on patientId -> DocumentListener.removeUpdate"); }
            public void changedUpdate(DocumentEvent e) { }
        });
        department.addItemListener(e -> {                                                          // source: JComboBox, event: ItemEvent
            eventLog.add("ItemEvent " + (e.getStateChange() == ItemEvent.SELECTED ? "SELECTED" : "DESELECTED") + " '" + e.getItem() + "' on department -> ItemListener.itemStateChanged");
            if (e.getStateChange() == ItemEvent.SELECTED) refreshDoctors();
        });
        doctor.addItemListener(e -> { if (!refilling) eventLog.add("ItemEvent " + (e.getStateChange() == ItemEvent.SELECTED ? "SELECTED" : "DESELECTED") + " '" + e.getItem() + "' on doctor -> ItemListener.itemStateChanged"); });
        book.addActionListener(e -> { eventLog.add("ActionEvent '" + e.getActionCommand() + "' on Book button -> ActionListener.actionPerformed"); onBook(); });
    }

    /** (a) refresh the doctor list when the department changes: a Map lookup, no database access. */
    private void refreshDoctors() {
        refilling = true;
        try {
            doctor.removeAllItems();
            String dept = (String) department.getSelectedItem();
            if (dept != null && !dept.equals(PLEASE)) for (Doctor d : service.doctorsIn(dept)) doctor.addItem(d);
            doctor.setSelectedIndex(-1);
        } finally { refilling = false; }
    }
    /** (b) validate and book. */
    private void onBook() {
        String dept = (String) department.getSelectedItem(); if (PLEASE.equals(dept)) dept = null;
        AppointmentService.Result r = service.book(patientId.getText(), dept, (Doctor) doctor.getSelectedItem(), (String) slot.getSelectedItem());
        status.setText(r.message()); status.setForeground(r.ok() ? new Color(20, 110, 40) : new Color(176, 0, 32));
        if (r.ok()) resetForm();
    }
    private void resetForm() { patientId.setText(""); department.setSelectedIndex(0); slot.setSelectedIndex(-1); }

    /** True when the receptionist has started a booking (something typed or selected) and has not finished or cleared it. */
    public boolean isBookingInProgress() { return !patientId.getText().isEmpty() || department.getSelectedIndex() > 0 || slot.getSelectedIndex() >= 0; }

    public static void main(String[] args) { AppointmentFrame.main(args); }
}
