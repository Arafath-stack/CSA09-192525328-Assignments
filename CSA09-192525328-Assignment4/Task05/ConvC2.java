import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ConvC2 extends JFrame {
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

    public ConvC2() {
        super("(c) anonymous inner class");
        setLayout(new FlowLayout());
        add(new JLabel("INR")); add(amount); add(target); add(convertButton); add(result);
        ActionListener convertAction = new ActionListener() {       // an anonymous class cannot be registered twice
            @Override public void actionPerformed(ActionEvent e) {   // without naming it first
                String text = convert();
                result.setText(text);
                JOptionPane.showMessageDialog(ConvC2.this, text);
            }
        };
        convertButton.addActionListener(convertAction);
        amount.addActionListener(convertAction);                    // Enter in the field
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        pack();
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new ConvC2().setVisible(true)); }
}
