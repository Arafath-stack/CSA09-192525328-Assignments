import java.math.BigDecimal;
import java.util.List;

/** Data access for fee payments (the service does not know whether this is JDBC or memory). */
public interface FeeRepository {
    void save(String regNo, BigDecimal amount) throws Exception;
    List<Payment> findByRegNo(String regNo) throws Exception;
}
