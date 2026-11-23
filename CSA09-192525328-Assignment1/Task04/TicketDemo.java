public class TicketDemo {
    static class Ticket {
        private static int counter = 1000;
        private final int no; private final String reg; private final int minutes;
        Ticket() { this("UNKNOWN"); }                       // default -> chains to 1-arg
        Ticket(String reg) { this(reg, 0); }                // overloaded -> chains to full
        Ticket(String reg, int minutes) {                   // parameterised (master constructor)
            this.no = ++counter; this.reg = reg; this.minutes = minutes;
        }
        Ticket(Ticket t) { this.no = t.no; this.reg = t.reg; this.minutes = t.minutes; } // copy (duplicate ticket)
        public String toString() { return "Ticket#" + no + "[" + reg + "," + minutes + "min]"; }
    }
    public static void main(String[] args) {
        Ticket a = new Ticket();
        Ticket b = new Ticket("TN01AB1111");
        Ticket c = new Ticket("TN02CD2222", 45);
        Ticket dup = new Ticket(c);          // re-issued duplicate: same ticket number, new object
        Ticket alias = c;                    // second reference to the SAME heap object
        System.out.println(a + "\n" + b + "\n" + c + "\n" + dup);
        System.out.println("c == alias : " + (c == alias));
        System.out.println("c == dup   : " + (c == dup) + "  (different objects on the heap)");
    }
}
