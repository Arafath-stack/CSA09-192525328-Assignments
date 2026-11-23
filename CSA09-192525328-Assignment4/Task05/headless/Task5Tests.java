import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/** Drives the twins (JPanel versions of the real JFrame classes) with the same events a user would cause. */
public class Task5Tests {
    @SuppressWarnings("unchecked") static <T extends Component> T find(Container c, Class<T> t, int index) {
        List<T> hits = new ArrayList<>(); collect(c, t, hits); return hits.get(index < 0 ? hits.size() + index : index);
    }
    @SuppressWarnings("unchecked") static <T extends Component> void collect(Container c, Class<T> t, List<T> out) { for (Component k : c.getComponents()) { if (t.isInstance(k)) out.add((T) k); if (k instanceof Container kc) collect(kc, t, out); } }
    static JButton btn(Container c) { List<JButton> l = new ArrayList<>(); collect(c, JButton.class, l); for (JButton b : l) if ("Convert".equals(b.getText())) return b; throw new IllegalStateException("no Convert button"); }
    static int pass, fail;
    static void check(String id, String what, String expected, String actual) { boolean ok = expected.equals(actual); if (ok) pass++; else fail++; System.out.printf("  %-4s %-46s expected=%-16s actual=%-16s %s%n", id, what, "'" + expected + "'", "'" + actual + "'", ok ? "PASS" : "FAIL"); }

    static void run(String label, JPanel p, boolean afterChange) {
        System.out.println("== " + label);
        JTextField amount = find(p, JTextField.class, 0); JComboBox<?> cur = find(p, JComboBox.class, 0); JButton b = btn(p); JLabel res = find(p, JLabel.class, -1);
        Twin.messages.clear();
        amount.setText("1000"); cur.setSelectedItem("USD"); b.doClick(); check("T1", "1000 INR to USD via button", "12.00 USD", res.getText());
        cur.setSelectedItem("EUR"); b.doClick(); check("T2", "1000 INR to EUR via button", "11.00 EUR", res.getText());
        amount.setText("abc"); b.doClick(); check("T3", "invalid amount 'abc'", "Invalid amount", res.getText());
        amount.setText(""); b.doClick(); check("T4", "empty amount", "Invalid amount", res.getText());
        amount.setText("250"); cur.setSelectedItem("USD"); res.setText(" "); amount.postActionEvent();      // Enter key in the field
        check("T5", "Enter in the amount field", afterChange ? "3.00 USD" : " ", res.getText());
        check("T6", "dialog box shows the result", afterChange ? "[5 messages recorded, last = 3.00 USD]" : "[0 messages recorded, last = -]",
              "[" + Twin.messages.size() + " messages recorded, last = " + (Twin.messages.isEmpty() ? "-" : Twin.messages.get(Twin.messages.size() - 1)) + "]");
    }
    static Set<Object> listenerObjects(Container p) { Set<Object> s = Collections.newSetFromMap(new IdentityHashMap<>()); s.addAll(Arrays.asList(btn(p).getActionListeners())); s.addAll(Arrays.asList(find(p, JTextField.class, 0).getActionListeners())); return s; }

    public static void main(String[] args) throws Exception {
        run("Style (a) frame implements ActionListener - original requirement", new ConvAT(), false);
        run("Style (b) named inner class - original requirement", new ConvBT(), false);
        run("Style (c) anonymous inner class - original requirement", new ConvCT(), false);
        run("Style (d) lambda - original requirement", new ConvDT(), false);
        run("Style (a) after the change (Enter + dialog)", new ConvA2T(), true);
        run("Style (b) after the change (Enter + dialog)", new ConvB2T(), true);
        run("Style (c) after the change (Enter + dialog)", new ConvC2T(), true);
        run("Style (d) after the change (Enter + dialog)", new ConvD2T(), true);
        System.out.println("\n== Complete-the-Code program Converter (lambda, Enter key included, result in the label)");
        ConverterT c = new ConverterT(); JTextField amount = find(c, JTextField.class, 0); JButton b = btn(c); JLabel res = find(c, JLabel.class, -1); JComboBox<?> cur = find(c, JComboBox.class, 0);
        amount.setText("1000"); cur.setSelectedItem("USD"); b.doClick(); check("C1", "valid amount, button, USD", "12.00 USD", res.getText());
        Shot.render(c, c.getPreferredSize().width + 40, 70, "Currency Converter", "shot_5_converter_valid.png");
        amount.setText("12x"); amount.postActionEvent(); check("C2", "invalid amount, Enter key", "Invalid amount", res.getText());
        Shot.render(c, c.getPreferredSize().width + 40, 70, "Currency Converter", "shot_5_converter_invalid.png");
        amount.setText("500"); cur.setSelectedItem("EUR"); amount.postActionEvent(); check("C3", "valid amount, Enter key, EUR", "5.50 EUR", res.getText());
        System.out.println("\n== Listener objects (distinct) registered on the button and the field");
        for (JPanel p : new JPanel[]{new ConvAT(), new ConvBT(), new ConvCT(), new ConvDT(), new ConvA2T(), new ConvB2T(), new ConvC2T(), new ConvD2T()})
            System.out.printf("  %-8s distinct listener objects: %d   (registrations: button %d + field %d)%n", p.getClass().getSimpleName().replace("T", ""), listenerObjects(p).size(), btn(p).getActionListeners().length, find(p, JTextField.class, 0).getActionListeners().length);
        System.out.println("\n== Shared Action (alternative concept)");
        ConverterActionPanel sp = new ConverterActionPanel(); sp.amount.setText("1000"); sp.target.setSelectedItem("USD");
        sp.button.doClick(); String r1 = sp.result.getText(); sp.result.setText(" "); sp.menuItem.doClick(); String r2 = sp.result.getText(); sp.result.setText(" ");
        sp.getActionMap().get("convert").actionPerformed(new java.awt.event.ActionEvent(sp, 0, "convert")); String r3 = sp.result.getText(); sp.result.setText(" "); sp.amount.postActionEvent(); String r4 = sp.result.getText(); sp.result.setText(" "); sp.methodRefButton.doClick(); String r5 = sp.result.getText();
        System.out.println("  button -> " + r1 + " | menu item -> " + r2 + " | Ctrl+Enter -> " + r3 + " | Enter in field -> " + r4 + " | method reference -> " + r5);
        System.out.println("  one Action object serves 4 triggers; the button holds " + sp.button.getActionListeners().length + " listener(s), the menu item " + sp.menuItem.getActionListeners().length + ", the field " + sp.amount.getActionListeners().length);
        Shot.render(sp, 640, 90, "Shared Action", "shot_5_shared_action.png");
        System.out.println("\nTotal checks: " + (pass + fail) + "   passed: " + pass + "   failed: " + fail);
    }
}
