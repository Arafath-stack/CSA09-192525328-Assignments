import java.io.Serializable;
import java.sql.Connection;

public class Resident implements Serializable {
    //UID//
    String name;
    String regNo;
    String phone;                     // NEW FIELD added after files were saved
    transient String password;
    transient Connection con;

    Resident(String name, String regNo, String password) { this.name = name; this.regNo = regNo; this.password = password; }
    public String toString() { return "Resident{" + name + ", " + regNo + ", phone=" + phone + ", password=" + password + "}"; }
}
