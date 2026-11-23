import java.io.PrintWriter;
import java.util.*;
import javax.swing.DefaultComboBoxModel;

/** Alternative concept, measured part: load the directory once into Map<String, List<Doctor>>, then refill the doctor list per department change. */
public class DirectoryBench {
    static long sink;
    static long used() { Runtime r = Runtime.getRuntime(); return r.totalMemory() - r.freeMemory(); }
    public static void main(String[] args) throws Exception {
        PrintWriter csv = new PrintWriter("directory_times.csv"); csv.println("departments,doctors_per_department,rep,us_per_department_change");
        System.out.printf("%-12s %-12s %-10s %24s %24s%n", "departments", "doctors/dept", "m total", "us per department change", "Map memory (bytes)");
        for (int[] dm : new int[][]{{4, 3}, {10, 50}, {20, 500}, {20, 5000}}) {
            int d = dm[0], per = dm[1];
            for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(40); } long u0 = used();
            AppointmentService s = new AppointmentService(new SampleDirectory(d, per)); long raw = 0;
            for (int g = 0; g < 3; g++) { System.gc(); Thread.sleep(40); } long u1 = used(); raw = Math.max(0, u1 - u0);
            List<String> depts = new ArrayList<>(s.departments()); DefaultComboBoxModel<Doctor> model = new DefaultComboBoxModel<>(); double[] us = new double[7]; int reps = 2000;
            for (int rep = -2; rep < 7; rep++) {
                long t0 = System.nanoTime();
                for (int k = 0; k < reps; k++) { model.removeAllElements(); for (Doctor x : s.doctorsIn(depts.get(k % depts.size()))) model.addElement(x); }
                double v = (System.nanoTime() - t0) / 1e3 / reps; if (rep >= 0) { us[rep] = v; csv.printf("%d,%d,%d,%.2f%n", d, per, rep, v); }
            }
            sink += model.getSize();
            System.out.printf("%-12d %-12d %-10d %24.1f %24d%n", d, per, d * per, Arrays.stream(us).average().orElse(0), raw);
        }
        csv.close(); System.out.println("(checksum " + sink + ")");
    }
}
