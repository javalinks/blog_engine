package searchengine.services;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.ExceptionCatch;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.SiteMap;
import searchengine.model.DbSite;
import searchengine.model.Page;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@AllArgsConstructor
public class StartIndexingServiceImpl implements StartIndexingService {

    private final SitesList sites;

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

    private List<String> statusIndex;

    private String urlTrim(String link) {
        return link.replaceFirst("http(s)?://(www\\.)?\\w+\\.\\w+",
                "");
    }

    private DbSite getDbSite(String url) {
        return siteRepository.getDbSiteByUrl(url);
    }

    private List<Page> dataForPageTable(String url) {
        List<Page> pageList = new ArrayList<>();
        Map<String, Map<String, Integer>> siteMapWithContent = new ForkJoinPool().
                invoke(new SiteMap(url));
        Map<String, Integer> contentAndStatusCode;
        for (Map.Entry<String, Map<String, Integer>> item : siteMapWithContent.entrySet()) {
            String link = urlTrim(item.getKey());
            contentAndStatusCode = item.getValue();
            int statusCode = 0;
            String content = null;
            for (Map.Entry<String, Integer> contentItem : contentAndStatusCode.entrySet()) {
                content = contentItem.getKey().replaceAll("'+|,+", "");
                statusCode = contentItem.getValue();
            }
            Page page = new Page();
            page.setSite(getDbSite(url));
            page.setCode(statusCode);
            page.setContent(content);
            page.setPath(link);
            pageList.add(page);
            contentAndStatusCode.clear();
        }
        return pageList;
    }


    @Override
    public boolean siteListIndexing() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        for (Site site : sites.getSites()) {
            DbSite dbSite = new DbSite();
            dbSite.setStatus(Status.INDEXING);
            dbSite.setStatusTime(new Date());
            dbSite.setUrl(site.getUrl());
            dbSite.setName(site.getName());
            siteRepository.save(dbSite);
            new Thread(() -> {
                try {
                    List<Page> pageList = new ArrayList<>(dataForPageTable(dbSite.getUrl()));
                    pageRepository.saveAll(pageList);
                    dbSite.setStatusTime(new Date());
                    dbSite.setStatus(Status.INDEXED);
                    siteRepository.save(dbSite);
                } catch (Exception ex) {
                    dbSite.setLastError(String.valueOf(ex.getMessage()));
                    dbSite.setStatus(Status.FAILED);
                    dbSite.setStatusTime(new Date());
                    siteRepository.save(dbSite);
                }
            }).start();
        }
        return siteRepository.indexingStatus().contains("INDEXING");
    }

    @Override
    public IndexingResponse startIndexing() throws InterruptedException {
        IndexingResponse indexingResponse = new IndexingResponse();
        try {
            siteListIndexing();
            if (siteRepository.indexingStatus().contains("INDEXING"))
                throw new ExceptionCatch();
        } catch (ExceptionCatch e) {
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация уже запущена");
            return indexingResponse;
        }
        finally {
            statusIndex.addAll(siteRepository.indexingStatus());
            while (statusIndex.contains("INDEXING")) {
                Thread.sleep(2000);
                statusIndex.clear();
                statusIndex.addAll(siteRepository.indexingStatus());
            }
            indexingResponse.setResult(true);
            indexingResponse.setError("");
        }
        return indexingResponse;
    }
}
