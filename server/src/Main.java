import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
        try {
            for (TimetableSlot slot : loadByDay("Wednesday")) {
                System.out.println(slot);
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}