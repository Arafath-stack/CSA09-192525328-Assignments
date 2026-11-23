package parking.service;

import parking.model.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Coordinates entry and exit. Depends only on abstractions (Vehicle, Pass, Chargeable). */
public class ParkingService {
    private final ParkingFacility facility;
    private final Map<String, ParkingTicket> active = new HashMap<>();   // regNo -> ticket, O(1) lookup
    private double collection;

    public ParkingService(ParkingFacility facility) { this.facility = facility; }

    public ParkingTicket enter(Vehicle v, UserType user, Pass pass, LocalDateTime when) {
        if (active.containsKey(v.getRegNo())) throw new IllegalStateException("Duplicate registration: " + v.getRegNo());
        int[] pos = facility.allocate(v.getRegNo());
        if (pos == null) throw new IllegalStateException("Facility full");
        ParkingTicket t = new ParkingTicket(v, pos[0], pos[1], when, user, pass);
        active.put(v.getRegNo(), t);
        return t;
    }

    /** Releases the slot and returns the amount payable (parking + charging). */
    public double exit(String regNo, LocalDateTime out, int kWh) {
        ParkingTicket t = active.get(regNo);
        if (t == null) throw new IllegalStateException("No active ticket for " + regNo);
        Vehicle v = t.getVehicle();
        double fee = v.calculateFee(t.getEntry(), out);                   // dynamic dispatch
        Pass p = t.getPass();
        if (p != null && p.isValidOn(t.getEntry().toLocalDate())) fee = p.payable(fee);
        if (v instanceof Chargeable c && kWh > 0) fee += c.chargingFee(kWh);
        active.remove(regNo);
        facility.release(t.getFloor(), t.getSlot());
        collection += fee;
        return fee;
    }

    public void recordSale(double amount) { collection += amount; }
    public double getCollection() { return collection; }
    public int activeCount() { return active.size(); }
}
