public class SeatBookingYield {
    private final boolean[] booked;
    private int bookingsMade = 0;
    SeatBookingYield(int seats) { booked = new boolean[seats]; }

    // TODO 1 written WITHOUT synchronized (first experiment)
    void book(int seat) {
        if (!booked[seat]) {
            Thread.yield();          // the scheduler may switch threads right after the check (models a slow database call)
            booked[seat] = true;
            bookingsMade++;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        SeatBookingYield screen = new SeatBookingYield(10);
        Thread[] users = new Thread[100];
        for (int i = 0; i < users.length; i++) {
            final int seat = i % 10; // 10 users compete for every seat
            users[i] = new Thread(() -> screen.book(seat));
            users[i].start();
        }
        for (Thread t : users) t.join();
        System.out.println("Seats sold: " + screen.bookingsMade);
    }
}
