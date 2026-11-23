# CSA0914 – Data Persistence Engineering – Assignment 5

| | |
|---|---|
| **Name** | Yassar Arafath A |
| **Register number** | 192525328 |
| **Repository** | `CSA09-192525328-Assignment5` |
| **Java version** | OpenJDK 21 (`javac 21`) – works with Java 11+ |
| **Database** | MySQL-compatible (tested on MariaDB 10.11 with MariaDB Connector/J 2.7.6; the same code uses the `jdbc:mysql://` URL, so MySQL 8 + Connector/J 8 works unchanged) |
| **OS / hardware used for measurements** | Ubuntu 24.04, 1 vCPU Intel Xeon 2.1 GHz, 4 GB RAM |

## Folder map (one folder per task)

| Folder | Task | What it contains |
|---|---|---|
| `A1_FixedRecord/` | A1 – alternative concept | `RandomAccessFile` fixed-length record store, measured against text scan |
| `A4_Serialization/` | A4 | `transient`, object graph, `serialVersionUID` versioning test, serialization vs CSV vs JSON |
| `B1_FileStore/` | B1 (+A2) | `PassStore` (TODO 1 completed), strict `BusPassStore`, tests, buffered line counter, growth benchmark |
| `B2_Adversarial/` | B2 | SQL-injection demo (unsafe vs `PreparedStatement` vs escaping vs regex), damaged file demo |
| `B3_Jdbc/` | B3 | `PassLoader` (TODO 1-3 completed), Statement / PreparedStatement / batch / transaction experiments, multi-row INSERT & LOAD DATA |
| `B4_Migration/` | B4 | `Migrator`, `MetaReport` (DatabaseMetaData), concurrency demo, 12-campus scale test |
| `sql/` | – | `create_test_db.sql` – creates the local test database and tables |

Part A (pseudocode, record layouts, reasoning) is in the submitted PDF report. Every `outputs/` folder holds the real console output of the programs (and **you should add your own screenshots** next to them).

## Setup

```bash
# 1. database (run as MySQL admin)
mysql -u root -p < sql/create_test_db.sql      # creates buspass_test, user bususer/buspass123, tables users + bus_pass

# 2. JDBC driver: download MySQL Connector/J (mysql-connector-j-8.x.jar) and point JDBC_JAR to it
export JDBC_JAR=/path/to/mysql-connector-j-8.x.jar

# optional – other DB settings (defaults shown)
export DB_URL=jdbc:mysql://localhost:3306/buspass_test
export DB_USER=bususer
export DB_PASS=buspass123
```

## How to compile and run

Run the folders **in this order** (B2 and B4 reuse classes compiled by B1 and B3).

```bash
cd B1_FileStore   && ./run.sh && cd ..                 # PassStore, tests, line counter (takes ~25 s), growth benchmark
cd A1_FixedRecord && javac -d out FixedRecordStore.java && java -cp out FixedRecordStore && cd ..
cd A4_Serialization && ./run.sh && cd ..
cd B2_Adversarial && ./run.sh && cd ..                 # needs the database
cd B3_Jdbc        && ./run.sh && cd ..                 # needs the database (~1.5 min)
cd B4_Migration   && ./run.sh && cd ..                 # needs the database
```

Manual compile example (B1): `javac -d out *.java` then `java -cp out PassStore`.
For JDBC programs add the driver: `java -cp "out:$JDBC_JAR" LoginDemo` (use `;` instead of `:` on Windows).

## Short result of each task

| Task | Result |
|---|---|
| **B1** | `PassStore` prints exactly `[P1003, 192311003, Divya, 7, 3800.0]` and `null`; the damaged line is skipped. 11/11 tests pass. 50 MB line count: **14,281 ms unbuffered (52,428,811 read calls)** vs **1,010 ms with BufferedInputStream (6,402 calls)** vs **23 ms with a 64 KB array (802 calls)**. Find time grows linearly: 0.19 ms (1,000 records) → 12.8 ms (100,000). |
| **B2** | `admin' -- ` logs in without a password on the unsafe version; `PreparedStatement` rejects it. Quote-doubling is bypassed with a backslash; regex validation locks out valid users. Damaged-file lines (wrong field count, non-numeric amount, empty) are all rejected by the strict reader. |
| **B3** | Inserting 100,000 rows: Statement 15.1 s, PreparedStatement 14.4 s, batch (auto-commit on) 14.8 s, **batch in one transaction 2.2 s**, multi-row INSERT 0.45 s, LOAD DATA 0.25 s. Forced failure: auto-commit leaves 5,000 rows, transaction leaves **0**. |
| **B4** | Migration: 10,000 lines → 9,977 loaded + 23 rejected (report generated, reconciles exactly). 4 counters writing a shared serialized file lost 381 of 400 passes; the database lost none and the PRIMARY KEY rejected a duplicate ID. **Decision: use the database.** |

## Commit history

One commit per task (see `git log`).

## Extra material

- `report/` – the two PDFs submitted with the assignment (design + analysis report, and execution-evidence captures).
- `evidence_captures/` – PNG captures of each program's console output (replace/add your own screenshots from your machine if your faculty requires them).
