package parking.model;

/** Single inheritance from Car + one interface: multilevel hierarchy with an added capability. */
public class ElectricCar extends Car implements Chargeable {
    public ElectricCar(String regNo) { super(regNo); }

    @Override public double chargingFee(int kWh) {
        if (!Chargeable.isValidUnits(kWh)) throw new IllegalArgumentException("kWh cannot be negative");
        return kWh * RATE_PER_KWH;
    }
}
