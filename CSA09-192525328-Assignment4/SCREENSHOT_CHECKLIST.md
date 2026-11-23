# Screenshots to take on YOUR machine (real windows)

The screenshots in the report PDF were produced without a display (the build machine's Java has no AWT window support):
the real Swing component trees were laid out and painted off-screen into PNG files, and events were delivered with
`doClick()`, `postActionEvent()` and `dispatchEvent(MouseEvent)`. For the viva / GitHub evidence, run each program in a real
window on your laptop and take these screenshots (Windows: Win+Shift+S). Save them in the task folder with these names.

| Task | Run | Screenshot name | What it must show (requirement it demonstrates) |
|---|---|---|---|
| 1.2 | `java FeePanel` | `real_1_validation.png`, `real_1_processing.png`, `real_1_success.png` | invalid input message; "Processing..." with the window still movable; success message |
| 2.4 | `java AppointmentFrame` | `real_2_trace_console.png` | the console printing windowOpened, windowActivated ... windowClosing, windowClosed in order |
| 3.4 | `java LibraryPanel` | `real_3_small.png`, `real_3_medium.png`, `real_3_large.png` | the window dragged to three sizes: the result list grows, search row and buttons keep their size |
| 4 | `java DrawPad` (repaired) | `real_4_before_minimise.png`, `real_4_after_minimise.png` | dots and "Clicks: n" before and after minimise + restore |
| 4 | `java buggy.DrawPad` (compile it separately) | `real_4_defect.png` | clicking does nothing, label stays "Clicks: 0" |
| 5 | `java Converter` | `real_5_valid.png`, `real_5_invalid.png` | 1000 -> USD gives 12.00 USD; "abc" gives Invalid amount |
| 5.2 | `java ConvA2` ... `ConvD2` | `real_5_dialog.png` | the result in a dialog box after pressing Enter |
| 6 | `java -cp out:mysql-connector-j.jar app.ui.MainFrame mysql jdbc:mysql://localhost:3306/campus_events USER PASSWORD` | `real_6_normal.png`, `real_6_invalid.png`, `real_6_duplicate.png`, `real_6_table.png`, `real_6_filtered.png` | run against YOUR MySQL after `mysql < Task06/sql/schema.sql` |

IMPORTANT for Task 6: the JDBC class `JdbcRegistrationDao` could not be executed on the build machine (no MySQL server). Run it
against your own MySQL, and fix the connection URL/user/password if needed, before the viva.
