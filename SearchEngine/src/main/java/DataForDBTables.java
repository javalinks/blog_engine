import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class DataForDBTables {

    private final String url;
    private Map<String, Map<String, Integer>> siteMapWithContent;
    private static final StringBuilder builderPage = new StringBuilder();
    private static final StringBuilder builderLemma = new StringBuilder();
    private static final StringBuilder builderIndex = new StringBuilder();

    DBConnection dbConnection = new DBConnection();

    public DataForDBTables(String url) {
        this.url = url;
    }

    public void setsiteMapWithContent() {
        this.siteMapWithContent = new ForkJoinPool().invoke(new SiteMap(url));
    }

    public Map<String, Map<String, Integer>> getsiteMapWithContent() {
        return siteMapWithContent;
    }

    public static StringBuilder getBuilderPage() {
        return builderPage;
    }

    public static StringBuilder getBuilderLemma() {
        return builderLemma;
    }

    public static StringBuilder getBuilderIndex() {
        return builderIndex;
    }

    private String SubString(String link) {
        return link.replaceFirst("http(s)?://(www\\.)?\\w+\\.\\w+",
                "");
    }

    public void dataToTables(Map<String, Map<String, Integer>> dataToPage) throws SQLException, IOException {
        Map<String, Integer> contentAndStatusCode;
        Lemma lemma = new Lemma();
        Document document;

        for (Map.Entry<String, Map<String, Integer>> item : dataToPage.entrySet()) {
            String link = SubString(item.getKey());
            contentAndStatusCode = item.getValue();
            int statusCode = 0;
            String content = null;
            for (Map.Entry<String, Integer> contentItem : contentAndStatusCode.entrySet()) {
                content = contentItem.getKey().replaceAll("'+|,+", "");
                statusCode = contentItem.getValue();
                document = Jsoup.parse(content);
                lemma.setLemmaList(document);
                document.clearAttributes();
                dataToLemmaTable(lemma.getLemmaList());
                dbConnection.insertIntoLemma();
                lemma.clearLemmaList();
            }
            dataToPageTable(link, statusCode, content);
            contentAndStatusCode.clear();
        }
        dbConnection.insertIntoPage();
    }

    private void dataToPageTable(String link, int statusCode, String content) throws SQLException {
        builderPage.append(builderPage.length() == 0 ? "" : ",").append("('").append(link)
                .append("', '").append(statusCode)
                .append("', '").append(content)
                .append("')");
        if (builderPage.length() > 2000000) {
            dbConnection.insertIntoPage();
            builderPage.delete(0, builderPage.length());
        }
    }

    private void dataToLemmaTable(Set<String> lemmaSet) {
        for (String lemma : lemmaSet) {
            builderLemma.append(builderLemma.length() == 0 ? "" : ",").append("('").append(lemma).append("', 1)");
        }
    }

    public void dataToIndexTable() throws SQLException, IOException {
        Map<Integer, String> idAndContentFromPageTable = new HashMap<>(dbConnection.idAndContentFromPageTable());
        Map<Integer, String> idAndLemmaFromLemmaTable = new HashMap<>(dbConnection.idAndLemmaFromLemmaTable());
        Index index = new Index();
        Document document;

        for (Map.Entry<Integer, String> contentItem : idAndContentFromPageTable.entrySet()) {
            int pageId = contentItem.getKey();
            document = Jsoup.parse(contentItem.getValue());
            index.setLemmaMap(document);
            index.rankCalculate();
            for (Map.Entry<Integer, String> idAndLemmaItem : idAndLemmaFromLemmaTable.entrySet()) {
                for (Map.Entry<String, Float> lemmaItem : index.getLemmaRank().entrySet()) {
                    if (lemmaItem.getKey().equals(idAndLemmaItem.getValue())) {
                        int lemmaId = idAndLemmaItem.getKey();
                        float rank = lemmaItem.getValue();
                        builderIndex(pageId, lemmaId, rank);
                    }
                }
            }
            index.clearLemmaMapTitle();
            index.clearLemmaMapBody();
            index.clearLemmaRank();
            document.clearAttributes();
        }
    }

    private void builderIndex(int pageId, int lemmaId, float rank) {
        builderIndex.append(builderIndex.length() == 0 ? "" : ",").append("('").append(pageId)
                .append("', '").append(lemmaId)
                .append("', '").append(rank)
                .append("')");
    }
}
