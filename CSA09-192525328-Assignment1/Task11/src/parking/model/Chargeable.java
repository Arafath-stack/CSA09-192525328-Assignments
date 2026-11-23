package parking.model;

/** Capability interface: anything that can be billed for EV charging. */
public interface Chargeable {
    double RATE_PER_KWH = 12;                       // implicitly public static final

    double chargingFee(int kWh);

    default String chargingNote() { return "Charging is billed separately at Rs " + RATE_PER_KWH + "/kWh"; }

    static boolean isValidUnits(int kWh) { return kWh >= 0; }
}
