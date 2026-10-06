package com.example.fucai.controller;

import com.example.fucai.entity.DltRecord;
import com.example.fucai.service.DltCrawlerService;
import com.example.fucai.service.DltRecommendationFilterRequest;
import com.example.fucai.service.DltRecommendationFilterService;
import com.example.fucai.service.RepeatStat;
import com.example.fucai.service.SsqFetchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/dlt")
public class DltController {

    private final DltCrawlerService crawlerService;
    private final DltRecommendationFilterService recommendationFilterService;

    public DltController(
            DltCrawlerService crawlerService,
            DltRecommendationFilterService recommendationFilterService
    ) {
        this.crawlerService = crawlerService;
        this.recommendationFilterService = recommendationFilterService;
    }

    @PostMapping("/sync-history")
    public SsqFetchResult syncHistory() throws IOException {
        return crawlerService.syncHistory();
    }

    @PostMapping("/fetch-recent")
    public SsqFetchResult fetchRecent(@RequestParam(defaultValue = "10") int expect) throws IOException {
        return crawlerService.fetchAndSaveRecent(expect);
    }

    @GetMapping
    public List<DltRecord> list() {
        return crawlerService.findAll();
    }

    @GetMapping("/repeats")
    public List<RepeatStat> repeats(@RequestParam(defaultValue = "3") int matchCount) {
        return crawlerService.analyzeRepeats(matchCount);
    }

    @PostMapping("/recommendation/filter")
    public List<DltRecommendationFilterRequest.RecommendationGroup> filterRecommendation(
            @RequestBody DltRecommendationFilterRequest request
    ) {
        return recommendationFilterService.filter(request);
    }
}
