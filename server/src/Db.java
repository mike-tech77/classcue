import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    private static final String URL = "jdbc:mysql://localhost:3306/classcue";
    private static final String USER = "root";

    public static Connection connect() throws SQLException {
        String password = System.getenv("CLASSCUE_DB_PASSWORD");
        return DriverManager.getConnection(URL, USER, password);
    }
}