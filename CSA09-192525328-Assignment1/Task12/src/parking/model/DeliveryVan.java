package parking.model;

import java.time.*;

/** NEW (Task 12): double the car rate for every started hour that begins between 09:00 and 17:00. */
public class DeliveryVan extends Vehicle {
    private static final LocalTime PEAK_START = LocalTime.of(9, 0), PEAK_END = LocalTime.of(17, 0);
    public DeliveryVan(String regNo) { super(regNo); }
    @Override protected double ratePerHour() { return Car.RATE; }          // off-peak = car rate

    @Override public double calculateFee(LocalDateTime in, LocalDateTime out) {
        long mins = Duration.between(in, out).toMinutes();
        if (mins < 0) throw new IllegalArgumentException("Exit before entry");
        int hours = (int) ((mins + 59) / 60);
        double total = 0;
        for (int h = 0; h < hours; h++) {
            LocalTime start = in.plusHours(h).toLocalTime();
            boolean peak = !start.isBefore(PEAK_START) && start.isBefore(PEAK_END);
            total += peak ? 2 * Car.RATE : Car.RATE;
        }
        long days = (mins + 1439) / 1440;
        return Math.min(total, days * DAILY_CAP_HOURS * 2 * Car.RATE);
    }
}
