import java.math.BigDecimal;

/** Unit tests of the business logic: no window needed, which is exactly the benefit of the separation. */
public class FeeServiceTest {
    static int pass, fail;
    static void check(String id, String what, Object expected, Object actual) { boolean ok = String.valueOf(expected).equals(String.valueOf(actual)); if (ok) pass++; else fail++; System.out.printf("%-4s %-62s expected=%-34s actual=%-34s %s%n", id, what, expected, actual, ok ? "PASS" : "FAIL"); }
    public static void main(String[] args) throws Exception {
        InMemoryFeeRepository repo = new InMemoryFeeRepository(); FeeService s = new FeeService(repo, new SlowBankGateway(0, new BigDecimal("100000")));
        check("V1", "empty register number", "Enter the register number.", s.validate("", "100"));
        check("V2", "register number with letters", "Register number must be exactly 9 digits.", s.validate("19AB11001", "100"));
        check("V3", "empty amount", "Enter the amount.", s.validate("192311001", " "));
        check("V4", "amount 'abc'", "Amount must be a number, for example 12500.50.", s.validate("192311001", "abc"));
        check("V5", "negative amount", "Amount must be greater than zero.", s.validate("192311001", "-5"));
        check("V6", "zero amount", "Amount must be greater than zero.", s.validate("192311001", "0"));
        check("V7", "three decimal places", "Amount can have at most 2 decimal places.", s.validate("192311001", "10.255"));
        check("V8", "amount above the maximum", "Amount cannot exceed Rs 500000.00.", s.validate("192311001", "500000.01"));
        check("V9", "valid input", null, s.validate("192311001", "12500.50"));
        check("S1", "SQL-injection text in the register number is rejected", "Register number must be exactly 9 digits.", s.validate("1'); DROP TABLE fees;--", "100"));
        FeeService.Result r = s.pay("192311001", "12500.50"); check("P1", "valid payment succeeds and is stored", "true / 1 row", r.ok() + " / " + repo.size() + " row");
        r = s.pay("192311002", "200000"); check("P2", "bank declines amount above its limit: nothing stored", "false / 1 row", r.ok() + " / " + repo.size() + " row");
        repo.failNextSave = true; r = s.pay("192311003", "100"); check("P3", "database failure is reported, not thrown", "false", r.ok()); System.out.println("     message shown to the user: " + r.message());
        check("R1", "receipt for a paid student", true, s.receipt("192311001").contains("Rs 12500.50"));
        check("R2", "receipt for an unknown student", "No payment found for 192311009.", s.receipt("192311009"));
        check("C1", "original defect: action command compared with ==", "false", new String("Pay") == "Pay");
        check("C2", "the repair: equals()", "true", new String("Pay").equals("Pay"));
        System.out.println("\nTotal: " + (pass + fail) + "   passed: " + pass + "   failed: " + fail);
    }
}
