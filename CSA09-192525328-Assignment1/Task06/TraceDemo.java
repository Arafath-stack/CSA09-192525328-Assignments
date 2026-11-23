public class TraceDemo {
    static class Vehicle { Vehicle(String r) { System.out.println("1. Vehicle(" + r + ") constructor runs"); } }
    static class Car extends Vehicle { Car(String r) { super(r); System.out.println("2. Car(" + r + ") constructor runs"); } }
    static class ElectricCar extends Car {
        ElectricCar(String r) { super(r); System.out.println("3. ElectricCar(" + r + ") constructor runs"); }
    }
    public static void main(String[] args) {
        System.out.println("new ElectricCar(\"TN09EV0001\")");
        new ElectricCar("TN09EV0001");
    }
}
