package app.ui;

import app.service.RegistrationService;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import javax.swing.*;

/** Main window content: menu bar + two screens in a CardLayout. */
public class MainPanel extends JPanel {
    static final String REGISTER = "register", VIEW = "view";
    final CardLayout cards = new CardLayout(); final JPanel deck = new JPanel(cards);
    final RegisterPanel registerPanel; final RegistrationsPanel viewPanel;
    public final JMenuBar menuBar = new JMenuBar(); final JMenuItem exportItem = new JMenuItem("Export CSV..."), exitItem = new JMenuItem("Exit"),
            newItem = new JMenuItem("New registration"), listItem = new JMenuItem("Registrations"), aboutItem = new JMenuItem("About");
    private final RegistrationService service; private volatile boolean exporting; String lastExportMessage = "";

    public MainPanel(RegistrationService service) {
        super(new BorderLayout()); this.service = service;
        viewPanel = new RegistrationsPanel(service); registerPanel = new RegisterPanel(service, () -> viewPanel.loadPage());
        deck.add(registerPanel, REGISTER); deck.add(viewPanel, VIEW); add(deck, BorderLayout.CENTER);
        JMenu file = new JMenu("File"), view = new JMenu("View"), help = new JMenu("Help");
        file.add(exportItem); file.addSeparator(); file.add(exitItem); view.add(newItem); view.add(listItem); help.add(aboutItem);
        menuBar.add(file); menuBar.add(view); menuBar.add(help);
        newItem.addActionListener(e -> cards.show(deck, REGISTER));
        listItem.addActionListener(e -> { cards.show(deck, VIEW); viewPanel.loadPage(); });
        exportItem.addActionListener(e -> chooseAndExport());
        exitItem.addActionListener(e -> { Window w = SwingUtilities.getWindowAncestor(this); if (w != null) w.dispose(); });
        aboutItem.addActionListener(e -> UiMessages.show(this, "About", "Campus Event Registration - Swing + JDBC", JOptionPane.INFORMATION_MESSAGE));
    }
    public void showScreen(String name) { cards.show(deck, name); }
    public boolean isExporting() { return exporting; }

    private void chooseAndExport() {
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("registrations.csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) exportTo(fc.getSelectedFile());
    }
    /** Writes the registrations of the event currently selected in the filter, on a worker thread (the file can be large). */
    public void exportTo(File file) {
        if (exporting) return; exporting = true; int ev = viewPanel.selectedEventId();
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception { try (Writer w = new FileWriter(file)) { return service.exportCsv(ev, w); } }
            @Override protected void done() {
                try { lastExportMessage = "Exported " + get() + " registrations to " + file.getName(); } catch (Exception ex) { lastExportMessage = "Export failed: " + ex.getMessage(); }
                UiMessages.show(MainPanel.this, "Export", lastExportMessage, JOptionPane.INFORMATION_MESSAGE); exporting = false;
            }
        }.execute();
    }
}
