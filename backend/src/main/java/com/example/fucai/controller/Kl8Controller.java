package com.example.fucai.controller;

import com.example.fucai.entity.Kl8Record;
import com.example.fucai.service.Kl8CrawlerService;
import com.example.fucai.service.Kl8RankHitGenerateResult;
import com.example.fucai.service.Kl8RankHitStatService;
import com.example.fucai.service.Kl8RankHitSummary;
import com.example.fucai.service.Kl8WeightOptimizationResult;
import com.example.fucai.service.Kl8WeightOptimizationService;
import com.example.fucai.service.Kl8WeightOptimizationStatus;
import com.example.fucai.service.RepeatStat;
import com.example.fucai.service.SsqFetchResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/kl8")
public class Kl8Controller {

    private final Kl8CrawlerService crawlerService;
    private final Kl8RankHitStatService rankHitStatService;
    private final Kl8WeightOptimizationService weightOptimizationService;

    public Kl8Controller(
            Kl8CrawlerService crawlerService,
            Kl8RankHitStatService rankHitStatService,
            Kl8WeightOptimizationService weightOptimizationService
    ) {
        this.crawlerService = crawlerService;
        this.rankHitStatService = rankHitStatService;
        this.weightOptimizationService = weightOptimizationService;
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
    public List<Kl8Record> list() {
        return crawlerService.findAll();
    }

    @GetMapping("/repeats")
    public List<RepeatStat> repeats(@RequestParam(defaultValue = "10") int matchCount) {
        return crawlerService.analyzeRepeats(matchCount);
    }

    @PostMapping("/rank-hit-stats/generate")
    public Kl8RankHitGenerateResult generateRankHitStats(@RequestParam(defaultValue = "3") int windowSize) {
        return rankHitStatService.generate(windowSize);
    }

    @GetMapping("/rank-hit-stats")
    public Kl8RankHitSummary rankHitStats(
            @RequestParam(defaultValue = "3") int windowSize,
            @RequestParam(required = false) String beforeExpect
    ) {
        return rankHitStatService.summary(windowSize, beforeExpect);
    }

    @PostMapping("/weight-optimization/run")
    public List<Kl8WeightOptimizationResult> runWeightOptimization() {
        return weightOptimizationService.optimizeAll();
    }

    @PostMapping("/weight-optimization/start")
    public Kl8WeightOptimizationStatus startWeightOptimization() {
        return weightOptimizationService.startAsync();
    }

    @GetMapping("/weight-optimization/status")
    public Kl8WeightOptimizationStatus weightOptimizationStatus() {
        return weightOptimizationService.getStatus();
    }

    @PostMapping("/weight-optimization/run-window")
    public Kl8WeightOptimizationResult runWeightOptimization(@RequestParam(defaultValue = "100") int windowSize) {
        return weightOptimizationService.optimize(windowSize);
    }

    @GetMapping("/weight-optimization/latest")
    public List<Kl8WeightOptimizationResult> latestWeightOptimization() {
        return weightOptimizationService.latestAll();
    }

    @GetMapping("/weight-optimization/latest-window")
    public Kl8WeightOptimizationResult latestWeightOptimization(@RequestParam(defaultValue = "100") int windowSize) {
        return weightOptimizationService.latest(windowSize);
    }
}
