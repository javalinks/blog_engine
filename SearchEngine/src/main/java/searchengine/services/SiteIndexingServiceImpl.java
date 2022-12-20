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
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class SiteIndexingServiceImpl implements SiteIndexingService {

    private final SitesList sites;

    private final List<ForkJoinPool> forkJoinPoolList;

    private final AtomicBoolean interruptionCheck = new AtomicBoolean(false);

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
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPoolList.add(forkJoinPool);
        Map<String, Map<String, Integer>> siteMapWithContent = forkJoinPool.invoke(new SiteMap(url));
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

    private boolean indexingStart() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        for (Site site : sites.getSites()) {
            DbSite dbSite = new DbSite();
            dbSite.setStatus(Status.INDEXING);
            dbSite.setStatusTime(new Date());
            dbSite.setUrl(site.getUrl());
            dbSite.setName(site.getName());
            siteRepository.save(dbSite);
        }
        return true;
    }

    private void siteListIndexing() {
        for (Site site : sites.getSites()) {
            DbSite dbSite = siteRepository.getDbSiteByUrl(site.getUrl());
            new Thread(() -> {
                List<Page> pageList = new ArrayList<>();
                try {
                    pageList = (dataForPageTable(site.getUrl()));
                    throw new InterruptedException();
                }
                catch (RuntimeException ex) {
                    dbSite.setStatus(Status.FAILED);
                    dbSite.setStatusTime(new Date());
                    dbSite.setLastError("Индексация остановлена пользователем");
                    siteRepository.save(dbSite);
                }
                catch (Exception ex) {
                    dbSite.setLastError(String.valueOf(ex.getMessage()));
                    dbSite.setStatus(Status.FAILED);
                    dbSite.setStatusTime(new Date());
                    siteRepository.save(dbSite);
                }
                finally {
                    if (!interruptionCheck.get()) {
                        pageRepository.saveAll(pageList);
                        dbSite.setStatusTime(new Date());
                        dbSite.setStatus(Status.INDEXED);
                        dbSite.setLastError("NULL");
                        siteRepository.save(dbSite);
                    }
                }
            }).start();
        }
    }

    @Override
    public StartIndexingResponse startIndexing() {
        StartIndexingResponse startIndexingResponse = new StartIndexingResponse();
        if (indexingStart()) {
            startIndexingResponse.setResult(true);
            startIndexingResponse.setError("");
            siteListIndexing();
            return startIndexingResponse;
        }
        else if (siteRepository.indexingStatus().contains("INDEXING")) {
            startIndexingResponse.setResult(false);
            startIndexingResponse.setError("Индексация уже запущена");
            return startIndexingResponse;
        }
        return startIndexingResponse;
    }

    @Override
    public StopIndexingResponse stopIndexing() {
        StopIndexingResponse stopIndexingResponse = new StopIndexingResponse();
        if (siteRepository.indexingStatus().contains("INDEXING")) {
            forkJoinPoolList.forEach(pool -> {
                if (!pool.isTerminated()) {
                    pool.shutdownNow();
                    interruptionCheck.set(true);
                } else {
                    interruptionCheck.set(false);
                }
            });
            stopIndexingResponse.setResult(true);
            stopIndexingResponse.setError("");
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
