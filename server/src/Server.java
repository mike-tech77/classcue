import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class Server {

    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/courses", Server::courses);
        server.createContext("/api/timetable", Server::timetable);
        server.createContext("/api/plan", Server::plan);
        server.createContext("/", Server::page);          // everything else: the web page files
        server.start();
        System.out.println("ClassCue running. Open http://localhost:8080");
        System.out.println("Serving the web page from: " + Paths.get("web").toAbsolutePath());
    }

    // GET /  ->  web/index.html, and /style.css, /app.js from the web folder
    static void page(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        Path webFolder = Paths.get("web");
        Path file = webFolder.resolve(path.substring(1)).normalize();

        // refuse anything that climbs out of the web folder (such as ../secret)
        if (!file.startsWith(webFolder) || !Files.isRegularFile(file)) {
            byte[] msg = "Page not found".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, msg.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(msg);
            }
            return;
        }

        String name = file.getFileName().toString();
        String type = "application/octet-stream";
        if (name.endsWith(".html")) type = "text/html; charset=utf-8";
        if (name.endsWith(".css")) type = "text/css; charset=utf-8";
        if (name.endsWith(".js")) type = "text/javascript; charset=utf-8";

        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
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
        String query = ex.getRequestURI().getRawQuery();
        Map<String, String> registered = parseReg(getParam(query, "reg"));

        try {
            List<String> slotJson = new ArrayList<>();
            List<String> clashJson = new ArrayList<>();
            for (String day : DAYS) {
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

    // GET /api/plan?reg=...&hours=8&from=17&to=22&free=Friday&focus=CE 273,CE 277
    // -> {"blocks":[{"day":..., "start":"18:00", "end":"19:00", "course":"CE 273"}, ...]}
    static void plan(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getRawQuery();
        Map<String, String> registered = parseReg(getParam(query, "reg"));

        int hours = Math.max(0, Math.min(40, intParam(query, "hours", 8)));
        int from = Math.max(0, Math.min(24, intParam(query, "from", 17)));
        int to = Math.max(0, Math.min(24, intParam(query, "to", 22)));
        List<String> free = splitList(getParam(query, "free"));
        List<String> focus = splitList(getParam(query, "focus"));
        if (focus.isEmpty()) {
            focus = new ArrayList<>(new TreeSet<>(registered.keySet()));   // no choice: use every course
        }
        if (focus.isEmpty() || to <= from) {
            send(ex, 200, "{\"blocks\":[]}");
            return;
        }

        try {
            Map<String, List<TimetableSlot>> week = new LinkedHashMap<>();
            for (String day : DAYS) {
                week.put(day, StudentFilter.filter(Main.loadByDay(day), registered));
            }
            List<String> blockJson = new ArrayList<>();
            for (Planner.StudyBlock b : Planner.planBlocks(week, hours, from * 60, to * 60,
                    new HashSet<>(free), focus)) {
                blockJson.add("{\"day\":" + q(b.day) + ",\"start\":" + q(b.start)
                        + ",\"end\":" + q(b.end) + ",\"course\":" + q(b.course) + "}");
            }
            send(ex, 200, "{\"blocks\":[" + String.join(",", blockJson) + "]}");
        } catch (SQLException e) {
            send(ex, 500, "{\"error\":" + q(e.getMessage()) + "}");
        }
    }

    // "CE 251:,CE 271:A" -> {"CE 251": "", "CE 271": "A"}
    static Map<String, String> parseReg(String reg) {
        Map<String, String> registered = new HashMap<>();
        if (reg != null && !reg.isEmpty()) {
            for (String part : reg.split(",")) {
                String[] pair = part.split(":", -1);
                registered.put(pair[0].trim(), pair.length > 1 ? pair[1].trim() : "");
            }
        }
        return registered;
    }

    // "a,b,c" -> [a, b, c]  (empty or missing -> empty list)
    static List<String> splitList(String text) {
        List<String> items = new ArrayList<>();
        if (text != null) {
            for (String part : text.split(",")) {
                if (!part.trim().isEmpty()) {
                    items.add(part.trim());
                }
            }
        }
        return items;
    }

    static int intParam(String rawQuery, String name, int fallback) {
        try {
            return Integer.parseInt(getParam(rawQuery, name).trim());
        } catch (Exception e) {
            return fallback;                     // missing or not a number
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
