import parking.model.*;
import java.util.*;

public class EqualsDemo {
    public static void main(String[] args) {
        Vehicle a = new Car("TN02B2222"), b = new ElectricCar("TN02B2222"), c = new Car("TN99Z9999");
        System.out.println("toString : " + a + " | " + b);
        System.out.println("a.equals(b) : " + a.equals(b) + "   (same registration number)");
        System.out.println("a.equals(c) : " + a.equals(c));
        System.out.println("hashCodes equal : " + (a.hashCode() == b.hashCode()));
        Set<Vehicle> parked = new HashSet<>(List.of(a, c));
        System.out.println("HashSet contains b : " + parked.contains(b));
        System.out.println("fee(a, 130 min) = " + a.calculateFee(130) + "   fee(b, 130 min) = " + b.calculateFee(130));
    }
}
