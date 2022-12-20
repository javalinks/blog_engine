package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.StartIndexingResponse;
import searchengine.dto.indexing.StopIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.SiteIndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexingService siteIndexingService;


    public ApiController(StatisticsService statisticsService, SiteIndexingService siteIndexingService) {
        this.statisticsService = statisticsService;
        this.siteIndexingService = siteIndexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<StartIndexingResponse> startIndexing() {
        return ResponseEntity.ok(siteIndexingService.startIndexing());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StopIndexingResponse> stopIndexing() {
        return ResponseEntity.ok(siteIndexingService.stopIndexing());
    }
}
