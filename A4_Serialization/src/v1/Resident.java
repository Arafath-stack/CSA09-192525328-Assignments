import java.io.Serializable;
import java.sql.Connection;

public class Resident implements Serializable {
    //UID//
    String name;
    String regNo;
    transient String password;        // secret: must never be written to disk
    transient Connection con;         // live resource: not Serializable and meaningless after restart

    Resident(String name, String regNo, String password) { this.name = name; this.regNo = regNo; this.password = password; }
    public String toString() { return "Resident{" + name + ", " + regNo + ", password=" + password + "}"; }
}
