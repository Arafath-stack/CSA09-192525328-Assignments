public class WrapperDemo {
    static class Stay {
        private int minutes;
        public int getMinutes() { return minutes; }
        public void setMinutes(int m) {                      // validating setter
            if (m < 0) throw new IllegalArgumentException("duration cannot be negative: " + m);
            this.minutes = m;
        }
    }
    public static void main(String[] args) {
        Integer a = 127, b = 127, c = 128, d = 128;          // autoboxing int -> Integer
        System.out.println("127 == 127 : " + (a == b) + "   (cached Integer objects)");
        System.out.println("128 == 128 : " + (c == d) + "  (different objects!)");
        System.out.println("128 equals : " + c.equals(d) + "   (always use equals for wrappers)");
        Stay s = new Stay();
        s.setMinutes(90);
        System.out.println("valid setter   : " + s.getMinutes());
        try { s.setMinutes(-5); } catch (IllegalArgumentException e) { System.out.println("invalid setter : " + e.getMessage()); }
        Integer missing = null;
        try { int x = missing; } catch (NullPointerException e) { System.out.println("auto-unboxing null -> NullPointerException"); }
    }
}
