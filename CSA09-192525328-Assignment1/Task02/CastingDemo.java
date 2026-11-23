public class CastingDemo {
    public static void main(String[] args) {
        int minutes = 130, rate = 30;
        int wrongHours = minutes / 60;                 // integer division -> 2 (truncates)
        System.out.println("Wrong  (minutes/60)           hours=" + wrongHours + " fee=Rs " + wrongHours * rate);
        double wrongCeil = Math.ceil(minutes / 60);    // division happens BEFORE ceil -> 2.0
        System.out.println("Wrong  Math.ceil(minutes/60)  hours=" + wrongCeil + " fee=Rs " + wrongCeil * rate);
        int rightHours = (minutes + 59) / 60;          // integer ceiling -> 3
        System.out.println("Right  (minutes+59)/60        hours=" + rightHours + " fee=Rs " + rightHours * rate);
        double rightCeil = Math.ceil(minutes / 60.0);  // 60.0 forces floating-point division
        System.out.println("Right  Math.ceil(minutes/60.0) hours=" + rightCeil + " fee=Rs " + rightCeil * rate);
        long big = 3_000_000_000L;
        System.out.println("Unsafe (int) cast of long     : " + (int) big);
        try { Math.toIntExact(big); } catch (ArithmeticException e) { System.out.println("Math.toIntExact detects it   : " + e.getMessage()); }
    }
}
