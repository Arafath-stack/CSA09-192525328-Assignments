import java.io.*;
public class WriteOld {
    public static void main(String[] a) throws Exception {
        Hostel h = new Hostel("Boys Hostel A");
        Room r1 = new Room(101); r1.residents.add(new Resident("Arun", "192311001", "secret1"));
        r1.residents.add(new Resident("Karthik", "192311002", "secret2"));
        Room r2 = new Room(102); r2.residents.add(new Resident("Divya", "192311003", "secret3"));
        h.rooms.add(r1); h.rooms.add(r2);
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(a[0]))) { o.writeObject(h); }
        System.out.println("Saved hostel with " + h.rooms.size() + " rooms using OLD Resident class -> " + a[0] + " (" + new File(a[0]).length() + " bytes)");
        System.out.println("  one writeObject(hostel) saved the whole graph: Hostel -> Rooms -> Residents");
    }
}
