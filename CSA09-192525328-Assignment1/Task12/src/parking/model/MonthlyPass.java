package parking.model;

import java.time.LocalDate;

/** Students and staff holding a valid monthly pass park free of charge. */
public final class MonthlyPass implements Pass {
    private final UserType holder;
    private final LocalDate validFrom, validTo;

    public MonthlyPass(UserType holder, LocalDate validFrom, LocalDate validTo) {
        if (holder == UserType.VISITOR) throw new IllegalArgumentException("Visitors cannot hold a monthly pass");
        if (validTo.isBefore(validFrom)) throw new IllegalArgumentException("validTo is before validFrom");
        this.holder = holder; this.validFrom = validFrom; this.validTo = validTo;
    }

    @Override public boolean isValidOn(LocalDate day) { return !day.isBefore(validFrom) && !day.isAfter(validTo); }
    @Override public double payable(double fee) { return 0; }
    public UserType getHolder() { return holder; }
}
