import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** All rules of the fee payment, with no GUI code: validation, bank confirmation, storage, receipt text. */
public class FeeService {
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("500000.00");
    public record Result(boolean ok, String message) { }

    private final FeeRepository repository; private final PaymentGateway bank;
    public FeeService(FeeRepository repository, PaymentGateway bank) { this.repository = repository; this.bank = bank; }

    /** @return null when the input is valid, otherwise the message to show the user. */
    public String validate(String regNo, String amountText) {
        if (regNo == null || regNo.trim().isEmpty()) return "Enter the register number.";
        if (!regNo.trim().matches("\\d{9}")) return "Register number must be exactly 9 digits.";
        if (amountText == null || amountText.trim().isEmpty()) return "Enter the amount.";
        BigDecimal a;
        try { a = new BigDecimal(amountText.trim()); } catch (NumberFormatException e) { return "Amount must be a number, for example 12500.50."; }
        if (a.signum() <= 0) return "Amount must be greater than zero.";
        if (a.scale() > 2) return "Amount can have at most 2 decimal places.";
        if (a.compareTo(MAX_AMOUNT) > 0) return "Amount cannot exceed Rs " + MAX_AMOUNT.toPlainString() + ".";
        return null;
    }

    /** Validates, asks the bank, then stores. Blocking: call it from a background thread. Never throws. */
    public Result pay(String regNo, String amountText) {
        String error = validate(regNo, amountText);
        if (error != null) return new Result(false, error);
        String reg = regNo.trim(); BigDecimal amount = new BigDecimal(amountText.trim());
        try {
            if (!bank.confirm(reg, amount)) return new Result(false, "The bank declined the payment. Nothing was recorded.");
            repository.save(reg, amount);                       // stored only AFTER the bank has confirmed
            return new Result(true, "Payment of Rs " + amount.toPlainString() + " recorded for " + reg + ".");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); return new Result(false, "Payment cancelled.");
        } catch (Exception e) { return new Result(false, "Could not complete the payment: " + e.getMessage()); }
    }

    public String receipt(String regNo) {
        try {
            if (regNo == null || !regNo.trim().matches("\\d{9}")) return "Enter a valid 9-digit register number to print a receipt.";
            List<Payment> p = repository.findByRegNo(regNo.trim());
            if (p.isEmpty()) return "No payment found for " + regNo.trim() + ".";
            Payment last = p.get(p.size() - 1);
            return "RECEIPT\nRegister no : " + last.regNo() + "\nAmount      : Rs " + last.amount().toPlainString() + "\nPaid at     : " + last.paidAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "\nPayments    : " + p.size();
        } catch (Exception e) { return "Could not read the receipt: " + e.getMessage(); }
    }
}
