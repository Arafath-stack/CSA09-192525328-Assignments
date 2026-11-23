package parking.model;

import java.time.LocalDateTime;

public class ParkingTicket {
    private static int counter;                      // shared by ALL tickets -> numbers never repeat
    static { counter = 1000; }                       // static block: first ticket is #1001

    private final int ticketNo;
    private final Vehicle vehicle;
    private final int floor, slot;
    private final LocalDateTime entry;
    private final UserType user;
    private final Pass pass;                         // null when the user has no pass

    public ParkingTicket(Vehicle v, int floor, int slot, LocalDateTime entry) {          // overloaded, chains
        this(v, floor, slot, entry, UserType.VISITOR, null);
    }

    public ParkingTicket(Vehicle v, int floor, int slot, LocalDateTime entry, UserType user, Pass pass) {
        if (v == null || entry == null || user == null) throw new IllegalArgumentException("null argument");
        if (floor < 0 || slot < 0) throw new IllegalArgumentException("invalid slot position");
        this.ticketNo = ++counter;
        this.vehicle = v; this.floor = floor; this.slot = slot;
        this.entry = entry; this.user = user; this.pass = pass;
    }

    /** Copy constructor: re-issues a duplicate ticket with the SAME number. */
    public ParkingTicket(ParkingTicket t) {
        this.ticketNo = t.ticketNo; this.vehicle = t.vehicle; this.floor = t.floor; this.slot = t.slot;
        this.entry = t.entry; this.user = t.user; this.pass = t.pass;
    }

    public static int issuedCount() { return counter - 1000; }

    public int getTicketNo() { return ticketNo; }
    public Vehicle getVehicle() { return vehicle; }
    public int getFloor() { return floor; }
    public int getSlot() { return slot; }
    public LocalDateTime getEntry() { return entry; }
    public UserType getUser() { return user; }
    public Pass getPass() { return pass; }

    @Override public String toString() {
        return "Ticket#" + ticketNo + " " + vehicle + " F" + floor + "-S" + slot + " " + entry;
    }
}
