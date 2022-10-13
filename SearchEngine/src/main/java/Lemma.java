import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Lemma {

    private final Map<String, Float> htmlTagsWeightMap = new HashMap<>(DBConnection.htmlTagsandWeight());
    private final Set<String> lemmaList = new HashSet<>();

    public Lemma() throws SQLException {
    }

    public Set<String> getLemmaList() {
        return lemmaList;
    }

    public Map<String, Float> getHtmlTagsWeightMap() {
        return htmlTagsWeightMap;
    }

    public void setLemmaList(Document doc) throws IOException {
        for (Map.Entry<String, Float> htmlTag : htmlTagsWeightMap.entrySet()) {
            String lemmaSet = Objects.requireNonNull(doc.selectFirst(htmlTag.getKey())).toString();
            LemmaFinder finder = LemmaFinder.getInstance();
            lemmaList.addAll(finder.getLemmaSet(lemmaSet));
        }
    }

    public void clearLemmaList() {
        lemmaList.clear();
    }
}
