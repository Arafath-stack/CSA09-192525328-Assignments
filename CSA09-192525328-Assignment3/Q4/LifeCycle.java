public class LifeCycle {
    static final Object lock = new Object();
    public static void main(String[] args) throws Exception {
        Thread sleeper = new Thread(() -> { try { Thread.sleep(300); } catch (InterruptedException e) { } }, "sleeper");
        System.out.println("sleeper before start()        : " + sleeper.getState());          // NEW
        sleeper.start();
        System.out.println("sleeper right after start()   : " + sleeper.getState());          // RUNNABLE
        Thread.sleep(50);
        System.out.println("sleeper inside sleep(300)     : " + sleeper.getState());          // TIMED_WAITING

        Thread waiter = new Thread(() -> { synchronized (lock) { try { lock.wait(); } catch (InterruptedException e) { } } }, "waiter");
        waiter.start(); Thread.sleep(50);
        System.out.println("waiter inside lock.wait()     : " + waiter.getState());           // WAITING

        Thread blocked;
        synchronized (lock) {                                                                   // main owns the monitor
            blocked = new Thread(() -> { synchronized (lock) { } }, "blocked");
            blocked.start(); Thread.sleep(50);
            System.out.println("blocked trying to enter lock: " + blocked.getState());         // BLOCKED
            lock.notifyAll();
        }
        waiter.join(); blocked.join();
        Thread joiner = new Thread(() -> { try { sleeper.join(); } catch (InterruptedException e) { } }, "joiner");
        joiner.start(); Thread.sleep(50);
        System.out.println("joiner inside sleeper.join()  : " + joiner.getState());           // WAITING
        sleeper.join(); joiner.join();
        System.out.println("sleeper after it finished     : " + sleeper.getState() + ", isAlive=" + sleeper.isAlive());   // TERMINATED
    }
}
