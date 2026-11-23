import java.util.*;
import javax.swing.*;

/** Executes the booking scenario on the real panel and prints the events in the order the handlers received them. */
public class AppointmentTrace {
    static void edt(Runnable r) throws Exception { SwingUtilities.invokeAndWait(r); }
    static int pass, fail;
    static void check(String id, String what, Object expected, Object actual) { boolean ok = String.valueOf(expected).equals(String.valueOf(actual)); if (ok) pass++; else fail++; System.out.printf("%-4s %-58s expected=%-44s actual=%-44s %s%n", id, what, expected, actual, ok ? "PASS" : "FAIL"); }
    public static void main(String[] args) throws Exception {
        AppointmentPanel[] h = new AppointmentPanel[1]; edt(() -> h[0] = new AppointmentPanel(new AppointmentService(new SampleDirectory()))); AppointmentPanel p = h[0];
        System.out.println("== Event trace: the receptionist books a Cardiology appointment (events in the order they reached the handlers)");
        p.eventLog.clear();
        edt(() -> p.patientId.setText("123456"));
        edt(() -> p.department.setSelectedItem("Cardiology"));
        edt(() -> p.doctor.setSelectedIndex(1));
        edt(() -> p.slot.setSelectedItem("10:00"));
        edt(() -> { try { Shot.render(p, 430, 250, "Outpatient Appointment Desk", "shot_2_filled_form.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        edt(() -> p.book.doClick());
        int n = 1; for (String s : p.eventLog) System.out.println(String.format("%2d. ", n++) + s);
        edt(() -> { try { Shot.render(p, 430, 250, "Outpatient Appointment Desk", "shot_2_booked.png"); } catch (Exception e) { throw new RuntimeException(e); } });
        System.out.println("\n== Tests");
        check("A1", "booking succeeds", "Booked: patient 123456 with Dr. Iyer (Cardiology) at 10:00.", p.status.getText());
        check("A2", "form is cleared after a booking", false, p.isBookingInProgress());
        check("A3", "doctor list for Paediatrics has 4 doctors", 4, new AppointmentService(new SampleDirectory()).doctorsIn("Paediatrics").size());
        edt(() -> p.department.setSelectedItem("Orthopaedics")); check("A4", "doctor list refreshed to the department chosen", "Dr. Khan,Dr. Das", String.join(",", doctors(p)));
        edt(() -> p.department.setSelectedItem("Dermatology")); check("A5", "doctor list refreshed again", "Dr. Bose", String.join(",", doctors(p)));
        edt(() -> { p.patientId.setText("12ab"); p.book.doClick(); }); check("A6", "invalid patient id", "Patient ID must be 6 digits.", p.status.getText());
        edt(() -> { p.patientId.setText("123456"); p.book.doClick(); }); check("A7", "doctor not selected", "Select a doctor.", p.status.getText());
        edt(() -> { p.doctor.setSelectedIndex(0); p.book.doClick(); }); check("A8", "slot not selected", "Select a time slot.", p.status.getText());
        edt(() -> { p.slot.setSelectedItem("10:00"); p.book.doClick(); }); check("A9", "booking succeeds for Dr. Bose", true, p.status.getText().startsWith("Booked"));
        edt(() -> { p.patientId.setText("654321"); p.department.setSelectedItem("Dermatology"); p.doctor.setSelectedIndex(0); p.slot.setSelectedItem("10:00"); p.book.doClick(); }); check("A10", "same doctor and slot twice", "Dr. Bose is already booked at 10:00.", p.status.getText());
        edt(() -> { p.patientId.setText("1"); }); check("A11", "close guard: booking in progress (typed text)", true, p.isBookingInProgress());
        edt(() -> { p.patientId.setText(""); p.department.setSelectedIndex(0); p.slot.setSelectedIndex(-1); }); check("A12", "close guard: nothing started", false, p.isBookingInProgress());
        System.out.println("\nTotal: " + (pass + fail) + "  passed: " + pass + "  failed: " + fail); System.exit(0);
    }
    static List<String> doctors(AppointmentPanel p) { List<String> l = new ArrayList<>(); for (int i = 0; i < p.doctor.getItemCount(); i++) l.add(p.doctor.getItemAt(i).name()); return l; }
}
