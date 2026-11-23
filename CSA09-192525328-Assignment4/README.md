# CSA0914-192525328-Assignment4

**Name:** A. Yassar Arafath  **Register number:** 192525328
**Assignment:** Event-Driven GUI Design (AWT / Swing)  **Java version:** OpenJDK 21 (Java 16+ needed: records, pattern matching)

## Layout
`Task01` fee window redesign | `Task02` outpatient desk | `Task03` library screen | `Task04` DrawPad | `Task05` currency converter |
`Task06` event-registration application (Swing + JDBC + SQL script) | `tools/` helpers | `charts/` graphs | `SCREENSHOT_CHECKLIST.md`

## Compile and run (real windows, on a machine with a display)
```
cd Task01 && javac *.java && java FeePanel                  # tests without a window:  java FeeServiceTest ; java FeePanelTest ; java DispatchBench
cd Task02 && javac *.java && java AppointmentFrame          # trace without a window:  java AppointmentTrace ; java DirectoryBench
cd Task03 && javac *.java && java LibraryPanel              # also: java LibraryShots ; java LayoutVariants
cd Task04 && javac DrawPad.java PaintBench.java CompileErrorDemo.java && java DrawPad
cd Task04/buggy && javac DrawPad.java                       # the defective program as supplied
cd Task05 && javac *.java && java Converter                 # ConvA..ConvD (original), ConvA2..ConvD2 (after the change), ConverterActionPanel
cd Task06 && javac -d out $(find src -name "*.java") && java -cp out app.ui.MainFrame      # in-memory demo data
```
Task 6 with MySQL: `mysql -u root -p < Task06/sql/schema.sql`, download MySQL Connector/J, then
`java -cp out:mysql-connector-j-8.x.x.jar app.ui.MainFrame mysql jdbc:mysql://localhost:3306/campus_events USER PASSWORD` (use `;` instead of `:` on Windows).

## Headless verification (no display needed - how the evidence in the report was produced)
`Task04/headless/run_headless.sh`, `Task05/headless/run_headless.sh`, `Task06/run_headless.sh`. The classes that must extend `Frame`/`JFrame`
are verified through mechanically derived JPanel twins (`tools/make_twin.py`: same listener and layout code, window-only lines removed).

## Result of each task
| Task | Result |
|---|---|
| 1 | 14 deficiencies diagnosed; redesign `FeeService` + `FeePanel` (SwingWorker); 17/17 service tests; EDT freeze 602 ms (original) vs 76 ms (redesign) |
| 2 | Event table, component tree, pseudocode; real event trace (8 events); 12/12 tests; Map-based directory measured |
| 3 | Nested BorderLayout/FlowLayout screen; results area grows 420x191 -> 1000x531, search row/buttons constant; GridBag/absolute compared |
| 4 | Defects: misspelled `mouseclicked`, dots drawn outside `paint()`; repaired DrawPad; @Override gives a compile error |
| 5 | Four handler styles + change effort (5 / 5 / 12 / 8 lines); Converter complete code; 51/51 checks; shared Action |
| 6 | Layered Swing + JDBC application, SQL script, 18/18 demo checks; paged vs full load measured |
