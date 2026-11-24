# CSA09-192525328-Assignment3 - Exception Handling & Multithreading

**Student:** A. Yassar Arafath, Register No 192525328 (pair-work: Student A and Student B sections are in the joint report)
**Java:** OpenJDK 21.0.12.1 (Ubuntu 24.04, 1 CPU core). Compile with `javac`, run with `java`.

Every folder holds the Java source, input files, `output.txt` (real terminal output of every command) and `output_screenshot.png` (the same output as a terminal image).
Compile example: `javac -Xlint:all Atm.java AtmTests.java` then `java AtmTests`. Programs that use several files are compiled together as shown in the first line of each `output.txt`.

| Folder | What it contains | How to run (from inside the folder) | Result |
|---|---|---|---|
| Q1 | ATM failures: try / multi-catch / finally, tests, uncaught stack trace, catch-order error, error-code alternative, cost of exceptions | `javac Atm.java AtmTests.java && java AtmTests` | 12 test inputs, every path and the finally block recorded; exception with stack trace 656 ns vs error code 5 ns at depth 5 |
| Q2 | Exam portal: user-defined exceptions, layers, chaining; `complete/StorageDemo.java` (TODO 1); unchecked alternative | `cd complete && javac StorageDemo.java && java StorageDemo` ; `cd portal && javac *.java && java PortalTests` | Expected output reproduced; without the cause `getCause().getMessage()` throws NullPointerException |
| Q3 | Sales report: manual close vs try-with-resources, jdb debugging session, suppressed exceptions, ManyFiles | `javac Reports.java ... && java Reports` ; `sh -c 'ulimit -n 256; java ManyFiles 3000 256'` | Naive version leaves 2 files open and an empty report; try-with-resources 0; no-close fails at file 252 with "Too many open files" |
| Q4 | Scanner stations: Thread vs Runnable, run() vs start(), life cycle, priorities, daemon, executor pool | `javac *.java && java Stations thread` | 10 runs give 10 different finish orders; pool time matches n/p x 5 ms |
| Q5 | Seat booking: `SeatBooking.java` (TODO 1), unsynchronized runs, race lab, static sync, atomic/lock/CHM alternatives | `javac *.java && java SeatBooking` | Seats sold: 10; unsynchronized with a pause sold 10-15 (up to 22 in the lab) |
| Q6 | Deadlock and producer-consumer: `TokenCounter.java` (TODO 1, 2), if-vs-while, notify vs notifyAll, transfers, alternatives | `javac *.java && java TokenCounter` | Sum 210; with `if` wrong sums in 191 of 1000 runs; `notify` hangs 200 of 200 runs |
| Q7 | 10,000-file processor: ExecutorService + Callable + Future, benchmarks, parallel stream and thread-per-file alternatives | `java GenerateData data` (or unzip data.zip), `java ResultProcessor data 4 0`, `java Bench data 1` | ok=9710 corrupt=187 missing=103, matches the generator; 1 ms latency: 11,245 ms (1 thread) to 211 ms (64 threads) |
