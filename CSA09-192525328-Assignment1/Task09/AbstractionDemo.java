import parking.model.*;
import java.time.LocalDate;

public class AbstractionDemo {
    public static void main(String[] args) {
        // Vehicle v = new Vehicle("TN01A1111");      // compile error: Vehicle is abstract
        Vehicle ev = new ElectricCar("TN09EV0001");   // is-a Vehicle (abstract class) AND Chargeable (interface)
        if (ev instanceof Chargeable c) {
            System.out.println(c.chargingNote());                          // default method
            System.out.println("valid units(-5)? " + Chargeable.isValidUnits(-5));  // static interface method
            System.out.println("charging 10 kWh = Rs " + c.chargingFee(10));
        }
        Pass p = new MonthlyPass(UserType.STAFF, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        System.out.println(p.describe() + " valid on 14 Nov: " + p.isValidOn(LocalDate.of(2026, 11, 14)) + ", payable for Rs 90 -> " + p.payable(90));
    }
}
