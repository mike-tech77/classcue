import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Planner {

    // One study session, e.g. Monday 18:00-19:00 for CE 273
    public static class StudyBlock {
        public final String day;
        public final String start;    // "18:00"
        public final String end;      // "19:00"
        public final String course;

        public StudyBlock(String day, String start, String end, String course) {
            this.day = day;
            this.start = start;
            this.end = end;
            this.course = course;
        }

        @Override
        public String toString() {
            return day + " " + start + "-" + end + "  Study " + course;
        }
    }

    static int toMin(String t) {          // "10:30:00" -> 630 (minutes since midnight)
        return Integer.parseInt(t.substring(0, 2)) * 60 + Integer.parseInt(t.substring(3, 5));
    }

    static String fmt(int m) {            // 630 -> "10:30"
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    // Free 1-hour blocks between classes, inside the window [from, to]
    static List<int[]> freeHours(List<TimetableSlot> classes, int from, int to) {
        List<int[]> busy = new ArrayList<>();
        for (TimetableSlot s : classes) {
            busy.add(new int[]{toMin(s.startTime), toMin(s.endTime)});
        }
        busy.sort((a, b) -> a[0] - b[0]);       // earliest class first

        List<int[]> hours = new ArrayList<>();
        int cursor = from;                      // everything before cursor is used or past
        for (int[] b : busy) {
            addHours(hours, cursor, Math.min(b[0], to));   // the gap before this class
            cursor = Math.max(cursor, b[1]);               // jump to the end of the class
        }
        addHours(hours, cursor, to);                       // the gap after the last class
        return hours;
    }

    static void addHours(List<int[]> out, int start, int end) {
        for (int t = start; t + 60 <= end; t += 60) {
            out.add(new int[]{t, t + 60});
        }
    }

    // The planner itself: returns the study blocks as objects
    public static List<StudyBlock> planBlocks(Map<String, List<TimetableSlot>> week, int weeklyHours,
                                              int from, int to, Set<String> freeDays, List<String> courses) {
        Map<String, Deque<int[]>> options = new LinkedHashMap<>();
        for (String day : week.keySet()) {
            if (freeDays.contains(day)) {
                continue;                       // student wants this day kept free
            }
            options.put(day, new ArrayDeque<>(freeHours(week.get(day), from, to)));
        }

        List<StudyBlock> plan = new ArrayList<>();
        boolean added = true;
        while (plan.size() < weeklyHours && added) {
            added = false;
            for (String day : options.keySet()) {           // one block per day per pass
                Deque<int[]> queue = options.get(day);
                if (queue.isEmpty() || plan.size() >= weeklyHours) {
                    continue;
                }
                int[] h = queue.poll();
                String course = courses.get(plan.size() % courses.size());
                plan.add(new StudyBlock(day, fmt(h[0]), fmt(h[1]), course));
                added = true;
            }
        }
        return plan;
    }

    // Same plan as plain text lines (used by Main)
    public static List<String> plan(Map<String, List<TimetableSlot>> week, int weeklyHours,
                                    int from, int to, Set<String> freeDays, List<String> courses) {
        List<String> lines = new ArrayList<>();
        for (StudyBlock b : planBlocks(week, weeklyHours, from, to, freeDays, courses)) {
            lines.add(b.toString());
        }
        return lines;
    }
}
