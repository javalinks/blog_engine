import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws SQLException, IOException {

        System.setProperty("java.net.useSystemProxies", "true");

        System.out.println("Введите адрес сайта (например: https://skillbox.ru/) : ");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.nextLine();
        scanner.close();

        DataForDBTables dataForTables = new DataForDBTables(url);
        dataForTables.setsiteMapWithContent();

        DBConnection dbConnection = new DBConnection();

        dbConnection.getConnection();
        dbConnection.createTableField();
        dbConnection.insertIntoField();
        dbConnection.createTableLemma();
        dbConnection.createTablePage();
        dataForTables.dataToTables(dataForTables.getsiteMapWithContent());
        dbConnection.createTableIndex();
        dataForTables.dataToIndexTable();
        dbConnection.insertIntoIndex();
    }
}
