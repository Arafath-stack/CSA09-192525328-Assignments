public class ErrCatchOrder {
    public static void main(String[] args) {
        try {
            Integer.parseInt("abc");
        } catch (Exception e) {                        // broader first ...
            System.out.println("any exception");
        } catch (NumberFormatException e) {            // ... narrower can never be reached
            System.out.println("bad number");
        }
    }
}
