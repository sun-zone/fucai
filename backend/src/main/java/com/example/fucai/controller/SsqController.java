package com.example.fucai.controller;

import com.example.fucai.entity.SsqRecord;
import com.example.fucai.service.RepeatStat;
import com.example.fucai.service.SsqCrawlerService;
import com.example.fucai.service.SsqFetchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ssq")
public class SsqController {

    private final SsqCrawlerService crawlerService;

    public SsqController(SsqCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    @PostMapping("/fetch")
    public SsqFetchResult fetch(@RequestParam(defaultValue = "1000") int expect) throws IOException {
        return crawlerService.fetchAndSaveRecent(expect);
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
    public List<SsqRecord> list() {
        return crawlerService.findAll();
    }

    @GetMapping("/repeats")
    public List<RepeatStat> repeats(@RequestParam(defaultValue = "3") int matchCount) {
        return crawlerService.analyzeRepeats(matchCount);
    }
}
