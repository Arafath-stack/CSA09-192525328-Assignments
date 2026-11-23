import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One recorded fee payment. */
public record Payment(String regNo, BigDecimal amount, LocalDateTime paidAt) { }
