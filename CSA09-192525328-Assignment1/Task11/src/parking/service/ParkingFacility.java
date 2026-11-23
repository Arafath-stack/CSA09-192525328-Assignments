package parking.service;

/** Jagged array: every floor may have a different number of slots. */
public class ParkingFacility {
    private final String[][] slots;

    public ParkingFacility(int[] slotsPerFloor) {
        slots = new String[slotsPerFloor.length][];
        for (int f = 0; f < slotsPerFloor.length; f++) {
            if (slotsPerFloor[f] <= 0) throw new IllegalArgumentException("floor " + f + " needs at least 1 slot");
            slots[f] = new String[slotsPerFloor[f]];
        }
    }

    public int[] allocate(String vehicleNo) {
        for (int f = 0; f < slots.length; f++)
            for (int s = 0; s < slots[f].length; s++)
                if (slots[f][s] == null) { slots[f][s] = vehicleNo; return new int[]{f, s}; }
        return null;
    }

    public boolean release(int floor, int slot) {
        if (floor < 0 || floor >= slots.length || slot < 0 || slot >= slots[floor].length || slots[floor][slot] == null)
            return false;
        slots[floor][slot] = null;
        return true;
    }

    public int capacity() { int c = 0; for (String[] fl : slots) c += fl.length; return c; }
}
