package parking.model;

import java.time.LocalDate;

/** Abstraction for any pass that can reduce the parking fee. */
public interface Pass {
    boolean isValidOn(LocalDate day);

    /** Amount still payable for parking after the pass is applied. */
    double payable(double fee);

    default String describe() { return getClass().getSimpleName(); }
}
