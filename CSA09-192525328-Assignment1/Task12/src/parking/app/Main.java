package parking.app;

import parking.model.*;
import parking.service.*;
import java.time.*;

public class Main {
    static final LocalDate D = LocalDate.of(2026, 11, 14);
    static LocalDateTime t(int h, int m) { return LocalDateTime.of(D, LocalTime.of(h, m)); }
    static double run(String type, String reg, UserType u, Pass p, LocalDateTime in, LocalDateTime out, int kWh) {
        ParkingService s = new ParkingService(new ParkingFacility(new int[]{2}));
        s.enter(VehicleFactory.create(type, reg), u, p, in);
        return s.exit(reg, out, kWh);
    }

    public static void main(String[] args) {
        TestKit k = new TestKit();
        k.header();
        System.out.println("--- Regression: original tests still pass on the extended code");
        k.check("R1", "TwoWheeler, 90 min", 20.0, () -> new TwoWheeler("TN01A1111").calculateFee(90));
        k.check("R2", "Car, 180 min", 90.0, () -> new Car("TN02B2222").calculateFee(180));
        k.check("R3", "EV 61 min + 10 kWh", 180.0, () -> run("EV", "TN03C3333", UserType.VISITOR, null, t(10, 0), t(11, 1), 10));
        k.check("R4", "Car, 600 min daily cap", 240.0, () -> new Car("TN05E5555").calculateFee(600));
        k.check("R5", "Car, 0 min", 0.0, () -> new Car("TN05E5555").calculateFee(0));
        k.throwsEx("R6", "Unknown vehicle type BUS", IllegalArgumentException.class, () -> VehicleFactory.create("BUS", "TN07G7777"));
        System.out.println("--- New: e-bike (flat fee)");
        k.check("E1", "E-bike 10 minutes", 20.0, () -> new EBike("TN21E0001").calculateFee(10));
        k.check("E2", "E-bike 10 hours", 20.0, () -> new EBike("TN21E0001").calculateFee(600));
        System.out.println("--- New: delivery van (2x car rate 09:00-17:00)");
        k.check("V1", "Van 08:30-10:30 (30 + 60)", 90.0, () -> run("VAN", "TN22V0001", UserType.VISITOR, null, t(8, 30), t(10, 30), 0));
        k.check("V2", "Van 09:00-11:00 fully peak (60 + 60)", 120.0, () -> run("VAN", "TN22V0002", UserType.VISITOR, null, t(9, 0), t(11, 0), 0));
        k.check("V3", "Van 18:00-20:00 fully off-peak (30 + 30)", 60.0, () -> run("VAN", "TN22V0003", UserType.VISITOR, null, t(18, 0), t(20, 0), 0));
        k.check("V4", "Van 17:00-18:00 starts at 17:00 = off-peak", 30.0, () -> run("VAN", "TN22V0004", UserType.VISITOR, null, t(17, 0), t(18, 0), 0));
        k.check("V5", "Van 16:30-17:30 starts in peak", 60.0, () -> run("VAN", "TN22V0005", UserType.VISITOR, null, t(16, 30), t(17, 30), 0));
        k.check("V6", "Van zero minutes", 0.0, () -> run("VAN", "TN22V0006", UserType.VISITOR, null, t(10, 0), t(10, 0), 0));
        System.out.println("--- New: one-day pass");
        k.check("P1", "Visitor car, one-day pass, 11 h on pass day", 0.0, () -> run("Car", "TN23P0001", UserType.VISITOR, new OneDayPass(D), t(9, 0), t(20, 0), 0));
        k.check("P2", "One-day pass for another day is ignored (60 min)", 30.0, () -> run("Car", "TN23P0002", UserType.VISITOR, new OneDayPass(D.plusDays(1)), t(10, 0), t(11, 0), 0));
        k.check("P3", "OneDayPass.isValidOn(other day)", false, () -> new OneDayPass(D).isValidOn(D.plusDays(1)));
        System.out.println("--- New: extended facility (5 floors, different slots per floor)");
        k.check("F1", "Capacity of {10,20,15,30,25}", 100, () -> new ParkingFacility(new int[]{10, 20, 15, 30, 25}).capacity());
        k.check("F2", "100 vehicles fit, 101st rejected", "Facility full", () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{10, 20, 15, 30, 25}));
            for (int i = 0; i < 100; i++) s.enter(new Car(String.format("TN30X%04d", i)), UserType.VISITOR, null, t(9, 0));
            try { s.enter(new Car("TN30X9999"), UserType.VISITOR, null, t(9, 1)); return "accepted"; }
            catch (IllegalStateException e) { return e.getMessage(); } });
        System.out.println("--- Hand-calculated scenario (expected total Rs 450)");
        ParkingService day = new ParkingService(new ParkingFacility(new int[]{10, 20, 15, 30, 25}));
        day.enter(VehicleFactory.create("TwoWheeler", "TN11A0001"), UserType.VISITOR, null, t(8, 0));
        day.enter(VehicleFactory.create("Car", "TN11B0002"), UserType.VISITOR, null, t(9, 0));
        day.enter(VehicleFactory.create("EV", "TN11C0003"), UserType.VISITOR, null, t(10, 0));
        day.enter(VehicleFactory.create("Car", "TN11D0004"), UserType.STUDENT, new MonthlyPass(UserType.STUDENT, D.minusDays(2), D.plusDays(27)), t(9, 0));
        day.enter(VehicleFactory.create("EBike", "TN11E0005"), UserType.VISITOR, null, t(8, 0));
        day.enter(VehicleFactory.create("Van", "TN11F0006"), UserType.VISITOR, null, t(8, 30));
        day.enter(VehicleFactory.create("Car", "TN11G0007"), UserType.VISITOR, new OneDayPass(D), t(9, 0));
        day.recordSale(OneDayPass.PRICE);
        double[] f = { day.exit("TN11A0001", t(9, 30), 0), day.exit("TN11B0002", t(12, 0), 0), day.exit("TN11C0003", t(11, 1), 10),
                day.exit("TN11D0004", t(17, 0), 0), day.exit("TN11E0005", t(18, 0), 0), day.exit("TN11F0006", t(10, 30), 0),
                day.exit("TN11G0007", t(20, 0), 0) };
        System.out.println("Fees: " + java.util.Arrays.toString(f) + " + one-day pass sale 50.0");
        k.check("S1", "Total collection for the day", 450.0, day::getCollection);
        k.summary();
    }
}
