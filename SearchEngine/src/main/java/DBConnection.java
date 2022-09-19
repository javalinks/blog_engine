import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static StringBuilder insertQuery = new StringBuilder();

    private static Connection connection;

    public static void getConnection() throws SQLException {
        if (connection == null) {
            String dbName = "search_engine";
            String dbUser = "root";
            String dbPass = "testtest";
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/"
                + dbName +"?user=" + dbUser + "&password=" + dbPass);
        }
    }

    public static void createTablePage() throws SQLException {
        connection.createStatement().execute("DROP TABLE IF EXISTS page");
        connection.createStatement().execute("CREATE TABLE page(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "path TEXT NOT NULL, " +
                "code INT NOT NULL, " +
                "content MEDIUMTEXT NOT NULL, " +
                "PRIMARY KEY(id))");
        connection.createStatement().execute("CREATE INDEX path_index ON page (path(100))");
    }

    public static void insertIntoPage() throws SQLException {
        connection.createStatement().execute("INSERT INTO page(path, code, content) " +
            "VALUES" + insertQuery.toString());
    }

    public static void dataToPageTable(String link, int statusCode, String content) throws SQLException {
        insertQuery.append(insertQuery.length() == 0 ? "" : ",").append("('").append(link)
                .append("', '").append(statusCode)
                .append("', '").append(content)
                .append("')");
        if (insertQuery.length() > 2000000) {
            insertIntoPage();
            insertQuery = new StringBuilder();
        }
    }
}
