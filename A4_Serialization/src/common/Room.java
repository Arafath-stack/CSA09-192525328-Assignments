import java.io.Serializable;
import java.util.*;
public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    int number; List<Resident> residents = new ArrayList<>();
    Room(int number) { this.number = number; }
}
