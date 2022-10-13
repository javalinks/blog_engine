import java.sql.*;
import java.util.*;

public class DBConnection {
    private static Connection connection;

    public void getConnection() throws SQLException {
        if (connection == null) {
            String dbName = "search_engine";
            String dbUser = "root";
            String dbPass = "testtest";
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/"
                + dbName +"?user=" + dbUser + "&password=" + dbPass);
            connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");
            connection.createStatement().execute("DROP TABLE IF EXISTS page");
            connection.createStatement().execute("DROP TABLE IF EXISTS field");
            connection.createStatement().execute("DROP TABLE IF EXISTS lemma");
            connection.createStatement().execute("DROP TABLE IF EXISTS `index`");
            connection.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");
            connection.createStatement().close();
        }
    }

    public void createTablePage() throws SQLException {
        connection.createStatement().execute("CREATE TABLE page(" +
                "id INT NOT NULL AUTO_INCREMENT, " +
                "path TEXT NOT NULL, " +
                "code INT NOT NULL, " +
                "content MEDIUMTEXT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "KEY(path(100)))");
        connection.createStatement().close();
    }

    public void insertIntoPage() throws SQLException {
        connection.createStatement().execute("INSERT INTO page(path, code, content) " +
            "VALUES" + DataForDBTables.getBuilderPage().toString());
        connection.createStatement().close();
    }

    public void createTableField() throws SQLException {
        connection.createStatement().execute("CREATE TABLE field(" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "name VARCHAR(255) NOT NULL, " +
                "selector VARCHAR(255) NOT NULL, " +
                "weight FLOAT NOT NULL, " +
                "CONSTRAINT check_weight CHECK(weight>=0.0 and weight<=1.0), " +
                "PRIMARY KEY(id))");
        connection.createStatement().close();
    }

    public void insertIntoField() throws SQLException {
        connection.createStatement().execute("INSERT INTO field(name, selector, weight) VALUES" +
                "('title', 'title', 1.0)," +
                "('body', 'body', 0.8)");
        connection.createStatement().close();
    }

    public void createTableLemma() throws SQLException {
        connection.createStatement().execute("CREATE TABLE lemma(" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "lemma VARCHAR(255) NOT NULL, " +
                "frequency INT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "UNIQUE KEY(lemma))");
        connection.createStatement().close();
    }

    public void createTableIndex() throws SQLException {
        connection.createStatement().execute("CREATE TABLE `index`(" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "page_id INT NOT NULL, " +
                "lemma_id INT NOT NULL, " +
                "`rank` FLOAT NOT NULL, " +
                "PRIMARY KEY(id), " +
                "FOREIGN KEY(page_id) REFERENCES page(id), " +
                "FOREIGN KEY(lemma_id) REFERENCES lemma(id))");
        connection.createStatement().close();
    }

    public static Map<String, Float> htmlTagsandWeight() throws SQLException {
        Map<String, Float> htmlTagsWeightMap = new HashMap<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM field");
        while (resultSet.next()) {
            htmlTagsWeightMap.put(resultSet.getString("selector"), resultSet.getFloat("weight"));
        }
        resultSet.close();
        return htmlTagsWeightMap;
    }

    public void insertIntoLemma() throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT id FROM lemma LIMIT 1");
        if (resultSet.next() && !DataForDBTables.getBuilderLemma().isEmpty()) {
            connection.createStatement().execute("INSERT INTO lemma(lemma, frequency) " +
                    "VALUES" + DataForDBTables.getBuilderLemma() + "ON DUPLICATE KEY UPDATE frequency = frequency + 1");
        }
        else if (!DataForDBTables.getBuilderLemma().isEmpty()) {
            connection.createStatement().execute("INSERT INTO lemma(lemma, frequency) " +
                    "VALUES" + DataForDBTables.getBuilderLemma());
        }
        resultSet.close();
        connection.createStatement().close();
        DataForDBTables.getBuilderLemma().delete(0, DataForDBTables.getBuilderLemma().length());
    }

    public Map<Integer, String> idAndContentFromPageTable() throws SQLException {
        Map<Integer, String> idAndContent = new HashMap<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT id, content FROM page");
        while (resultSet.next()) {
            idAndContent.put(resultSet.getInt("id"), resultSet.getString("content"));
        }
        resultSet.close();
        return idAndContent;
    }

    public Map<Integer, String> idAndLemmaFromLemmaTable() throws SQLException {
        Map<Integer, String> idAndLemma = new HashMap<>();
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT id, lemma FROM lemma");
        while (resultSet.next()) {
            idAndLemma.put(resultSet.getInt("id"), resultSet.getString("lemma"));
        }
        resultSet.close();
        return idAndLemma;
    }

    public void insertIntoIndex() throws SQLException {
        connection.createStatement().execute("INSERT INTO `index`(page_id, lemma_id, `rank`) " +
                "VALUES" + DataForDBTables.getBuilderIndex().toString());
        connection.createStatement().close();
    }
}
