import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StudentFilter {

    // registered maps a course code to the student's group ("A", "B"), or "" if the course has no groups
    public static List<TimetableSlot> filter(List<TimetableSlot> all, Map<String, String> registered) {
        List<TimetableSlot> mine = new ArrayList<>();
        for (TimetableSlot slot : all) {
            if (!registered.containsKey(slot.courseCode)) {
                continue;   // not registered for this course
            }
            String chosenGroup = registered.get(slot.courseCode);
            if (slot.groupLabel == null || slot.groupLabel.equals(chosenGroup)) {
                mine.add(slot);
            }
        }
        return mine;
    }
}