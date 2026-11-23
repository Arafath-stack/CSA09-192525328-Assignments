package parking.model;

import java.time.LocalDate;

/** NEW (Task 12): a visitor buys a pass valid for one calendar day; parking that day is free. */
public final class OneDayPass implements Pass {
    public static final double PRICE = 50;
    private final LocalDate day;
    public OneDayPass(LocalDate day) { this.day = day; }
    @Override public boolean isValidOn(LocalDate d) { return day.equals(d); }
    @Override public double payable(double fee) { return 0; }
}
