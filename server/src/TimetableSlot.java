public class TimetableSlot {
    public String courseCode;
    public String groupLabel;   // null means the whole class attends
    public String day;
    public String startTime;
    public String endTime;
    public String venue;
    public String lecturer;

    @Override
    public String toString() {
        String group = (groupLabel == null) ? "" : " " + groupLabel;
        return courseCode + group + "  " + day + "  " + startTime + "-" + endTime
                + "  " + venue + "  " + lecturer;
    }
}