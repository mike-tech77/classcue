import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// Everything about a student's profile, saved courses and study settings.
// The check methods (valid...) never touch the database, so they are easy to test.
public class Students {

    public static class Profile {
        public int id;
        public String username;
        public String programme;   // "CE" or "DS"
        public int level;          // 100, 200, 300 or 400
        public boolean isNew;      // true if this call created the profile
    }

    public static class Prefs {
        public int hours;
        public String when;        // "6-12", "12-17" or "17-22"
        public String free;        // "Friday,Monday"
        public String focus;       // "CE 273,CE 277"
    }

    // ---------- checks on what the browser sends us ----------

    static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    static final Pattern COURSE_CODE = Pattern.compile("^[A-Z]{2} \\d{3}$");
    static final List<String> WINDOWS = List.of("6-12", "12-17", "17-22");
    static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    public static boolean validUsername(String s) {
        return s != null && USERNAME.matcher(s).matches();
    }

    public static boolean validProgramme(String s) {
        return "CE".equals(s) || "DS".equals(s);
    }

    public static boolean validLevel(int level) {
        return level == 100 || level == 200 || level == 300 || level == 400;
    }

    public static boolean validCourseCode(String s) {
        return s != null && COURSE_CODE.matcher(s).matches();
    }

    public static boolean validGroup(String s) {
        return "".equals(s) || "A".equals(s) || "B".equals(s);
    }

    public static boolean validWindow(String s) {
        return WINDOWS.contains(s);
    }

    public static boolean validDays(List<String> days) {
        return DAYS.containsAll(days);
    }

    // ---------- profile ----------

    // Finds the student with this username, or creates one.
    // If an existing student picks a different programme or level, we update it
    // and clear their old course choices (they belong to the old level).
    public static Profile findOrCreate(String username, String programme, int level) throws SQLException {
        try (Connection conn = Db.connect()) {
            Profile p = new Profile();

            String find = "SELECT id, username, programme, level FROM students WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(find)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        p.id = rs.getInt("id");
                        p.username = rs.getString("username");
                        p.isNew = false;
                        boolean moved = !rs.getString("programme").equals(programme)
                                || rs.getInt("level") != level;
                        if (moved) {
                            updateProfile(conn, p.id, programme, level);
                        }
                        p.programme = programme;
                        p.level = level;
                        return p;
                    }
                }
            }

            String insert = "INSERT INTO students (username, programme, level) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, username);
                stmt.setString(2, programme);
                stmt.setInt(3, level);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    p.id = keys.getInt(1);
                }
            }
            p.username = username;
            p.programme = programme;
            p.level = level;
            p.isNew = true;
            return p;
        }
    }

    private static void updateProfile(Connection conn, int id, String programme, int level) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE students SET programme = ?, level = ? WHERE id = ?")) {
            stmt.setString(1, programme);
            stmt.setInt(2, level);
            stmt.setInt(3, id);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM registrations WHERE student_id = ?")) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // Returns the student's id, or -1 if there is no such username
    public static int findId(String username) throws SQLException {
        try (Connection conn = Db.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM students WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
        }
    }

    // ---------- saved courses ----------

    // Returns "CE 251:,CE 271:A" or null if the student has saved nothing yet
    public static String loadRegistration(int studentId) throws SQLException {
        String sql = "SELECT course_code, group_label FROM registrations "
                + "WHERE student_id = ? ORDER BY course_code";
        List<String> parts = new ArrayList<>();
        try (Connection conn = Db.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    parts.add(rs.getString("course_code") + ":" + rs.getString("group_label"));
                }
            }
        }
        return parts.isEmpty() ? null : String.join(",", parts);
    }

    // Replaces the student's saved courses with this list (all or nothing)
    public static void saveRegistration(int studentId, Map<String, String> registered) throws SQLException {
        try (Connection conn = Db.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM registrations WHERE student_id = ?")) {
                    del.setInt(1, studentId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO registrations (student_id, course_code, group_label) VALUES (?, ?, ?)")) {
                    for (Map.Entry<String, String> e : registered.entrySet()) {
                        ins.setInt(1, studentId);
                        ins.setString(2, e.getKey());
                        ins.setString(3, e.getValue());
                        ins.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    // ---------- saved study settings ----------

    public static Prefs loadPreferences(int studentId) throws SQLException {
        String sql = "SELECT weekly_hours, time_window, free_days, focus_courses "
                + "FROM preferences WHERE student_id = ?";
        try (Connection conn = Db.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Prefs p = new Prefs();
                p.hours = rs.getInt("weekly_hours");
                p.when = rs.getString("time_window");
                p.free = rs.getString("free_days");
                p.focus = rs.getString("focus_courses");
                return p;
            }
        }
    }

    public static void savePreferences(int studentId, Prefs p) throws SQLException {
        try (Connection conn = Db.connect()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM preferences WHERE student_id = ?")) {
                    del.setInt(1, studentId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO preferences (student_id, weekly_hours, time_window, free_days, focus_courses) "
                                + "VALUES (?, ?, ?, ?, ?)")) {
                    ins.setInt(1, studentId);
                    ins.setInt(2, p.hours);
                    ins.setString(3, p.when);
                    ins.setString(4, p.free);
                    ins.setString(5, p.focus);
                    ins.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
