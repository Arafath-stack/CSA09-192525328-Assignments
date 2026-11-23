package parking.model;

import java.time.Duration;
import java.time.LocalDateTime;

/** Abstract superclass: holds what every vehicle has and defines the billing algorithm once. */
public abstract class Vehicle {
    public static final int DAILY_CAP_HOURS = 8;     // a day is never billed for more than 8 hours

    private final String regNo;                      // final: a registration number never changes

    protected Vehicle(String regNo) {
        if (regNo == null || !regNo.matches("[A-Z0-9]{4,12}"))
            throw new IllegalArgumentException("Invalid registration number: " + regNo);
        this.regNo = regNo;
    }

    public final String getRegNo() { return regNo; }

    protected abstract double ratePerHour();         // each subclass supplies its own rate

    /** Every started hour is charged in full; integer arithmetic only; daily cap applied. */
    public double calculateFee(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Duration cannot be negative: " + minutes);
        int fullDays = minutes / 1440;
        int hours = ((minutes % 1440) + 59) / 60;    // integer ceiling
        return fullDays * DAILY_CAP_HOURS * ratePerHour() + Math.min(hours, DAILY_CAP_HOURS) * ratePerHour();
    }

    /** Overload (compile-time polymorphism): fee from entry and exit time. */
    public double calculateFee(LocalDateTime in, LocalDateTime out) {
        return calculateFee(Math.toIntExact(Duration.between(in, out).toMinutes()));
    }

    @Override public boolean equals(Object o) { return o instanceof Vehicle v && regNo.equals(v.regNo); }
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return getClass().getSimpleName() + "[" + regNo + "]"; }
}
