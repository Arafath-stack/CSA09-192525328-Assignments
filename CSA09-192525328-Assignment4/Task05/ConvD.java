import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ConvD extends JFrame {
    private final JTextField amount = new JTextField(10);
    private final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    private final JLabel result = new JLabel(" ");
    private final JButton convertButton = new JButton("Convert");

    private static double rate(String currency) { return currency.equals("USD") ? 0.012 : 0.011; }

    /** Pure conversion: returns the text to show. */
    private String convert() {
        try {
            double inr = Double.parseDouble(amount.getText().trim());
            String cur = (String) target.getSelectedItem();
            return String.format(java.util.Locale.US, "%.2f %s", inr * rate(cur), cur);
        } catch (NumberFormatException ex) {
            return "Invalid amount";
        }
    }

    public ConvD() {
        super("(d) lambda expression");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convertButton); add(result);
        convertButton.addActionListener(e -> result.setText(convert()));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ConvD().setVisible(true)); }
}
