import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Repository used by the tests and the headless demo. */
public class InMemoryFeeRepository implements FeeRepository {
    private final List<Payment> rows = new ArrayList<>();
    public boolean failNextSave;                       // lets a test simulate a database failure
    @Override public synchronized void save(String regNo, BigDecimal amount) throws Exception {
        if (failNextSave) { failNextSave = false; throw new java.sql.SQLException("connection refused"); }
        rows.add(new Payment(regNo, amount, LocalDateTime.now()));
    }
    @Override public synchronized List<Payment> findByRegNo(String regNo) { List<Payment> r = new ArrayList<>(); for (Payment p : rows) if (p.regNo().equals(regNo)) r.add(p); return r; }
    public synchronized int size() { return rows.size(); }
}
