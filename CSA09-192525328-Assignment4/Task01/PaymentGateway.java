import java.math.BigDecimal;

/** The bank confirmation step (slow, may fail). */
public interface PaymentGateway {
    boolean confirm(String regNo, BigDecimal amount) throws Exception;
}
