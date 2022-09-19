import java.sql.SQLException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;

public class Main {

    public static void main(String[] args) throws SQLException {

        System.setProperty("java.net.useSystemProxies", "true");

        System.out.println("Введите адрес сайта (например: https://skillbox.ru/) : ");
        Scanner scanner = new Scanner(System.in);
        String url = scanner.nextLine();
        scanner.close();

        SiteMap siteMap = new SiteMap(url);
        Map<String, Map<String, Integer>> dataToPage = new ForkJoinPool().invoke(siteMap);

        System.out.println("Количество элементов = " + dataToPage.size());

        DBConnection.getConnection();
        DBConnection.createTablePage();
        dataToPageTable(dataToPage);
        DBConnection.insertIntoPage();
    }

    public static String SubString(String link) {
        return link.replaceFirst("http(s)?://(www\\.)?\\w+\\.\\w+",
                "");
    }

    public static void dataToPageTable(Map<String, Map<String, Integer>> dataToPage) throws SQLException {
        Map<String, Integer> contentAndStatusCode;

        for (Map.Entry<String, Map<String, Integer>> item : dataToPage.entrySet()) {
            String link = SubString(item.getKey());
            contentAndStatusCode = item.getValue();
            int statusCode = 0;
            String content = null;
            for (Map.Entry<String, Integer> contentItem : contentAndStatusCode.entrySet()) {
                content = contentItem.getKey().replaceAll("'+|,+", "");
                statusCode = contentItem.getValue();
            }
            DBConnection.dataToPageTable(link, statusCode, content);
            contentAndStatusCode.clear();
        }
    }
}
