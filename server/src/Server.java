import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/courses", Server::courses);
        server.createContext("/api/timetable", Server::timetable);
        server.start();
        System.out.println("ClassCue API running on http://localhost:8080");
    }

    // GET /api/courses  ->  [{"code":..., "title":..., "credits":..., "groups":["A","B"]}, ...]
    static void courses(HttpExchange ex) throws IOException {
        String sql = "SELECT c.code, c.title, c.credits, "
                + "GROUP_CONCAT(DISTINCT s.group_label ORDER BY s.group_label) AS grp "
                + "FROM courses c LEFT JOIN timetable_slots s ON s.course_id = c.id "
                + "GROUP BY c.id ORDER BY c.code";
        try (Connection conn = Db.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> items = new ArrayList<>();
            while (rs.next()) {
                String grp = rs.getString("grp");
                String groups = (grp == null) ? "" : "\"" + grp.replace(",", "\",\"") + "\"";
                items.add("{\"code\":" + q(rs.getString("code"))
                        + ",\"title\":" + q(rs.getString("title"))
                        + ",\"credits\":" + rs.getInt("credits")
                        + ",\"groups\":[" + groups + "]}");
            }
            send(ex, 200, "[" + String.join(",", items) + "]");
        } catch (SQLException e) {
            send(ex, 500, "{\"error\":" + q(e.getMessage()) + "}");
        }
    }

    // GET /api/timetable?reg=CE 251:,CE 271:A,CE 277:B
    // -> {"slots":[...], "clashes":[...]}   (only the student's classes)
    static void timetable(HttpExchange ex) throws IOException {
        String reg = getParam(ex.getRequestURI().getRawQuery(), "reg");
        Map<String, String> registered = new HashMap<>();
        if (reg != null && !reg.isEmpty()) {
            for (String part : reg.split(",")) {
                String[] pair = part.split(":", -1);        // "CE 271:A" -> ["CE 271", "A"]
                registered.put(pair[0].trim(), pair.length > 1 ? pair[1].trim() : "");
            }
        }

        try {
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
            List<String> slotJson = new ArrayList<>();
            List<String> clashJson = new ArrayList<>();
            for (String day : days) {
                List<TimetableSlot> mine = StudentFilter.filter(Main.loadByDay(day), registered);
                for (TimetableSlot s : mine) {
                    slotJson.add("{\"course\":" + q(s.courseCode)
                            + ",\"group\":" + q(s.groupLabel)
                            + ",\"day\":" + q(s.day)
                            + ",\"start\":" + q(s.startTime)
                            + ",\"end\":" + q(s.endTime)
                            + ",\"venue\":" + q(s.venue)
                            + ",\"lecturer\":" + q(s.lecturer) + "}");
                }
                for (String c : ClashDetector.findClashes(mine)) {
                    clashJson.add(q(c));
                }
            }
            send(ex, 200, "{\"slots\":[" + String.join(",", slotJson) + "],\"clashes\":["
                    + String.join(",", clashJson) + "]}");
        } catch (SQLException e) {
            send(ex, 500, "{\"error\":" + q(e.getMessage()) + "}");
        }
    }

    // finds name=value in a raw query string and decodes it (%20 -> space)
    static String getParam(String rawQuery, String name) {
        if (rawQuery == null) return null;
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // turns text into a JSON string, or null
    static String q(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    static void send(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}