package parking.app;

import parking.model.*;
import parking.service.*;
import java.time.*;

public class Main {
    static final LocalDate D = LocalDate.of(2026, 11, 14);
    static LocalDateTime t(int h, int m) { return LocalDateTime.of(D, LocalTime.of(h, m)); }

    public static void main(String[] args) {
        TestKit k = new TestKit();
        k.header();
        System.out.println("--- Normal operation");
        k.check("N1", "TwoWheeler, 90 min (2 started hours x 10)", 20.0, () -> new TwoWheeler("TN01A1111").calculateFee(90));
        k.check("N2", "Car, 180 min (3 x 30)", 90.0, () -> new Car("TN02B2222").calculateFee(180));
        k.check("N3", "EV 61 min + 10 kWh (2x30 + 10x12)", 180.0, () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{2}));
            s.enter(new ElectricCar("TN03C3333"), UserType.VISITOR, null, t(10, 0));
            return s.exit("TN03C3333", t(11, 1), 10); });
        k.check("N4", "Student with monthly pass, car 8 h", 0.0, () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{2}));
            s.enter(new Car("TN04D4444"), UserType.STUDENT, new MonthlyPass(UserType.STUDENT, D.minusDays(5), D.plusDays(20)), t(9, 0));
            return s.exit("TN04D4444", t(17, 0), 0); });
        System.out.println("--- Boundary cases");
        k.check("B1", "Zero-minute parking (car)", 0.0, () -> new Car("TN05E5555").calculateFee(0));
        k.check("B2", "Exactly 60 min = 1 hour (car)", 30.0, () -> new Car("TN05E5555").calculateFee(60));
        k.check("B3", "61 min = 2 hours (car)", 60.0, () -> new Car("TN05E5555").calculateFee(61));
        k.check("B4", "480 min = 8 h = daily cap (car)", 240.0, () -> new Car("TN05E5555").calculateFee(480));
        k.check("B5", "600 min, daily cap still 240 (car)", 240.0, () -> new Car("TN05E5555").calculateFee(600));
        k.check("B6", "1500 min = 1 day + 1 h (car)", 270.0, () -> new Car("TN05E5555").calculateFee(1500));
        k.check("B7", "Last free slot used, next vehicle rejected", "Facility full", () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{1}));
            s.enter(new Car("TN06F6661"), UserType.VISITOR, null, t(9, 0));
            try { s.enter(new Car("TN06F6662"), UserType.VISITOR, null, t(9, 5)); return "accepted"; }
            catch (IllegalStateException e) { return e.getMessage(); } });
        k.check("B8", "Slot reusable after exit", "accepted", () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{1}));
            s.enter(new Car("TN06F6661"), UserType.VISITOR, null, t(9, 0)); s.exit("TN06F6661", t(10, 0), 0);
            s.enter(new Car("TN06F6662"), UserType.VISITOR, null, t(10, 5)); return "accepted"; });
        System.out.println("--- Invalid cases");
        k.throwsEx("I1", "Unknown vehicle type BUS", IllegalArgumentException.class, () -> VehicleFactory.create("BUS", "TN07G7777"));
        k.throwsEx("I2", "Duplicate registration number", IllegalStateException.class, () -> {
            ParkingService s = new ParkingService(new ParkingFacility(new int[]{3}));
            s.enter(new Car("TN08H8888"), UserType.VISITOR, null, t(9, 0));
            s.enter(new Car("TN08H8888"), UserType.VISITOR, null, t(9, 1)); });
        k.check("I3", "release(9,9) on invalid slot", false, () -> new ParkingFacility(new int[]{2, 2}).release(9, 9));
        k.throwsEx("I4", "Exit for a vehicle that never entered", IllegalStateException.class,
                () -> new ParkingService(new ParkingFacility(new int[]{2})).exit("TN09J9999", t(10, 0), 0));
        k.throwsEx("I5", "Negative duration", IllegalArgumentException.class, () -> new Car("TN05E5555").calculateFee(-1));
        k.throwsEx("I6", "Visitor holding a monthly pass", IllegalArgumentException.class,
                () -> new MonthlyPass(UserType.VISITOR, D, D.plusDays(30)));
        k.throwsEx("I7", "Invalid registration number 'x!'", IllegalArgumentException.class, () -> new Car("x!"));
        System.out.println("--- Hand-calculated scenario (expected total Rs 290)");
        ParkingService day = new ParkingService(new ParkingFacility(new int[]{3, 2, 2}));
        day.enter(VehicleFactory.create("TwoWheeler", "TN11A0001"), UserType.VISITOR, null, t(8, 0));
        day.enter(VehicleFactory.create("Car", "TN11B0002"), UserType.VISITOR, null, t(9, 0));
        day.enter(VehicleFactory.create("EV", "TN11C0003"), UserType.VISITOR, null, t(10, 0));
        day.enter(VehicleFactory.create("Car", "TN11D0004"), UserType.STUDENT, new MonthlyPass(UserType.STUDENT, D.minusDays(2), D.plusDays(27)), t(9, 0));
        double f1 = day.exit("TN11A0001", t(9, 30), 0), f2 = day.exit("TN11B0002", t(12, 0), 0),
               f3 = day.exit("TN11C0003", t(11, 1), 10), f4 = day.exit("TN11D0004", t(17, 0), 0);
        System.out.println("TwoWheeler 90 min = " + f1 + " | Car 180 min = " + f2 + " | EV 61 min+10kWh = " + f3 + " | Student pass = " + f4);
        k.check("S1", "Total collection for the day", 290.0, day::getCollection);
        k.summary();
    }
}
