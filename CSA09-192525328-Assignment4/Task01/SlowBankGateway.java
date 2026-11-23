import java.math.BigDecimal;

/** Simulates the bank: waits delayMs, then approves (or declines amounts above the limit). */
public class SlowBankGateway implements PaymentGateway {
    private final long delayMs; private final BigDecimal limit;
    public SlowBankGateway(long delayMs, BigDecimal limit) { this.delayMs = delayMs; this.limit = limit; }
    @Override public boolean confirm(String regNo, BigDecimal amount) throws InterruptedException { Thread.sleep(delayMs); return amount.compareTo(limit) <= 0; }
}
