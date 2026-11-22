import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Planner {

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

    public static List<String> plan(Map<String, List<TimetableSlot>> week, int weeklyHours,
                                    int from, int to, Set<String> freeDays, List<String> courses) {
        Map<String, Deque<int[]>> options = new LinkedHashMap<>();
        for (String day : week.keySet()) {
            if (freeDays.contains(day)) {
                continue;                       // student wants this day kept free
            }
            options.put(day, new ArrayDeque<>(freeHours(week.get(day), from, to)));
        }

        List<String> plan = new ArrayList<>();
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
                plan.add(day + " " + fmt(h[0]) + "-" + fmt(h[1]) + "  Study " + course);
                added = true;
            }
        }
        return plan;
    }
}