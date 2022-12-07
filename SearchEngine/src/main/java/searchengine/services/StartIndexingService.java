package searchengine.services;

import searchengine.dto.indexing.IndexingResponse;

public interface StartIndexingService {

    boolean siteListIndexing();

    IndexingResponse startIndexing() throws InterruptedException;
}
