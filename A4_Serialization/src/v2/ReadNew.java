import java.io.*;
public class ReadNew {
    public static void main(String[] a) throws Exception {
        try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(a[0]))) {
            Hostel h = (Hostel) i.readObject();
            System.out.println("Read OK with NEW Resident class. Hostel=" + h.name);
            for (Room r : h.rooms) for (Resident res : r.residents) System.out.println("  room " + r.number + ": " + res);
            System.out.println("  -> new field 'phone' = default null; transient 'password' = null (never stored)");
        } catch (InvalidClassException e) {
            System.out.println("FAILED: java.io.InvalidClassException: " + e.getMessage());
        }
    }
}
