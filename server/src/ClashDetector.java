import java.util.ArrayList;
import java.util.List;

public class ClashDetector {

    // true if the two slots overlap in time (the caller checks they are on the same day)
    public static boolean overlaps(TimetableSlot a, TimetableSlot b) {
        return a.startTime.compareTo(b.endTime) < 0
                && b.startTime.compareTo(a.endTime) < 0;
    }

    public static List<String> findClashes(List<TimetableSlot> slots) {
        List<String> clashes = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                TimetableSlot a = slots.get(i);
                TimetableSlot b = slots.get(j);

                if (a.courseCode.equals(b.courseCode)) {
                    continue;   // groups of the same course are not a clash
                }
                if (overlaps(a, b)) {
                    clashes.add(a.day + ": " + a.courseCode + " "
                            + (a.groupLabel == null ? "" : a.groupLabel)
                            + " clashes with " + b.courseCode + " "
                            + (b.groupLabel == null ? "" : b.groupLabel)
                            + " (" + a.startTime + "-" + a.endTime + ")");
                }
            }
        }
        return clashes;
    }
}