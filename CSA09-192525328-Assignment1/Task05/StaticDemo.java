public class StaticDemo {
    static class Facility {
        static int sharedCounter;      // ONE copy for the whole class
        int ownCounter;                // one copy PER facility object
        int issueShared() { return ++sharedCounter; }
        int issueOwn()    { return ++ownCounter; }
    }
    public static void main(String[] args) {
        Facility north = new Facility(), south = new Facility();
        System.out.println("static counter  : north " + north.issueShared() + ", north " + north.issueShared()
                + ", south " + south.issueShared() + ", south " + south.issueShared());
        System.out.println("instance counter: north " + north.issueOwn() + ", north " + north.issueOwn()
                + ", south " + south.issueOwn() + ", south " + south.issueOwn() + "   <- numbers repeat!");
        Object ticket = new Object();
        ticket = null;   // after exit no reference remains -> object is eligible for garbage collection
        System.out.println("ticket reference set to null -> eligible for GC");
    }
}
