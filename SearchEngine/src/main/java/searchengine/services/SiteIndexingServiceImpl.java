package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.SiteMap;
import searchengine.dto.indexing.StopIndexingResponse;
import searchengine.model.DbSite;
import searchengine.model.Page;
import searchengine.model.Status;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private final SitesList sites;

    private final List<Thread> threads;

    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private SiteRepository siteRepository;

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

    private boolean siteListIndexing() {
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
                threads.add(Thread.currentThread());
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        List<Page> pageList = new ArrayList<>(dataForPageTable(dbSite.getUrl()));
                        pageRepository.saveAll(pageList);
                        dbSite.setStatusTime(new Date());
                        dbSite.setStatus(Status.INDEXED);
                        siteRepository.save(dbSite);
                        throw new InterruptedException();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    catch (Exception ex) {
                        dbSite.setLastError(String.valueOf(ex.getMessage()));
                        dbSite.setStatus(Status.FAILED);
                        dbSite.setStatusTime(new Date());
                        siteRepository.save(dbSite);
                    }
                }
            }).start();
        }
        return true;
    }

    @Override
    public StartIndexingResponse startIndexing() {
        StartIndexingResponse startIndexingResponse = new StartIndexingResponse();
        if (siteRepository.indexingStatus().contains("INDEXING")) {
            startIndexingResponse.setResult(false);
            startIndexingResponse.setError("Индексация уже запущена");
            return startIndexingResponse;
        }
        else if (siteListIndexing()) {
            startIndexingResponse.setResult(true);
            startIndexingResponse.setError("");
            return startIndexingResponse;
        }
        return startIndexingResponse;
    }

    @Override
    public StopIndexingResponse stopIndexing() {
        StopIndexingResponse stopIndexingResponse = new StopIndexingResponse();
        for (Thread thread : threads) {
            try {
                thread.interrupt();
                throw new InterruptedException();
            } catch (InterruptedException e) {
                stopIndexingResponse.setResult(true);
                stopIndexingResponse.setError("");
            }
        }
        if (siteRepository.indexingStatus().contains("INDEXING")) {
            List<DbSite> dbSites = siteRepository.indexingSite("INDEXING");
            for (DbSite dbSite : dbSites) {
                dbSite.setStatus(Status.FAILED);
                dbSite.setStatusTime(new Date());
                dbSite.setLastError("Индексация остановлена пользователем");
                siteRepository.save(dbSite);
            }
            return stopIndexingResponse;
        }
        else if (!siteRepository.indexingStatus().contains("INDEXING")) {
            stopIndexingResponse.setResult(false);
            stopIndexingResponse.setError("Индексация не запущена");
            return stopIndexingResponse;
        }
        return stopIndexingResponse;
    }
}
