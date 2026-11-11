import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static List<TimetableSlot> loadByDay(String day) throws SQLException {
        String sql = "SELECT c.code, s.group_label, s.day, s.start_time, s.end_time, "
                + "s.venue, s.lecturer "
                + "FROM timetable_slots s "
                + "JOIN courses c ON c.id = s.course_id "
                + "WHERE s.day = ? "
                + "ORDER BY s.start_time";

        List<TimetableSlot> slots = new ArrayList<>();
        try (Connection conn = Db.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, day);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TimetableSlot slot = new TimetableSlot();
                    slot.courseCode = rs.getString("code");
                    slot.groupLabel = rs.getString("group_label");
                    slot.day = rs.getString("day");
                    slot.startTime = rs.getString("start_time");
                    slot.endTime = rs.getString("end_time");
                    slot.venue = rs.getString("venue");
                    slot.lecturer = rs.getString("lecturer");
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    public static void main(String[] args) {
        System.out.println("ClassCue server starting...");

        // The student's registration (hard-coded for now; the web page will fill this in later)
        Map<String, String> registered = new HashMap<>();
        registered.put("CE 251", "");
        registered.put("CE 265", "");
        registered.put("CE 271", "A");
        registered.put("CE 273", "A");
        registered.put("CE 275", "");
        registered.put("CE 277", "B");
        registered.put("CE 279", "");
        registered.put("CE 281", "A");

        try {
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            int total = 0;
            for (String day : days) {
                List<TimetableSlot> mine = StudentFilter.filter(loadByDay(day), registered);
                total = total + mine.size();

                System.out.println("--- " + day + " ---");
                for (TimetableSlot slot : mine) {
                    System.out.println(slot);
                }
                for (String clash : ClashDetector.findClashes(mine)) {
                    System.out.println("CLASH: " + clash);
                }
            }
            System.out.println("Classes in my timetable: " + total);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}