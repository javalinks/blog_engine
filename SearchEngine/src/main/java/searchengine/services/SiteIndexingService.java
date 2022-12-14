package searchengine.services;

import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;

public interface SiteIndexingService {

    StartIndexingResponse startIndexing();

    StopIndexingResponse stopIndexing();
}