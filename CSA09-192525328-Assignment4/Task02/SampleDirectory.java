import java.util.ArrayList;
import java.util.List;

public class SampleDirectory implements DoctorDirectory {
    private final int departments, perDepartment;
    public SampleDirectory() { this(0, 0); }
    public SampleDirectory(int departments, int perDepartment) { this.departments = departments; this.perDepartment = perDepartment; }
    @Override public List<Doctor> loadAll() {
        List<Doctor> l = new ArrayList<>();
        if (departments == 0) {
            String[][] d = {{"Cardiology", "Dr. Rao", "Dr. Iyer", "Dr. Nair"}, {"Orthopaedics", "Dr. Khan", "Dr. Das"}, {"Paediatrics", "Dr. Mehta", "Dr. Joseph", "Dr. Sen", "Dr. Pillai"}, {"Dermatology", "Dr. Bose"}};
            for (String[] row : d) for (int i = 1; i < row.length; i++) l.add(new Doctor(row[i], row[0]));
        } else for (int d = 0; d < departments; d++) for (int i = 0; i < perDepartment; i++) l.add(new Doctor("Dr. D" + d + "-" + i, "Dept" + d));
        return l;
    }
}
