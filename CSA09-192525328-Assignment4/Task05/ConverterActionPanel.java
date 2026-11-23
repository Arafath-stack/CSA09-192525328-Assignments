import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Task 5 alternative concept: ONE shared Action used by a button, a menu item and a keyboard shortcut (plus a method reference). */
public class ConverterActionPanel extends JPanel {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" ");
    final JButton button, methodRefButton = new JButton("Convert (method reference)");
    final JMenuBar menuBar = new JMenuBar();
    final JMenuItem menuItem;
    final Action convertAction = new AbstractAction("Convert") {
        { putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK)); }
        @Override public void actionPerformed(ActionEvent e) { result.setText(convertText()); }
    };

    String convertText() {
        try { double v = Double.parseDouble(amount.getText().trim()); String c = (String) target.getSelectedItem(); return String.format(java.util.Locale.US, "%.2f %s", v * (c.equals("USD") ? 0.012 : 0.011), c); }
        catch (NumberFormatException ex) { return "Invalid amount"; }
    }
    private void onConvert(ActionEvent e) { result.setText(convertText()); }          // target of the method reference

    public ConverterActionPanel() {
        super(new BorderLayout());
        button = new JButton(convertAction);                       // trigger 1: button
        JMenu m = new JMenu("Actions"); menuItem = new JMenuItem(convertAction); m.add(menuItem); menuBar.add(m);   // trigger 2: menu item
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) convertAction.getValue(Action.ACCELERATOR_KEY), "convert");  // trigger 3: Ctrl+Enter
        getActionMap().put("convert", convertAction);
        amount.addActionListener(convertAction);                   // trigger 4: Enter in the field
        methodRefButton.addActionListener(this::onConvert);       // method reference
        JPanel row = new JPanel(new FlowLayout());
        row.add(new JLabel("INR")); row.add(amount); row.add(target); row.add(button); row.add(methodRefButton); row.add(result);
        add(menuBar, BorderLayout.NORTH); add(row, BorderLayout.CENTER);
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { JFrame f = new JFrame("Shared Action"); f.setContentPane(new ConverterActionPanel()); f.pack(); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); f.setVisible(true); }); }
}
