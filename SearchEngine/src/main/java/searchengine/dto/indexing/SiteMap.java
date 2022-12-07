package searchengine.dto.indexing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.RecursiveTask;

public class SiteMap extends RecursiveTask<Map<String, Map<String, Integer>>> {
    private final String url;
    private final Map<String, Map<String, Integer>> pageLinks = new ConcurrentSkipListMap<>();
    private final Map<String, Map<String, Integer>> linksSet = new ConcurrentSkipListMap<>();
    public SiteMap(String url) {
        this.url = url;
    }

    private Map<String, Map<String, Integer>> getPageLinks(String url) {
        pageLinks.clear();
        try {
            Thread.sleep(300);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)" +
                            " Chrome/102.0.5005.167")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String absUrl = (link.attr("abs:href"));
                if (absUrl.startsWith(url) && absUrl.endsWith("/")
                        && !absUrl.endsWith(".pdf")) {
                    Connection.Response response = Jsoup.connect(absUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/102.0.5005.167")
                            .referrer("http://www.google.com")
                            .ignoreHttpErrors(true)
                            .execute();
                    int statusCode = response.statusCode();
                    String content = response.body();
                    Map<String, Integer> contentAndStatusCode = new HashMap<>();
                    contentAndStatusCode.put(content, statusCode);
                    pageLinks.putIfAbsent(absUrl, contentAndStatusCode);
                    linksSet.putIfAbsent(absUrl, contentAndStatusCode);
                }
            }
            pageLinks.remove(url);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        return pageLinks;
    }

    @Override
    protected Map<String, Map<String, Integer>> compute() {
        Map<String, Map<String, Integer>> urlSet = new HashMap<>(getPageLinks(url));
        List<SiteMap> taskList = new ArrayList<>();

        for (String childUrl : urlSet.keySet()) {
            SiteMap task = new SiteMap(childUrl);
            task.fork();
            taskList.add(task);
        }

        for (SiteMap item : taskList) {
            linksSet.putAll(item.join());
        }
        return linksSet;
    }
}
