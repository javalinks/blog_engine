import org.jsoup.nodes.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Index {
    private final Map<String, Integer> lemmaMapTitle = new HashMap<>();
    private final Map<String, Integer> lemmaMapBody = new HashMap<>();
    private final Map<String, Float> lemmaRank = new HashMap<>();

    public Index() throws SQLException {
    }

    Lemma lemma = new Lemma();

    public Map<String, Float> getLemmaRank() {
        return lemmaRank;
    }

    public void clearLemmaMapTitle() {
        lemmaMapTitle.clear();
    }

    public void clearLemmaMapBody() {
        lemmaMapBody.clear();
    }

    public void clearLemmaRank() {
        lemmaRank.clear();
    }

    public void setLemmaMap(Document doc) throws IOException {
        for (Map.Entry<String, Float> htmlTag : lemma.getHtmlTagsWeightMap().entrySet()) {
            String lemma = Objects.requireNonNull(doc.selectFirst(htmlTag.getKey())).toString();
            LemmaFinder finder = LemmaFinder.getInstance();
            if (htmlTag.getKey().equals("title")) {
                lemmaMapTitle.putAll(finder.collectLemmas(lemma));
            }
            else if (htmlTag.getKey().equals("body")) {
                lemmaMapBody.putAll(finder.collectLemmas(lemma));
            }
        }
    }

    public void rankCalculate() {
        Float titleWeight = 1.0F;
        Float bodyWeight = 1.0F;
        float rank;
        for (Map.Entry<String, Float> item : lemma.getHtmlTagsWeightMap().entrySet()) {
            if (item.getKey().equals("title")) {
                titleWeight = item.getValue();
            }
            else if (item.getKey().equals("body")) {
                bodyWeight = item.getValue();
            }
        }
        for (Map.Entry<String, Integer> titleItem : lemmaMapTitle.entrySet()) {
            String lemmaTitle = titleItem.getKey();
            rank = titleWeight * titleItem.getValue();
            for (Map.Entry<String, Integer> bodyItem : lemmaMapBody.entrySet()) {
                if (Objects.equals(bodyItem.getKey(), lemmaTitle)) {
                    rank = rank + bodyWeight * bodyItem.getValue();
                }
                else {
                    rank = bodyWeight * bodyItem.getValue();
                }
                lemmaRank.put(bodyItem.getKey(), rank);
            }
            lemmaRank.putIfAbsent(lemmaTitle, rank);
        }
    }
}
