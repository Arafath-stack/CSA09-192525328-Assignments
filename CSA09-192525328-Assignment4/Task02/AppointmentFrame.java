import java.awt.event.*;
import javax.swing.*;

/** The top-level window: close confirmation and a window listener that prints EVERY window event (for the Task 2.4 trace). */
public class AppointmentFrame extends JFrame {
    public AppointmentFrame() {
        super("Outpatient Appointment Desk");
        AppointmentPanel panel = new AppointmentPanel(new AppointmentService(new SampleDirectory()));
        setContentPane(panel);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);                       // we decide in windowClosing
        addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e)      { System.out.println("windowOpened"); }
            public void windowActivated(WindowEvent e)   { System.out.println("windowActivated"); }
            public void windowDeactivated(WindowEvent e) { System.out.println("windowDeactivated"); }
            public void windowIconified(WindowEvent e)   { System.out.println("windowIconified"); }
            public void windowDeiconified(WindowEvent e) { System.out.println("windowDeiconified"); }
            public void windowClosing(WindowEvent e) {
                System.out.println("windowClosing");
                if (panel.isBookingInProgress() && JOptionPane.showConfirmDialog(AppointmentFrame.this, "A booking is in progress. Close anyway?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                dispose();                                                    // -> windowClosed
            }
            public void windowClosed(WindowEvent e)      { System.out.println("windowClosed"); System.exit(0); }
        });
        pack();
    }
    public static void main(String[] args) { System.out.println("main -> constructor runs"); SwingUtilities.invokeLater(() -> { AppointmentFrame f = new AppointmentFrame(); System.out.println("setVisible(true)"); f.setVisible(true); }); }
}
