package com.example.fucai.service;

import com.example.fucai.entity.Kl8FeatureWeight;
import com.example.fucai.entity.Kl8Record;
import com.example.fucai.entity.Kl8WeightRun;
import com.example.fucai.repository.Kl8FeatureWeightRepository;
import com.example.fucai.repository.Kl8RecordRepository;
import com.example.fucai.repository.Kl8WeightRunRepository;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class Kl8WeightOptimizationService {

    private static final int MAX_NUMBER = 80;
    private static final List<Integer> WINDOWS = Arrays.asList(3, 100, 200, 500);
    private static final List<String> FEATURES = Arrays.asList(
            "nextRate", "nextCount", "coRate", "coCount",
            "omissionAverage", "omissionMax", "runBelowMax", "runTwo", "runThree",
            "heat", "recentRepeat", "repeat", "neighbor", "consecutiveEdge",
            "recentHot", "recentCold", "hotTail", "hotZone"
    );

    private final Kl8RecordRepository recordRepository;
    private final Kl8WeightRunRepository runRepository;
    private final Kl8FeatureWeightRepository weightRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Object statusLock = new Object();
    private Kl8WeightOptimizationStatus status = new Kl8WeightOptimizationStatus();

    public Kl8WeightOptimizationService(
            Kl8RecordRepository recordRepository,
            Kl8WeightRunRepository runRepository,
            Kl8FeatureWeightRepository weightRepository
    ) {
        this.recordRepository = recordRepository;
        this.runRepository = runRepository;
        this.weightRepository = weightRepository;
    }

    @Scheduled(cron = "${fucai.kl8-weight-cron:0 30 3 * * ?}")
    public void scheduledOptimize() {
        startAsync();
    }

    public Kl8WeightOptimizationStatus startAsync() {
        synchronized (statusLock) {
            if ("RUNNING".equals(status.getStatus())) {
                return copyStatus(status);
            }
            status = new Kl8WeightOptimizationStatus();
            status.setStatus("RUNNING");
            status.setStartedAt(LocalDateTime.now());
            status.setFinishedAt(null);
            status.setErrorMessage(null);
            status.setMessage("后台调权已开始");
            status.setFinishedWindowCount(0);
            status.setTotalWindowCount(WINDOWS.size());
        }
        executorService.submit(() -> {
            try {
                optimizeAllWithStatus();
                synchronized (statusLock) {
                    status.setStatus("SUCCESS");
                    status.setCurrentWindow(null);
                    status.setFinishedAt(LocalDateTime.now());
                    status.setMessage("快乐8自动调权完成");
                }
            } catch (Exception ex) {
                synchronized (statusLock) {
                    status.setStatus("FAILED");
                    status.setFinishedAt(LocalDateTime.now());
                    status.setErrorMessage(ex.getMessage());
                    status.setMessage("快乐8自动调权失败");
                }
            }
        });
        return getStatus();
    }

    public Kl8WeightOptimizationStatus getStatus() {
        synchronized (statusLock) {
            return copyStatus(status);
        }
    }

    private void optimizeAllWithStatus() {
        int finished = 0;
        for (Integer window : WINDOWS) {
            synchronized (statusLock) {
                status.setCurrentWindow(window);
                status.setMessage("正在计算最近 " + window + " 期窗口权重");
            }
            optimize(window);
            finished++;
            synchronized (statusLock) {
                status.setFinishedWindowCount(finished);
                status.setMessage("已完成 " + finished + "/" + WINDOWS.size() + " 组窗口权重");
            }
        }
    }

    private Kl8WeightOptimizationStatus copyStatus(Kl8WeightOptimizationStatus source) {
        Kl8WeightOptimizationStatus copy = new Kl8WeightOptimizationStatus();
        copy.setStatus(source.getStatus());
        copy.setCurrentWindow(source.getCurrentWindow());
        copy.setFinishedWindowCount(source.getFinishedWindowCount());
        copy.setTotalWindowCount(source.getTotalWindowCount());
        copy.setMessage(source.getMessage());
        copy.setErrorMessage(source.getErrorMessage());
        copy.setStartedAt(source.getStartedAt());
        copy.setFinishedAt(source.getFinishedAt());
        return copy;
    }

    @Transactional
    public List<Kl8WeightOptimizationResult> optimizeAll() {
        List<Kl8WeightOptimizationResult> results = new ArrayList<>();
        for (Integer window : WINDOWS) {
            results.add(optimize(window));
        }
        return results;
    }

    @Transactional
    public Kl8WeightOptimizationResult optimize(int windowSize) {
        int window = Math.max(1, windowSize);
        List<Kl8Record> records = recordRepository.findAll(Sort.by(Sort.Direction.ASC, "expect"));
        int warmup = Math.max(20, Math.min(50, records.size() / 4));
        int start = Math.min(Math.max(1, warmup), records.size());
        StatsBundle bundle = new StatsBundle();
        StatsBundle rankedBundle = new StatsBundle();
        List<String> numbers = numberPool();
        int issueCount = 0;
        int hitCount = 0;

        for (int targetIndex = start; targetIndex < records.size(); targetIndex++) {
            Kl8Record base = records.get(targetIndex - 1);
            Kl8Record target = records.get(targetIndex);
            int historyStart = Math.max(0, targetIndex - window);
            List<Kl8Record> history = records.subList(historyStart, targetIndex);
            List<Kl8Record> allHistory = records.subList(0, targetIndex);
            Set<String> targetBalls = new HashSet<>(balls(target));
            issueCount++;
            hitCount += targetBalls.size();
            Map<String, Map<String, Double>> featureRows = new HashMap<>();
            for (String number : numbers) {
                Map<String, Double> row = features(number, base, history, allHistory);
                featureRows.put(number, row);
                bundle.add(row, targetBalls.contains(number));
            }
            if (window == 3) {
                List<String> ranked = rankWithBaseWeights(base, history, allHistory, numbers, baseWeights());
                for (String number : ranked) {
                    rankedBundle.add(featureRows.get(number), targetBalls.contains(number));
                }
            }
        }

        Kl8WeightRun run = new Kl8WeightRun();
        run.setWindowSize(window);
        run.setSampleIssueCount(issueCount);
        run.setSampleNumberCount(issueCount * MAX_NUMBER);
        run.setHitNumberCount(hitCount);
        run.setAverageHitCount(issueCount > 0 ? hitCount / (double) issueCount : 0);
        run.setStartExpect(issueCount > 0 ? records.get(start).getExpect() : null);
        run.setEndExpect(issueCount > 0 ? records.get(records.size() - 1).getExpect() : null);
        run.setCreatedAt(LocalDateTime.now());
        Kl8WeightRun savedRun = runRepository.save(run);

        List<Kl8FeatureWeight> weights = new ArrayList<>();
        for (String feature : FEATURES) {
            FeatureStats stats = bundle.stats(feature);
            double hitAverage = stats.hitCount > 0 ? stats.hitSum / stats.hitCount : 0;
            double missAverage = stats.missCount > 0 ? stats.missSum / stats.missCount : 0;
            double contribution = hitAverage - missAverage;
            if (window == 3) {
                FeatureStats rankedStats = rankedBundle.stats(feature);
                if (rankedStats.hitCount > 0 && rankedStats.missCount > 0) {
                    double rankedHitAverage = rankedStats.hitSum / rankedStats.hitCount;
                    double rankedMissAverage = rankedStats.missSum / rankedStats.missCount;
                    double rankedContribution = rankedHitAverage - rankedMissAverage;
                    contribution = contribution * 0.65 + rankedContribution * 0.35;
                }
            }
            Kl8FeatureWeight weight = new Kl8FeatureWeight();
            weight.setRun(savedRun);
            weight.setFeatureName(feature);
            weight.setHitAverage(round(hitAverage));
            weight.setMissAverage(round(missAverage));
            weight.setContribution(round(contribution));
            weight.setWeight(round(dynamicWeight(feature, contribution)));
            weights.add(weight);
        }
        weightRepository.saveAll(weights);
        return new Kl8WeightOptimizationResult(savedRun, weights);
    }

    private double featureScore(Map<String, Double> features) {
        return featureScore(features, baseWeights());
    }

    private double featureScore(Map<String, Double> features, Map<String, Double> weights) {
        double score = 0;
        for (String feature : FEATURES) {
            score += features.getOrDefault(feature, 0.0) * weights.getOrDefault(feature, baseWeight(feature));
        }
        return score;
    }

    private Map<String, Double> featureMaxima(Map<String, Map<String, Double>> rows) {
        Map<String, Double> maxima = new HashMap<>();
        String[] normalized = {
                "nextScore", "nextRate", "coScore", "coRate", "omissionScore",
                "omissionAverageBoost", "omissionMaxBoost", "runBelowMaxBoost",
                "maxConsecutiveScore", "heatScore", "recentRepeatScore", "repeatScore",
                "neighborScore", "zoneScore", "tailScore", "offsetScore", "multiWindowScore"
        };
        for (String feature : normalized) {
            maxima.put(feature, 0.0);
        }
        for (Map<String, Double> row : rows.values()) {
            for (String feature : normalized) {
                maxima.put(feature, Math.max(
                        maxima.get(feature),
                        row.getOrDefault(feature, 0.0)
                ));
            }
        }
        return maxima;
    }

    private double candidateScore(
            Map<String, Double> row,
            Map<String, Double> maxima,
            Map<String, Double> weights
    ) {
        double score = 0;
        score += normalize(row, maxima, "nextScore") * weights.getOrDefault("nextCount", 0.16);
        score += normalize(row, maxima, "nextRate") * weights.getOrDefault("nextRate", 0.22);
        score += normalize(row, maxima, "coScore") * weights.getOrDefault("coCount", 0.035);
        score += normalize(row, maxima, "coRate") * weights.getOrDefault("coRate", 0.045);
        score += normalize(row, maxima, "omissionScore") * 0.05;
        score += normalize(row, maxima, "omissionAverageBoost")
                * weights.getOrDefault("omissionAverage", 0.04);
        score += normalize(row, maxima, "omissionMaxBoost")
                * weights.getOrDefault("omissionMax", 0.025);
        score += normalize(row, maxima, "runBelowMaxBoost")
                * weights.getOrDefault("runBelowMax", 0.035);
        score += normalize(row, maxima, "maxConsecutiveScore") * 0.015;
        score += normalize(row, maxima, "heatScore") * weights.getOrDefault("heat", -0.035);
        score += normalize(row, maxima, "recentRepeatScore")
                * weights.getOrDefault("recentRepeat", -0.045);
        score += normalize(row, maxima, "repeatScore")
                * weights.getOrDefault("repeat", -0.035);
        score += row.getOrDefault("runTwo", 0.0) * weights.getOrDefault("runTwo", -0.12);
        score += row.getOrDefault("runThree", 0.0) * weights.getOrDefault("runThree", -0.35);
        score += row.getOrDefault("runOverMaxPenalty", 0.0) * -0.22;
        score += normalize(row, maxima, "neighborScore")
                * weights.getOrDefault("neighbor", 0.055);
        score += normalize(row, maxima, "zoneScore") * 0.025;
        score += normalize(row, maxima, "tailScore") * 0.025;
        score += normalize(row, maxima, "offsetScore") * 0.09;
        score += normalize(row, maxima, "multiWindowScore") * 0.12;
        score += row.getOrDefault("consecutiveEdgePenalty", 0.0)
                * weights.getOrDefault("consecutiveEdge", -0.12);
        score += row.getOrDefault("recentHotPenalty", 0.0)
                * weights.getOrDefault("recentHot", -0.07);
        score += row.getOrDefault("recentColdPenalty", 0.0)
                * weights.getOrDefault("recentCold", -0.05);
        score += row.getOrDefault("hotTailPenalty", 0.0)
                * weights.getOrDefault("hotTail", -0.055);
        score += row.getOrDefault("hotZonePenalty", 0.0)
                * weights.getOrDefault("hotZone", -0.06);
        return score;
    }

    private double normalize(Map<String, Double> row, Map<String, Double> maxima, String feature) {
        double max = maxima.getOrDefault(feature, 0.0);
        return max > 0 ? row.getOrDefault(feature, 0.0) / max * 100.0 : 0;
    }

    private List<Candidate> balanceRecommendation(
            List<Candidate> ranked,
            Kl8Record base,
            List<Kl8Record> history,
            List<Kl8Record> allHistory
    ) {
        List<Candidate> selected = new ArrayList<>(
                ranked.subList(0, Math.min(14, ranked.size()))
        );
        List<Candidate> pool = new ArrayList<>(
                ranked.subList(Math.min(14, ranked.size()), Math.min(45, ranked.size()))
        );
        Set<String> selectedSet = candidateNumbers(selected);
        while (selected.size() < 20 && !pool.isEmpty()) {
            Candidate best = null;
            double bestScore = -Double.MAX_VALUE;
            for (Candidate candidate : pool) {
                if (selectedSet.contains(candidate.number)) {
                    continue;
                }
                double score = candidate.score
                        + fitBonus(candidate, selected)
                        + candidate.features.getOrDefault("multiWindowScore", 0.0) * 0.08;
                if (best == null || score > bestScore
                        || (score == bestScore && candidate.rank < best.rank)) {
                    best = candidate;
                    bestScore = score;
                }
            }
            if (best == null) {
                break;
            }
            selected.add(best);
            selectedSet.add(best.number);
            pool.remove(best);
        }

        List<Candidate> backupPool = new ArrayList<>();
        for (Candidate candidate : ranked) {
            if (!selectedSet.contains(candidate.number)) {
                backupPool.add(candidate);
            }
        }
        for (int round = 0; round < 80; round++) {
            RecommendationIssue issue = recommendationIssue(selected, base, allHistory);
            if (issue == null) {
                break;
            }
            int removeIndex = removalIndex(selected, issue);
            int additionIndex = additionIndex(backupPool, issue, selectedSet);
            if (removeIndex < 0 || additionIndex < 0) {
                break;
            }
            Candidate removed = selected.remove(removeIndex);
            Candidate added = backupPool.remove(additionIndex);
            selectedSet.remove(removed.number);
            selectedSet.add(added.number);
            backupPool.add(removed);
            Collections.sort(backupPool, (left, right) -> Integer.compare(left.rank, right.rank));
        }
        return selected;
    }

    private List<Candidate> balanceAvoid(List<Candidate> remaining) {
        List<Candidate> risky = new ArrayList<>();
        for (Candidate candidate : remaining) {
            if (positiveSignalScore(candidate) < 55) {
                risky.add(candidate);
            }
        }
        Collections.sort(risky, (left, right) -> {
            if (left.score != right.score) {
                return Double.compare(left.score, right.score);
            }
            return Integer.compare(right.rank, left.rank);
        });
        List<Candidate> fallback = reverseCopy(remaining);
        List<Candidate> result = new ArrayList<>();
        addUnique(result, risky, 10);
        addUnique(result, fallback, 10);
        return result;
    }

    private void addUnique(List<Candidate> target, List<Candidate> source, int maxSize) {
        Set<String> used = candidateNumbers(target);
        for (Candidate candidate : source) {
            if (target.size() >= maxSize) {
                break;
            }
            if (used.add(candidate.number)) {
                target.add(candidate);
            }
        }
    }

    private Set<String> candidateNumbers(List<Candidate> candidates) {
        Set<String> numbers = new HashSet<>();
        for (Candidate candidate : candidates) {
            numbers.add(candidate.number);
        }
        return numbers;
    }

    private List<Candidate> reverseCopy(List<Candidate> source) {
        List<Candidate> copy = new ArrayList<>(source);
        Collections.reverse(copy);
        return copy;
    }

    private List<String> toNumbers(List<Candidate> source, int size, List<Candidate> fallback) {
        List<String> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        addNumbers(result, used, source, size);
        addNumbers(result, used, fallback, size);
        return result;
    }

    private void addNumbers(
            List<String> target,
            Set<String> used,
            List<Candidate> source,
            int size
    ) {
        for (Candidate candidate : source) {
            if (target.size() >= size) {
                break;
            }
            if (used.add(candidate.number)) {
                target.add(candidate.number);
            }
        }
    }

    private int count(List<Candidate> candidates, CandidatePredicate predicate) {
        int total = 0;
        for (Candidate candidate : candidates) {
            if (predicate.test(candidate)) {
                total++;
            }
        }
        return total;
    }

    private <T> Map<T, Integer> countBy(List<Candidate> candidates, CandidateValue<T> value) {
        Map<T, Integer> result = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            T key = value.get(candidate);
            result.put(key, result.getOrDefault(key, 0) + 1);
        }
        return result;
    }

    private double positiveSignalScore(Candidate candidate) {
        Map<String, Double> row = candidate.features;
        return row.getOrDefault("nextRate", 0.0) * 100
                + row.getOrDefault("multiWindowScore", 0.0) * 0.55
                + row.getOrDefault("offsetScore", 0.0) * 8
                + row.getOrDefault("neighborScore", 0.0) * 16
                + row.getOrDefault("repeatScore", 0.0) * 8
                + row.getOrDefault("omissionAverageBoost", 0.0) * 10
                + row.getOrDefault("omissionMaxBoost", 0.0) * 8;
    }

    private double fitBonus(Candidate candidate, List<Candidate> selected) {
        List<Candidate> next = new ArrayList<>(selected);
        next.add(candidate);
        int zone = zoneIndex(candidate.number);
        int tail = numberValue(candidate.number) % 10;
        int zoneCount = count(next, value -> zoneIndex(value.number) == zone);
        int tailCount = count(next, value -> numberValue(value.number) % 10 == tail);
        int oddCount = count(next, value -> numberValue(value.number) % 2 == 1);
        int bigCount = count(next, value -> numberValue(value.number) > 40);
        double zoneBonus = zoneCount <= 3 ? 14 : -18.0 * (zoneCount - 3);
        double tailBonus = tailCount <= 3 ? 8 : -12.0 * (tailCount - 3);
        double oddBonus = Math.abs(oddCount - 10) <= 2 ? 7 : -6;
        double bigBonus = Math.abs(bigCount - 10) <= 2 ? 7 : -6;
        return zoneBonus + tailBonus + oddBonus + bigBonus;
    }

    private RecommendationIssue recommendationIssue(
            List<Candidate> selected,
            Kl8Record base,
            List<Kl8Record> allHistory
    ) {
        Map<Integer, Integer> zoneCounts = countBy(selected, value -> zoneIndex(value.number));
        Set<Integer> hotZones = hotZones(balls(base));
        for (Map.Entry<Integer, Integer> entry : zoneCounts.entrySet()) {
            int limit = hotZones.contains(entry.getKey()) ? 2 : 3;
            if (entry.getValue() > limit) {
                return new RecommendationIssue("zone", entry.getKey());
            }
        }
        Map<Integer, Integer> tailCounts = countBy(selected, value -> numberValue(value.number) % 10);
        for (Map.Entry<Integer, Integer> entry : tailCounts.entrySet()) {
            if (entry.getValue() > 3) {
                return new RecommendationIssue("tail", entry.getKey());
            }
        }
        int oddCount = count(selected, value -> numberValue(value.number) % 2 == 1);
        if (oddCount > 12) {
            return new RecommendationIssue("parity", "odd");
        }
        if (oddCount < 8) {
            return new RecommendationIssue("parity", "even");
        }
        int bigCount = count(selected, value -> numberValue(value.number) > 40);
        if (bigCount > 12) {
            return new RecommendationIssue("size", "big");
        }
        if (bigCount < 8) {
            return new RecommendationIssue("size", "small");
        }
        double averageSum = recentAverageSum(allHistory);
        if (averageSum > 0) {
            int sum = 0;
            for (Candidate candidate : selected) {
                sum += numberValue(candidate.number);
            }
            if (sum - averageSum > 30) {
                return new RecommendationIssue("sum", "high");
            }
            if (averageSum - sum > 30) {
                return new RecommendationIssue("sum", "low");
            }
        }
        return null;
    }

    private int removalIndex(List<Candidate> selected, RecommendationIssue issue) {
        List<Candidate> candidates = new ArrayList<>();
        for (Candidate candidate : selected) {
            if (matchesIssue(candidate, issue)) {
                candidates.add(candidate);
            }
        }
        Candidate target = lowestCandidate(candidates);
        return target == null ? -1 : selected.indexOf(target);
    }

    private int additionIndex(List<Candidate> pool, RecommendationIssue issue, Set<String> selectedSet) {
        Candidate target = null;
        for (Candidate candidate : pool) {
            if (selectedSet.contains(candidate.number) || violatesIssue(candidate, issue)) {
                continue;
            }
            if (target == null || candidate.rank < target.rank) {
                target = candidate;
            }
        }
        return target == null ? -1 : pool.indexOf(target);
    }

    private boolean matchesIssue(Candidate candidate, RecommendationIssue issue) {
        if ("zone".equals(issue.type)) {
            return zoneIndex(candidate.number) == (Integer) issue.value;
        }
        if ("tail".equals(issue.type)) {
            return numberValue(candidate.number) % 10 == (Integer) issue.value;
        }
        if ("parity".equals(issue.type)) {
            return (numberValue(candidate.number) % 2 == 1 ? "odd" : "even").equals(issue.value);
        }
        if ("size".equals(issue.type)) {
            return (numberValue(candidate.number) > 40 ? "big" : "small").equals(issue.value);
        }
        if ("sum".equals(issue.type)) {
            return "high".equals(issue.value) ? numberValue(candidate.number) > 40
                    : numberValue(candidate.number) <= 40;
        }
        return false;
    }

    private boolean violatesIssue(Candidate candidate, RecommendationIssue issue) {
        return matchesIssue(candidate, issue);
    }

    private Candidate lowestCandidate(List<Candidate> candidates) {
        Candidate target = null;
        for (Candidate candidate : candidates) {
            if (target == null || candidate.score < target.score
                    || (candidate.score == target.score && candidate.rank > target.rank)) {
                target = candidate;
            }
        }
        return target;
    }

    private double recentAverageSum(List<Kl8Record> records) {
        int start = Math.max(0, records.size() - 5);
        if (start >= records.size()) {
            return 0;
        }
        double total = 0;
        int count = 0;
        for (int index = start; index < records.size(); index++) {
            for (String number : balls(records.get(index))) {
                total += numberValue(number);
            }
            count++;
        }
        return count == 0 ? 0 : total / count;
    }

    private double zoneTarget(String number, List<Kl8Record> history) {
        double[] averages = new double[8];
        int pairCount = 0;
        for (int index = 0; index + 1 < history.size(); index++) {
            pairCount++;
            for (String next : balls(history.get(index + 1))) {
                averages[zoneIndex(next)]++;
            }
        }
        if (pairCount == 0) {
            return 0;
        }
        for (int index = 0; index < averages.length; index++) {
            averages[index] /= pairCount;
        }
        int[] targets = distributeTargets(averages, 20);
        return targets[zoneIndex(number)];
    }

    private double tailTarget(String number, List<Kl8Record> history) {
        double[] averages = new double[10];
        int pairCount = 0;
        for (int index = 0; index + 1 < history.size(); index++) {
            pairCount++;
            for (String next : balls(history.get(index + 1))) {
                averages[numberValue(next) % 10]++;
            }
        }
        if (pairCount == 0) {
            return 0;
        }
        for (int index = 0; index < averages.length; index++) {
            averages[index] /= pairCount;
        }
        int[] targets = distributeTargets(averages, 20);
        return targets[numberValue(number) % 10];
    }

    private int[] distributeTargets(double[] averages, int total) {
        int[] targets = new int[averages.length];
        int assigned = 0;
        for (int index = 0; index < averages.length; index++) {
            targets[index] = Math.max(0, (int) Math.floor(averages[index]));
            assigned += targets[index];
        }
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < averages.length; index++) {
            order.add(index);
        }
        Collections.sort(order, (left, right) -> {
            double leftRest = averages[left] - Math.floor(averages[left]);
            double rightRest = averages[right] - Math.floor(averages[right]);
            if (leftRest != rightRest) {
                return Double.compare(rightRest, leftRest);
            }
            return Integer.compare(left, right);
        });
        int pointer = 0;
        while (assigned < total && !order.isEmpty()) {
            targets[order.get(pointer % order.size())]++;
            assigned++;
            pointer++;
        }
        while (assigned > total) {
            int index = 0;
            for (int current = 1; current < targets.length; current++) {
                if (targets[current] > targets[index]) {
                    index = current;
                }
            }
            if (targets[index] == 0) {
                break;
            }
            targets[index]--;
            assigned--;
        }
        return targets;
    }

    private double offsetScore(String number, List<Kl8Record> history, List<String> currentNumbers) {
        if (history.size() < 3) {
            return 0;
        }
        double score = 0;
        int start = Math.max(0, history.size() - 10);
        for (int index = start; index + 1 < history.size(); index++) {
            List<Kl8Record> training = history.subList(start, index);
            if (training.isEmpty()) {
                continue;
            }
            List<String> predicted = baselineNumbers(training, balls(history.get(index)), 20);
            for (String actual : balls(history.get(index + 1))) {
                int distance = Integer.MAX_VALUE;
                for (String predictedNumber : predicted) {
                    distance = Math.min(distance,
                            Math.abs(numberValue(actual) - numberValue(predictedNumber)));
                }
                if (actual.equals(number)) {
                    if (distance == 0) {
                        score += 1.2;
                    } else if (distance <= 3) {
                        score += 1.0 / distance;
                    }
                }
            }
        }
        return score;
    }

    private List<String> baselineNumbers(
            List<Kl8Record> records,
            List<String> currentNumbers,
            int size
    ) {
        List<Candidate> candidates = new ArrayList<>();
        for (String number : numberPool()) {
            int nextScore = 0;
            int coScore = 0;
            int heatScore = 0;
            for (int index = 0; index < records.size(); index++) {
                List<String> current = balls(records.get(index));
                if (current.contains(number)) {
                    heatScore++;
                }
                for (String currentNumber : currentNumbers) {
                    if (current.contains(currentNumber) && current.contains(number)
                            && !currentNumber.equals(number)) {
                        coScore++;
                    }
                }
                if (index + 1 < records.size()) {
                    List<String> next = balls(records.get(index + 1));
                    for (String currentNumber : currentNumbers) {
                        if (current.contains(currentNumber) && next.contains(number)) {
                            nextScore++;
                        }
                    }
                }
            }
            Map<String, Double> row = new HashMap<>();
            row.put("simple", nextScore * 0.55 + coScore * 0.18 + heatScore * 0.12);
            candidates.add(new Candidate(number, row, row.get("simple")));
        }
        Collections.sort(candidates, (left, right) -> {
            if (left.score != right.score) {
                return Double.compare(right.score, left.score);
            }
            return Integer.compare(numberValue(left.number), numberValue(right.number));
        });
        return toNumbers(candidates, size, candidates);
    }

    private double multiWindowScore(
            String number,
            List<Kl8Record> allHistory,
            List<String> currentNumbers
    ) {
        int[] sizes = {3, 5, 10, 20, 50};
        double[] weights = {1.25, 1.15, 1.35, 1.0, 0.75};
        double weighted = 0;
        double totalWeight = 0;
        for (int index = 0; index < sizes.length; index++) {
            int start = Math.max(0, allHistory.size() - sizes[index]);
            List<Kl8Record> history = allHistory.subList(start, allHistory.size());
            if (history.size() < 2) {
                continue;
            }
            weighted += windowSignalScore(number, history, currentNumbers) * weights[index];
            totalWeight += weights[index];
        }
        return totalWeight > 0 ? weighted / totalWeight : 0;
    }

    private double windowSignalScore(
            String number,
            List<Kl8Record> history,
            List<String> currentNumbers
    ) {
        int nextHit = 0;
        int nextBase = 0;
        int coHit = 0;
        int coBase = 0;
        int heat = 0;
        for (int index = 0; index < history.size(); index++) {
            List<String> current = balls(history.get(index));
            if (current.contains(number)) {
                heat++;
            }
            for (String selected : currentNumbers) {
                if (!current.contains(selected)) {
                    continue;
                }
                coBase++;
                if (!selected.equals(number) && current.contains(number)) {
                    coHit++;
                }
            }
            if (index + 1 < history.size()) {
                List<String> next = balls(history.get(index + 1));
                for (String selected : currentNumbers) {
                    if (current.contains(selected)) {
                        nextBase++;
                        if (next.contains(number)) {
                            nextHit++;
                        }
                    }
                }
            }
        }
        double expectedRate = currentNumbers.size() / (double) MAX_NUMBER;
        double nextRate = nextBase > 0
                ? (nextHit + expectedRate * 4) / (nextBase + 4.0) : 0;
        double coRate = coBase > 0
                ? (coHit + expectedRate * 4) / (coBase + 4.0) : 0;
        double heatRate = heat / (double) history.size();
        return nextRate * 58 + coRate * 18 + heatRate * 10;
    }

    private void updateOnlineWeights(
            Map<String, Double> weights,
            Map<String, Map<String, Double>> rows,
            Set<String> actualSet
    ) {
        Map<String, Double> hitSum = new HashMap<>();
        Map<String, Double> missSum = new HashMap<>();
        int hitCount = 0;
        int missCount = 0;
        for (Map.Entry<String, Map<String, Double>> entry : rows.entrySet()) {
            boolean hit = actualSet.contains(entry.getKey());
            if (hit) {
                hitCount++;
            } else {
                missCount++;
            }
            for (String feature : FEATURES) {
                double value = entry.getValue().getOrDefault(feature, 0.0);
                Map<String, Double> target = hit ? hitSum : missSum;
                target.put(feature, target.getOrDefault(feature, 0.0) + value);
            }
        }
        if (hitCount == 0 || missCount == 0) {
            return;
        }
        for (String feature : FEATURES) {
            double contribution = hitSum.getOrDefault(feature, 0.0) / hitCount
                    - missSum.getOrDefault(feature, 0.0) / missCount;
            double targetWeight = dynamicWeight(feature, contribution);
            double current = weights.getOrDefault(feature, baseWeight(feature));
            double next = current + (targetWeight - current) * 0.20;
            weights.put(feature, clampToBaseBand(feature, next));
        }
    }

    private double clampToBaseBand(String feature, double value) {
        double base = baseWeight(feature);
        if (base == 0) {
            return 0;
        }
        double lower = Math.min(base * 0.75, base * 1.25);
        double upper = Math.max(base * 0.75, base * 1.25);
        return Math.max(lower, Math.min(upper, value));
    }

    private List<String> rankWithBaseWeights(
            Kl8Record base,
            List<Kl8Record> history,
            List<Kl8Record> allHistory,
            List<String> numbers,
            Map<String, Double> weights
    ) {
        Map<String, Map<String, Double>> featureRows = buildFeatureRows(base, history, allHistory, numbers);
        return buildRecommendationTiers(featureRows, base, history, allHistory, numbers, weights).recommend;
    }

    private Map<String, Map<String, Double>> buildFeatureRows(
            Kl8Record base,
            List<Kl8Record> history,
            List<Kl8Record> allHistory,
            List<String> numbers
    ) {
        Map<String, Map<String, Double>> featureRows = new HashMap<>();
        for (String number : numbers) {
            featureRows.put(number, features(number, base, history, allHistory));
        }
        return featureRows;
    }

    private RecommendationTiers buildRecommendationTiers(
            Map<String, Map<String, Double>> featureRows,
            Kl8Record base,
            List<Kl8Record> history,
            List<Kl8Record> allHistory,
            List<String> numbers,
            Map<String, Double> weights
    ) {
        Map<String, Double> maxima = featureMaxima(featureRows);
        List<Candidate> rankedCandidates = new ArrayList<>();
        for (String number : numbers) {
            Map<String, Double> row = featureRows.get(number);
            rankedCandidates.add(new Candidate(number, row, candidateScore(row, maxima, weights)));
        }
        rankedCandidates.sort((left, right) -> {
            if (right.score != left.score) {
                return Double.compare(right.score, left.score);
            }
            double rightNext = right.features.getOrDefault("nextRate", 0.0);
            double leftNext = left.features.getOrDefault("nextRate", 0.0);
            if (rightNext != leftNext) {
                return Double.compare(rightNext, leftNext);
            }
            return Integer.compare(numberValue(left.number), numberValue(right.number));
        });
        for (int index = 0; index < rankedCandidates.size(); index++) {
            rankedCandidates.get(index).rank = index + 1;
        }

        List<Candidate> recommend = balanceRecommendation(rankedCandidates, base, history, allHistory);
        Set<String> recommendSet = candidateNumbers(recommend);
        List<Candidate> remaining = new ArrayList<>();
        for (Candidate candidate : rankedCandidates) {
            if (!recommendSet.contains(candidate.number)) {
                remaining.add(candidate);
            }
        }
        List<Candidate> avoid = balanceAvoid(remaining);
        Set<String> avoidSet = candidateNumbers(avoid);
        List<Candidate> middle = new ArrayList<>();
        for (Candidate candidate : remaining) {
            if (!avoidSet.contains(candidate.number)) {
                middle.add(candidate);
            }
        }
        return new RecommendationTiers(
                toNumbers(recommend, 20, rankedCandidates),
                toNumbers(middle, 50, remaining),
                toNumbers(avoid, 10, reverseCopy(remaining))
        );
    }

    public List<Kl8WeightOptimizationResult> latestAll() {
        List<Kl8WeightOptimizationResult> results = new ArrayList<>();
        for (Integer window : WINDOWS) {
            Kl8WeightOptimizationResult result = latest(window);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    public Kl8WeightOptimizationResult latest(int windowSize) {
        Optional<Kl8WeightRun> run = runRepository.findTopByWindowSizeOrderByCreatedAtDesc(windowSize);
        if (!run.isPresent()) {
            return null;
        }
        return new Kl8WeightOptimizationResult(
                run.get(),
                weightRepository.findByRunIdOrderByContributionDesc(run.get().getId())
        );
    }

    private Map<String, Double> features(String number, Kl8Record base, List<Kl8Record> history) {
        return features(number, base, history, history);
    }

    private Map<String, Double> features(
            String number,
            Kl8Record base,
            List<Kl8Record> history,
            List<Kl8Record> allHistory
    ) {
        List<String> currentNumbers = balls(base);
        Set<String> neighborNumbers = new HashSet<>(neighborNumbers(currentNumbers));
        List<Kl8Record> recent10 = allHistory.subList(Math.max(0, allHistory.size() - 10), allHistory.size());
        Omission omission = omission(number, allHistory);
        int runLength = currentRun(number, allHistory);
        int nextCount = nextCount(number, currentNumbers, history);
        int nextBase = nextBase(currentNumbers, history);
        int coCount = coCount(number, currentNumbers, history);
        int coBase = coBase(currentNumbers, history);
        int heat = appearCount(number, history);
        int recentRepeat = appearCount(number, history.subList(Math.max(0, history.size() - 5), history.size()));
        int recent10Count = appearCount(number, recent10);
        double expectedRate = currentNumbers.size() / (double) MAX_NUMBER;

        Map<String, Double> map = new LinkedHashMap<>();
        map.put("nextRate", nextBase > 0 ? (nextCount + expectedRate * 8) / (nextBase + 8) : 0);
        map.put("nextScore", (double) nextCount);
        map.put("nextCount", (double) nextCount);
        map.put("coRate", coBase > 0 ? (coCount + expectedRate * 8) / (coBase + 8) : 0);
        map.put("coCount", (double) coCount);
        map.put("omissionAverage", omission.average > 0 && omission.current > omission.average
                ? omission.current / omission.average : 0);
        map.put("omissionMax", omission.max > 0 && omission.current > omission.max
                ? omission.current / (double) omission.max : 0);
        map.put("runBelowMax", omission.maxConsecutive > 0 && runLength > 0
                && runLength < omission.maxConsecutive
                ? (omission.maxConsecutive - runLength) / (double) omission.maxConsecutive : 0);
        map.put("runTwo", runLength == 2 ? 1.0 : 0);
        map.put("runThree", runLength >= 3 ? 1.0 : 0);
        map.put("heat", history.isEmpty() ? 0 : heat / (double) history.size());
        map.put("recentRepeat", history.isEmpty() ? 0
                : recentRepeat / (double) Math.max(1, history.size()));
        map.put("repeat", currentNumbers.contains(number) ? 1.0 : 0);
        map.put("neighbor", neighborNumbers.contains(number) ? 1.0 : 0);
        map.put("consecutiveEdge", consecutiveEdgeNumbers(currentNumbers).contains(number) ? 1.0 : 0);
        map.put("recentHot", recent10Count >= 4 ? 1.0 : 0);
        map.put("recentCold", recent10Count < 2 && omission.average > 0
                && omission.current <= omission.average ? 1.0 : 0);
        map.put("hotTail", hotTails(currentNumbers).contains(numberValue(number) % 10) ? 1.0 : 0);
        map.put("hotZone", hotZones(currentNumbers).contains(zoneIndex(number)) ? 1.0 : 0);

        map.put("omissionScore", omission.max > 0
                ? Math.min(omission.current, omission.max) / (double) omission.max : 0);
        map.put("omissionAverageBoost", omission.average > 0 && omission.current > omission.average
                ? omission.current / omission.average : 0);
        map.put("omissionMaxBoost", omission.max > 0 && omission.current > omission.max
                ? omission.current / (double) omission.max : 0);
        map.put("runBelowMaxBoost", omission.maxConsecutive > 0 && runLength > 0
                && runLength < omission.maxConsecutive
                ? (omission.maxConsecutive - runLength) / (double) omission.maxConsecutive : 0);
        map.put("maxConsecutiveScore", (double) omission.maxConsecutive);
        map.put("heatScore", (double) heat);
        map.put("recentRepeatScore", (double) recentRepeat);
        map.put("repeatScore", currentNumbers.contains(number) ? 1.0 : 0);
        map.put("neighborScore", neighborNumbers.contains(number) ? 1.0 : 0);
        map.put("zoneScore", zoneTarget(number, history));
        map.put("tailScore", tailTarget(number, history));
        map.put("offsetScore", offsetScore(number, history, currentNumbers));
        map.put("multiWindowScore", multiWindowScore(number, allHistory, currentNumbers));
        map.put("runOverMaxPenalty", omission.maxConsecutive > 0 && runLength > omission.maxConsecutive ? 1.0 : 0);
        map.put("consecutiveEdgePenalty", consecutiveEdgeNumbers(currentNumbers).contains(number) ? 100.0 : 0);
        map.put("recentHotPenalty", recent10Count >= 4 ? 100.0 : 0);
        map.put("recentColdPenalty", recent10Count < 2 && omission.average > 0
                && omission.current <= omission.average ? 100.0 : 0);
        map.put("hotTailPenalty", hotTails(currentNumbers).contains(numberValue(number) % 10) ? 100.0 : 0);
        map.put("hotZonePenalty", hotZones(currentNumbers).contains(zoneIndex(number)) ? 100.0 : 0);
        return map;
    }

    private double dynamicWeight(String feature, double contribution) {
        double base = baseWeight(feature);
        if (base == 0) {
            return 0;
        }
        double strength = Math.min(1.0, Math.abs(contribution) / 25.0);
        double direction = contribution >= 0 ? 1 : -1;
        double baseDirection = base >= 0 ? 1 : -1;
        double multiplier = direction == baseDirection
                ? 1.0 + 0.25 * strength
                : 1.0 - 0.25 * strength;
        return round(base * multiplier);
    }

    private Map<String, Double> baseWeights() {
        Map<String, Double> weights = new HashMap<>();
        for (String feature : FEATURES) {
            weights.put(feature, baseWeight(feature));
        }
        return weights;
    }

    private double baseWeight(String feature) {
        if ("nextRate".equals(feature)) return 0.22;
        if ("nextCount".equals(feature)) return 0.16;
        if ("coRate".equals(feature)) return 0.04;
        if ("coCount".equals(feature)) return 0.03;
        if ("omissionAverage".equals(feature)) return 0.04;
        if ("omissionMax".equals(feature)) return 0.02;
        if ("runBelowMax".equals(feature)) return 0.03;
        if ("runTwo".equals(feature)) return -0.12;
        if ("runThree".equals(feature)) return -0.35;
        if ("heat".equals(feature)) return -0.03;
        if ("recentRepeat".equals(feature)) return -0.04;
        if ("repeat".equals(feature)) return -0.03;
        if ("neighbor".equals(feature)) return 0.05;
        if ("consecutiveEdge".equals(feature)) return -0.10;
        if ("recentHot".equals(feature)) return -0.07;
        if ("recentCold".equals(feature)) return -0.05;
        if ("hotTail".equals(feature)) return -0.05;
        if ("hotZone".equals(feature)) return -0.06;
        return 0;
    }

    private int nextCount(String number, List<String> currentNumbers, List<Kl8Record> history) {
        int count = 0;
        for (int i = 0; i < history.size() - 1; i++) {
            List<String> current = balls(history.get(i));
            List<String> next = balls(history.get(i + 1));
            for (String currentNumber : currentNumbers) {
                if (current.contains(currentNumber) && next.contains(number)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int nextBase(List<String> currentNumbers, List<Kl8Record> history) {
        int count = 0;
        for (int i = 0; i < history.size() - 1; i++) {
            List<String> current = balls(history.get(i));
            for (String currentNumber : currentNumbers) {
                if (current.contains(currentNumber)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int coCount(String number, List<String> currentNumbers, List<Kl8Record> history) {
        int count = 0;
        for (Kl8Record record : history) {
            List<String> balls = balls(record);
            for (String currentNumber : currentNumbers) {
                if (!number.equals(currentNumber) && balls.contains(currentNumber) && balls.contains(number)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int coBase(List<String> currentNumbers, List<Kl8Record> history) {
        int count = 0;
        for (Kl8Record record : history) {
            List<String> balls = balls(record);
            for (String currentNumber : currentNumbers) {
                if (balls.contains(currentNumber)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int appearCount(String number, List<Kl8Record> records) {
        int count = 0;
        for (Kl8Record record : records) {
            if (balls(record).contains(number)) {
                count++;
            }
        }
        return count;
    }

    private int currentRun(String number, List<Kl8Record> records) {
        int run = 0;
        for (int index = records.size() - 1; index >= 0; index--) {
            if (!balls(records.get(index)).contains(number)) {
                break;
            }
            run++;
        }
        return run;
    }

    private Omission omission(String number, List<Kl8Record> records) {
        int current = 0;
        for (int index = records.size() - 1; index >= 0; index--) {
            if (balls(records.get(index)).contains(number)) {
                break;
            }
            current++;
        }
        int miss = 0;
        int consecutive = 0;
        int maxConsecutive = 0;
        List<Integer> misses = new ArrayList<>();
        for (Kl8Record record : records) {
            if (balls(record).contains(number)) {
                misses.add(miss);
                miss = 0;
                consecutive++;
                maxConsecutive = Math.max(maxConsecutive, consecutive);
            } else {
                miss++;
                consecutive = 0;
            }
        }
        misses.add(miss);
        int max = 0;
        int total = 0;
        for (Integer value : misses) {
            max = Math.max(max, value);
            total += value;
        }
        Omission omission = new Omission();
        omission.current = current;
        omission.max = max;
        omission.average = misses.isEmpty() ? 0 : total / (double) misses.size();
        omission.maxConsecutive = maxConsecutive;
        return omission;
    }

    private List<String> neighborNumbers(List<String> numbers) {
        Set<String> result = new HashSet<>();
        for (String number : numbers) {
            int value = numberValue(number);
            if (value > 1) {
                result.add(normalize(value - 1));
            }
            if (value < MAX_NUMBER) {
                result.add(normalize(value + 1));
            }
        }
        result.removeAll(numbers);
        return new ArrayList<>(result);
    }

    private Set<String> consecutiveEdgeNumbers(List<String> numbers) {
        Set<String> result = new HashSet<>();
        List<Integer> values = new ArrayList<>();
        for (String number : numbers) {
            values.add(numberValue(number));
        }
        values.sort(Integer::compareTo);
        List<Integer> group = new ArrayList<>();
        for (Integer value : values) {
            if (group.isEmpty() || value == group.get(group.size() - 1) + 1) {
                group.add(value);
            } else {
                addGroupEdges(result, group);
                group.clear();
                group.add(value);
            }
        }
        addGroupEdges(result, group);
        result.removeAll(numbers);
        return result;
    }

    private void addGroupEdges(Set<String> result, List<Integer> group) {
        if (group.size() < 2) {
            return;
        }
        int left = group.get(0) - 1;
        int right = group.get(group.size() - 1) + 1;
        if (left >= 1) {
            result.add(normalize(left));
        }
        if (right <= MAX_NUMBER) {
            result.add(normalize(right));
        }
    }

    private Set<Integer> hotTails(List<String> numbers) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (String number : numbers) {
            int tail = numberValue(number) % 10;
            counts.put(tail, counts.getOrDefault(tail, 0) + 1);
        }
        return topKeys(counts, 2, 2);
    }

    private Set<Integer> hotZones(List<String> numbers) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (String number : numbers) {
            int zone = zoneIndex(number);
            counts.put(zone, counts.getOrDefault(zone, 0) + 1);
        }
        return topKeys(counts, 1, 3);
    }

    private Set<Integer> topKeys(Map<Integer, Integer> counts, int limit, int minCount) {
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((left, right) -> {
            int countCompare = right.getValue().compareTo(left.getValue());
            return countCompare != 0 ? countCompare : left.getKey().compareTo(right.getKey());
        });
        Set<Integer> result = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : entries) {
            if (result.size() >= limit || entry.getValue() < minCount) {
                break;
            }
            result.add(entry.getKey());
        }
        return result;
    }

    private List<String> numberPool() {
        List<String> numbers = new ArrayList<>();
        for (int i = 1; i <= MAX_NUMBER; i++) {
            numbers.add(normalize(i));
        }
        return numbers;
    }

    private List<String> balls(Kl8Record record) {
        return Arrays.asList(
                record.getNum1(), record.getNum2(), record.getNum3(), record.getNum4(), record.getNum5(),
                record.getNum6(), record.getNum7(), record.getNum8(), record.getNum9(), record.getNum10(),
                record.getNum11(), record.getNum12(), record.getNum13(), record.getNum14(), record.getNum15(),
                record.getNum16(), record.getNum17(), record.getNum18(), record.getNum19(), record.getNum20()
        );
    }

    private int zoneIndex(String number) {
        return (numberValue(number) - 1) / 10;
    }

    private int numberValue(String number) {
        return Integer.parseInt(number);
    }

    private String normalize(int number) {
        return String.format("%02d", number);
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private static class FeatureStats {
        private double hitSum;
        private int hitCount;
        private double missSum;
        private int missCount;
    }

    private static class StatsBundle {
        private final Map<String, FeatureStats> stats = new HashMap<>();

        private void add(Map<String, Double> features, boolean hit) {
            for (String feature : FEATURES) {
                FeatureStats stat = stats(feature);
                double value = features.getOrDefault(feature, 0.0);
                if (hit) {
                    stat.hitSum += value;
                    stat.hitCount++;
                } else {
                    stat.missSum += value;
                    stat.missCount++;
                }
            }
        }

        private FeatureStats stats(String feature) {
            FeatureStats stat = stats.get(feature);
            if (stat == null) {
                stat = new FeatureStats();
                stats.put(feature, stat);
            }
            return stat;
        }
    }

    private static class Omission {
        private int current;
        private int max;
        private double average;
        private int maxConsecutive;
    }

    private interface CandidatePredicate {
        boolean test(Candidate candidate);
    }

    private interface CandidateValue<T> {
        T get(Candidate candidate);
    }

    private static class Candidate {
        private final String number;
        private final Map<String, Double> features;
        private final double score;
        private int rank;

        private Candidate(String number, Map<String, Double> features, double score) {
            this.number = number;
            this.features = features;
            this.score = score;
        }
    }

    private static class RecommendationIssue {
        private final String type;
        private final Object value;

        private RecommendationIssue(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private static class RecommendationTiers {
        private final List<String> recommend;
        private final List<String> middle;
        private final List<String> avoid;

        private RecommendationTiers(List<String> recommend, List<String> middle, List<String> avoid) {
            this.recommend = recommend;
            this.middle = middle;
            this.avoid = avoid;
        }
    }
}
