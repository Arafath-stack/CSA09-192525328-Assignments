import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

/** Checked exceptions of the processor: every expected failure has its own type. */
class ScoreFileException extends Exception {
    private static final long serialVersionUID = 1L;
    ScoreFileException(String m) { super(m); }
    ScoreFileException(String m, Throwable cause) { super(m, cause); }
}
class MissingFileException extends ScoreFileException {
    private static final long serialVersionUID = 1L;
    MissingFileException(String id, Throwable cause) { super(id + ": file is missing", cause); }
}
class CorruptFileException extends ScoreFileException {
    private static final long serialVersionUID = 1L;
    CorruptFileException(String id, String reason) { super(id + ": corrupt file (" + reason + ")"); }
    CorruptFileException(String id, String reason, Throwable cause) { super(id + ": corrupt file (" + reason + ")", cause); }
}

record Result(String id, int total, String grade) { }

final class ScoreReader {
    static final String[] SUBJECTS = {"MATH", "PHY", "CHEM", "ENG", "CS"};
    private ScoreReader() { }

    static String gradeOf(int total) {
        int avg = total / SUBJECTS.length;
        if (avg >= 90) return "S"; if (avg >= 80) return "A"; if (avg >= 70) return "B"; if (avg >= 60) return "C"; if (avg >= 50) return "D"; return "F";
    }

    /** Reads, validates and computes one result. latencyMs simulates a slow disk or network share. */
    static Result read(Path dir, String id, int latencyMs) throws ScoreFileException {
        if (latencyMs > 0) {
            try { Thread.sleep(latencyMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ScoreFileException(id + ": interrupted", e); }
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(dir.resolve("score_" + id + ".txt"));
        } catch (NoSuchFileException e) {
            throw new MissingFileException(id, e);
        } catch (CharacterCodingException e) {
            throw new CorruptFileException(id, "not text", e);
        } catch (IOException e) {
            throw new ScoreFileException(id + ": unreadable", e);
        }
        if (lines.size() != 1 + SUBJECTS.length) throw new CorruptFileException(id, "expected " + (1 + SUBJECTS.length) + " lines but found " + lines.size());
        if (!lines.get(0).trim().equals(id)) throw new CorruptFileException(id, "wrong student id line");
        int total = 0;
        for (int k = 0; k < SUBJECTS.length; k++) {
            String[] p = lines.get(k + 1).split("=");
            if (p.length != 2 || !p[0].trim().equals(SUBJECTS[k])) throw new CorruptFileException(id, "bad line " + (k + 2));
            int marks;
            try { marks = Integer.parseInt(p[1].trim()); }
            catch (NumberFormatException e) { throw new CorruptFileException(id, "marks '" + p[1].trim() + "' is not a whole number", e); }
            if (marks < 0 || marks > 100) throw new CorruptFileException(id, "marks " + marks + " out of range 0-100");
            total += marks;
        }
        return new Result(id, total, gradeOf(total));
    }
}
