package app.ui;

import app.dao.InMemoryRegistrationDao;
import app.dao.JdbcRegistrationDao;
import app.dao.RegistrationDao;
import app.service.RegistrationService;
import javax.swing.*;

/**
 * Entry point.
 *   java -cp out app.ui.MainFrame                                   -> in-memory demo data
 *   java -cp out:mysql-connector-j.jar app.ui.MainFrame mysql jdbc:mysql://localhost:3306/campus_events USER PASSWORD
 */
public class MainFrame {
    public static void main(String[] args) {
        RegistrationDao dao = (args.length >= 4 && args[0].equals("mysql")) ? new JdbcRegistrationDao(args[1], args[2], args[3]) : new InMemoryRegistrationDao().seed(45);
        SwingUtilities.invokeLater(() -> {
            MainPanel panel = new MainPanel(new RegistrationService(dao));
            JFrame f = new JFrame("Campus Event Registration");
            f.setJMenuBar(panel.menuBar); f.setContentPane(panel); f.setSize(760, 480); f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); f.setVisible(true);
        });
    }
}
