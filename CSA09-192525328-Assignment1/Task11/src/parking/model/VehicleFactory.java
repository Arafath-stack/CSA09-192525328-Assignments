package parking.model;

/** Single place where a type name is turned into an object. */
public final class VehicleFactory {
    private VehicleFactory() { }

    public static Vehicle create(String type, String regNo) {
        switch (type.toUpperCase()) {
            case "TWOWHEELER": return new TwoWheeler(regNo);
            case "CAR":        return new Car(regNo);
            case "EV":         return new ElectricCar(regNo);
            default: throw new IllegalArgumentException("Unknown vehicle type: " + type);
        }
    }
}
