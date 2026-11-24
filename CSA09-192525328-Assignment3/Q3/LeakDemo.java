/** Small target for the debugger session: runs only the naive version on the bad data. */
public class LeakDemo {
    public static void main(String[] args) throws Exception {
        try {
            Reports.manualBuggy("data_bad", 5, "sum_debug.txt");
        } catch (NumberFormatException e) {
            System.out.println("caught " + e.getMessage() + "; open readers=" + TrackedReader.open + ", open writers=" + TrackedWriter.open);
        }
    }
}
