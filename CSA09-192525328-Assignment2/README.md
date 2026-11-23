# CSA0914-192525328-Assignment2

**Name:** A. Yassar Arafath  **Register number:** 192525328
**Assignment:** CO2 - Assignment 2 (Strings, Collections Framework, hashCode/equals, Generics, Comparable/Comparator, PriorityQueue)
**Java version:** OpenJDK 21 (Java 16+ required for records and `instanceof` patterns)

## How to compile and run
Every program is in the default package. From inside each Q folder (the programs write raw data to `data/`, so run them from that folder):
```
cd Q1 && javac StringBench.java && java StringBench           # about 1 minute; writes data/q1_*.csv
cd Q2 && javac ListBench.java && java ListBench               # about 1 minute
cd Q3/Before && javac Attendance.java && java Attendance       # BEFORE TODO 1 (no hashCode) -> Unique students: 3
cd Q3 && javac Attendance.java && java Attendance              # AFTER  TODO 1               -> Unique students: 2
cd Q3 && javac HashContractTests.java && java HashContractTests
cd Q4 && javac MapBench.java && java -Xmx3g MapBench           # needs about 2 GB heap for n = 500,000
cd Q5 && javac LeaderboardDemo.java RankingDesign.java && java LeaderboardDemo && java RankingDesign
cd Q6 && javac Triage.java TriageDesign.java && java Triage && java -Xmx3g TriageDesign
cd Q7 && javac MedicineStoreRedesign.java && java -Xmx3g MedicineStoreRedesign
```
(Q3: compile `Attendance.java` and `HashContractTests.java` in separate folders/commands because both define their own `Student` classes.)

## Result of each question
| Q | Result |
|---|---|
| Q1 | String + is x860 slower and allocates x3,200 more bytes than StringBuilder for one 20,000-field ticket; equal speed (but 5.4x more garbage) for 50 fields; StringBuilder per ticket, StringBuffer with one append per entry for the shared log |
| Q2 | ArrayList get is O(1) (x500 faster than LinkedList at 100,000); ArrayList is fine for append/random access; ArrayDeque for the Up Next queue |
| Q3 | Without hashCode the program prints `Unique students: 3`, with TODO 1 completed it prints `2` (expected); three designed tests with predictions |
| Q4 | LinkedHashMap chosen; code-order report needs a sort (n log n); sorted array comparison; Hashtable discussion |
| Q5 | `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]` (expected); subtraction comparator shown to be intransitive |
| Q6 | `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)` (expected); starvation simulation and hybrid fair rule |
| Q7 | Redesign of the Q4 store with an incrementally maintained sorted index: 100-10,000x fewer operations for the code-order report, lookups unchanged, +16 bytes per medicine |

Each Q folder contains the source, `output_*.txt`, an output screenshot `output_*.png` and the raw experimental data in `data/*.csv`. `charts/` holds the graphs used in the report.
