# CSA0914-192525328-Assignment1

**Name:** A. Yassar Arafath  **Register number:** 192525328
**Assignment:** Design, Implement and Validate an Object-Oriented Model for a Campus Smart-Parking System
**Java version:** OpenJDK 21 (Java 16+ needed because of `instanceof` pattern matching)

## How to compile and run
Single-file tasks (run inside the task folder):
```
cd Task03 && javac ParkingFacility.java && java ParkingFacility
cd Task08 && javac FeeDemo.java && java FeeDemo
```
Same pattern for Task02 (`CastingDemo`), Task04 (`TicketDemo`), Task05 (`StaticDemo`), Task06 (`TraceDemo`), Task10 (`WrapperDemo`).

Full system (Task 11 original design, Task 12 extended design):
```
cd Task11
javac -d out $(find src -name "*.java")        # on Windows: dir /s /b src\*.java > files.txt  then  javac -d out @files.txt
java -cp out parking.app.Main                   # 20 tests, expected vs actual table
javac Benchmarks.java && java Benchmarks        # measurements used in the complexity analyses
cd ../Task12 && javac -d out $(find src -name "*.java") && java -cp out parking.app.Main
```
Task07 and Task09 use the Task 11 model: after compiling Task11 run
`cd Task07 && javac -cp ../Task11/out EqualsDemo.java && java -cp .:../Task11/out EqualsDemo` (use `;` instead of `:` on Windows); same for Task09 / `AbstractionDemo`.

## Result of each task
| Task | Result |
|---|---|
| 01 | Analysis in report (classes, rules, assumptions, parallel-array comparison) |
| 02 | `CastingDemo`: `130/60` gives 2 hours (wrong); `(130+59)/60` gives 3 (right) |
| 03 | `ParkingFacility` prints `[0, 0] [0, 1] [1, 0] null / true false false / [0, 1]` = expected output |
| 04 | `TicketDemo`: overloaded constructors, chaining, copy constructor, `==` vs copy |
| 05 | `StaticDemo`: static counter gives 1,2,3,4; instance counters repeat 1,2,1,2 |
| 06 | `TraceDemo`: constructors run Vehicle -> Car -> ElectricCar |
| 07 | `EqualsDemo`: Car and ElectricCar with the same registration are equal |
| 08 | `FeeDemo` prints Rs 30.0 / 90.0 / 210.0 / true = expected output |
| 09 | `AbstractionDemo`: abstract class, default and static interface methods |
| 10 | `WrapperDemo`: Integer cache (127 vs 128), validating setter, unboxing null |
| 11 | Complete system, 20/20 tests pass, hand-calculated collection Rs 290 = program output |
| 12 | Design MODIFIED: 3 new classes + 2 lines in `VehicleFactory`; 20/20 tests pass; collection Rs 450 |

Each task folder contains the source, `output_*.txt` and an output screenshot `output_*.png`.
