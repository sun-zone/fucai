package com.example.fucai.service;

import com.example.fucai.entity.Kl8RankHitStat;
import com.example.fucai.entity.Kl8Record;
import com.example.fucai.repository.Kl8RankHitStatRepository;
import com.example.fucai.repository.Kl8RecordRepository;
import org.springframework.data.domain.Sort;
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
import java.util.Set;

@Service
public class Kl8RankHitStatService {

    private static final int MAX_NUMBER = 80;
    private static final int PICK_SIZE = 20;
    private static final String TIER_RECOMMEND = "推荐20码";
    private static final String TIER_MIDDLE = "观望50码";
    private static final String TIER_AVOID = "不推荐10码";

    private final Kl8RecordRepository recordRepository;
    private final Kl8RankHitStatRepository statRepository;

    public Kl8RankHitStatService(Kl8RecordRepository recordRepository, Kl8RankHitStatRepository statRepository) {
        this.recordRepository = recordRepository;
        this.statRepository = statRepository;
    }

    @Transactional
    public Kl8RankHitGenerateResult generate(int windowSize) {
        int normalizedWindow = Math.max(1, windowSize);
        List<Kl8Record> records = recordRepository.findAll(Sort.by(Sort.Direction.ASC, "expect"));
        statRepository.deleteByWindowSize(normalizedWindow);
        if (records.size() <= normalizedWindow + 1) {
            return new Kl8RankHitGenerateResult(normalizedWindow, 0, 0);
        }

        List<Kl8RankHitStat> stats = new ArrayList<>();
        int issueCount = 0;
        for (int targetIndex = normalizedWindow + 1; targetIndex < records.size(); targetIndex++) {
            int baseIndex = targetIndex - 1;
            List<Kl8Record> history = records.subList(baseIndex - normalizedWindow, baseIndex);
            Kl8Record baseRecord = records.get(baseIndex);
            Kl8Record targetRecord = records.get(targetIndex);
            List<String> rankedNumbers = rankNumbers(history, baseRecord);
            Set<String> targetBalls = new HashSet<>(balls(targetRecord));
            issueCount++;

            for (int rankIndex = 0; rankIndex < rankedNumbers.size(); rankIndex++) {
                String number = rankedNumbers.get(rankIndex);
                if (!targetBalls.contains(number)) {
                    continue;
                }
                int overallRank = rankIndex + 1;
                Kl8RankHitStat stat = new Kl8RankHitStat();
                stat.setWindowSize(normalizedWindow);
                stat.setTargetExpect(targetRecord.getExpect());
                stat.setBaseExpect(baseRecord.getExpect());
                stat.setNumber(number);
                stat.setOverallRank(overallRank);
                stat.setTierName(tierName(overallRank));
                stat.setTierRank(tierRank(overallRank));
                stat.setCreatedAt(LocalDateTime.now());
                stats.add(stat);
            }
        }
        statRepository.saveAll(stats);
        return new Kl8RankHitGenerateResult(normalizedWindow, issueCount, stats.size());
    }

    public Kl8RankHitSummary summary(int windowSize, String beforeExpect) {
        int normalizedWindow = Math.max(1, windowSize);
        List<Kl8RankHitStat> details = beforeExpect == null || beforeExpect.trim().isEmpty()
                ? statRepository.findByWindowSizeOrderByTargetExpectDescOverallRankAsc(normalizedWindow)
                : statRepository.findByWindowSizeAndTargetExpectLessThanOrderByTargetExpectDescOverallRankAsc(
                        normalizedWindow,
                        beforeExpect.trim()
                );
        Set<String> issueSet = new HashSet<>();
        for (Kl8RankHitStat detail : details) {
            issueSet.add(detail.getTargetExpect());
        }
        int total = details.size();
        return new Kl8RankHitSummary(
                normalizedWindow,
                beforeExpect,
                issueSet.size(),
                total,
                bucketByTier(details, total),
                bucketByOverallRank(details, total),
                bucketByTierRank(details, total),
                details
        );
    }

    private List<String> rankNumbers(List<Kl8Record> history, Kl8Record baseRecord) {
        List<String> currentNumbers = balls(baseRecord);
        Set<String> currentSet = new HashSet<>(currentNumbers);
        Set<String> neighborSet = neighborNumbers(currentNumbers);
        List<Pair> pairs = buildPairs(history);
        Map<String, Integer> nextTotals = zeroCountMap();
        Map<String, Integer> nextHits = zeroCountMap();
        int nextBase = 0;
        for (String baseNumber : currentNumbers) {
            Map<String, Integer> counts = zeroCountMap();
            for (Pair pair : pairs) {
                if (!pair.current.contains(baseNumber)) {
                    continue;
                }
                nextBase++;
                for (String number : pair.next) {
                    counts.put(number, counts.get(number) + 1);
                    nextTotals.put(number, nextTotals.get(number) + 1);
                }
            }
            for (String number : numberPool()) {
                if (counts.get(number) > 0) {
                    nextHits.put(number, nextHits.get(number) + 1);
                }
            }
        }

        Map<String, Integer> coTotals = zeroCountMap();
        int coBase = 0;
        for (String baseNumber : currentNumbers) {
            for (Kl8Record record : history) {
                List<String> recordBalls = balls(record);
                if (!recordBalls.contains(baseNumber)) {
                    continue;
                }
                coBase++;
                for (String number : recordBalls) {
                    if (!number.equals(baseNumber)) {
                        coTotals.put(number, coTotals.get(number) + 1);
                    }
                }
            }
        }

        List<Candidate> candidates = new ArrayList<>();
        for (String number : numberPool()) {
            Omission omission = omission(number, history);
            int heat = appearCount(number, history);
            int recentRepeat = appearCount(number, history.subList(Math.max(0, history.size() - 3), history.size()));
            double expectedRate = currentNumbers.size() / (double) MAX_NUMBER;
            double nextRate = nextBase > 0 ? (nextTotals.get(number) + expectedRate * 8) / (nextBase + 8.0) : 0;
            double coRate = coBase > 0 ? (coTotals.get(number) + expectedRate * 8) / (coBase + 8.0) : 0;
            double hitRows = currentNumbers.isEmpty() ? 0 : nextHits.get(number) / (double) currentNumbers.size();
            double omissionAverageRatio = Math.min(2, omission.current / Math.max(1.0, omission.average));
            double omissionMaxRatio = omission.current / Math.max(1.0, omission.max);
            int runLength = currentRun(number, history);
            double runToMaxRatio = runLength / Math.max(1.0, omission.maxConsecutive);
            Candidate candidate = new Candidate(number);
            candidate.nextScore = nextTotals.get(number);
            candidate.nextRateScore = nextRate;
            candidate.coScore = coTotals.get(number);
            candidate.coRateScore = coRate;
            candidate.omissionScore = omission.score;
            candidate.omissionAverageScore = omissionAverageRatio;
            candidate.omissionMaxScore = omissionMaxRatio;
            candidate.maxConsecutiveScore = omission.maxConsecutive;
            candidate.runToMaxScore = runToMaxRatio;
            candidate.heatScore = heat;
            candidate.repeatScore = currentSet.contains(number) ? 1 : 0;
            candidate.currentRun = runLength;
            candidate.recentRepeatScore = recentRepeat;
            candidate.neighborScore = neighborSet.contains(number) ? 1 : 0;
            candidate.hitRowsScore = hitRows;
            candidate.zoneScore = zoneScore(number);
            candidate.tailScore = tailScore(number);
            candidates.add(candidate);
        }
        normalizeAndScore(candidates);
        Collections.sort(candidates);
        List<String> ranked = new ArrayList<>();
        for (Candidate candidate : candidates) {
            ranked.add(candidate.number);
        }
        return ranked;
    }

    private void normalizeAndScore(List<Candidate> candidates) {
        double maxNext = max(candidates, "next");
        double maxNextRate = max(candidates, "nextRate");
        double maxCo = max(candidates, "co");
        double maxCoRate = max(candidates, "coRate");
        double maxOmission = max(candidates, "omission");
        double maxOmissionAverage = max(candidates, "omissionAverage");
        double maxOmissionMax = max(candidates, "omissionMax");
        double maxConsecutive = max(candidates, "maxConsecutive");
        double maxRunToMax = max(candidates, "runToMax");
        double maxHeat = max(candidates, "heat");
        double maxRecentRepeat = max(candidates, "recentRepeat");
        double maxHitRows = max(candidates, "hitRows");
        double maxZone = max(candidates, "zone");
        double maxTail = max(candidates, "tail");
        for (Candidate candidate : candidates) {
            double next = normalize(candidate.nextScore, maxNext);
            double nextRate = normalize(candidate.nextRateScore, maxNextRate);
            double co = normalize(candidate.coScore, maxCo);
            double coRate = normalize(candidate.coRateScore, maxCoRate);
            double omission = normalize(candidate.omissionScore, maxOmission);
            double omissionAverage = normalize(candidate.omissionAverageScore, maxOmissionAverage);
            double omissionMax = normalize(candidate.omissionMaxScore, maxOmissionMax);
            double consecutive = normalize(candidate.maxConsecutiveScore, maxConsecutive);
            double runToMax = normalize(candidate.runToMaxScore, maxRunToMax);
            double heat = normalize(candidate.heatScore, maxHeat);
            double repeat = candidate.repeatScore > 0 ? 100 : 0;
            double recentRepeat = normalize(candidate.recentRepeatScore, maxRecentRepeat);
            double neighbor = candidate.neighborScore > 0 ? 100 : 0;
            double hitRows = normalize(candidate.hitRowsScore, maxHitRows);
            double zone = normalize(candidate.zoneScore, maxZone);
            double tail = normalize(candidate.tailScore, maxTail);
            double runExclusion = candidate.currentRun >= 3 ? 1000 : 0;
            candidate.score = next * 0.1
                    + nextRate * 0.18
                    + co * 0.04
                    + coRate * 0.06
                    + omission * 0.03
                    + omissionAverage * 0.025
                    + omissionMax * 0.02
                    + consecutive * 0.01
                    - runToMax * 0.035
                    + heat * 0.05
                    + repeat * 0.07
                    + recentRepeat * 0.07
                    + neighbor * 0.05
                    + hitRows * 0.06
                    + zone * 0.015
                    + tail * 0.015
                    - runExclusion;
        }
    }

    private double max(List<Candidate> candidates, String key) {
        double max = 0;
        for (Candidate candidate : candidates) {
            double value = candidate.value(key);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private double normalize(double value, double max) {
        return max > 0 ? value / max * 100 : 0;
    }

    private List<Kl8RankHitSummary.Bucket> bucketByTier(List<Kl8RankHitStat> details, int total) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(TIER_RECOMMEND, 0);
        counts.put(TIER_MIDDLE, 0);
        counts.put(TIER_AVOID, 0);
        for (Kl8RankHitStat detail : details) {
            counts.put(detail.getTierName(), counts.getOrDefault(detail.getTierName(), 0) + 1);
        }
        return toBuckets(counts, total);
    }

    private List<Kl8RankHitSummary.Bucket> bucketByOverallRank(List<Kl8RankHitStat> details, int total) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("1-10", 0);
        counts.put("11-20", 0);
        counts.put("21-40", 0);
        counts.put("41-60", 0);
        counts.put("61-70", 0);
        counts.put("71-80", 0);
        for (Kl8RankHitStat detail : details) {
            int rank = detail.getOverallRank();
            String key = rank <= 10 ? "1-10"
                    : rank <= 20 ? "11-20"
                    : rank <= 40 ? "21-40"
                    : rank <= 60 ? "41-60"
                    : rank <= 70 ? "61-70"
                    : "71-80";
            counts.put(key, counts.get(key) + 1);
        }
        return toBuckets(counts, total);
    }

    private List<Kl8RankHitSummary.Bucket> bucketByTierRank(List<Kl8RankHitStat> details, int total) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("1-5", 0);
        counts.put("6-10", 0);
        counts.put("11-20", 0);
        counts.put("21-35", 0);
        counts.put("36-50", 0);
        for (Kl8RankHitStat detail : details) {
            int rank = detail.getTierRank();
            String key = rank <= 5 ? "1-5"
                    : rank <= 10 ? "6-10"
                    : rank <= 20 ? "11-20"
                    : rank <= 35 ? "21-35"
                    : "36-50";
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return toBuckets(counts, total);
    }

    private List<Kl8RankHitSummary.Bucket> toBuckets(Map<String, Integer> counts, int total) {
        List<Kl8RankHitSummary.Bucket> buckets = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double rate = total > 0 ? entry.getValue() * 100.0 / total : 0;
            buckets.add(new Kl8RankHitSummary.Bucket(entry.getKey(), entry.getValue(), Math.round(rate * 100.0) / 100.0));
        }
        return buckets;
    }

    private String tierName(int overallRank) {
        if (overallRank <= 20) {
            return TIER_RECOMMEND;
        }
        if (overallRank <= 70) {
            return TIER_MIDDLE;
        }
        return TIER_AVOID;
    }

    private int tierRank(int overallRank) {
        if (overallRank <= 20) {
            return overallRank;
        }
        if (overallRank <= 70) {
            return overallRank - 20;
        }
        return overallRank - 70;
    }

    private List<Pair> buildPairs(List<Kl8Record> history) {
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < history.size() - 1; i++) {
            pairs.add(new Pair(balls(history.get(i)), balls(history.get(i + 1))));
        }
        return pairs;
    }

    private Map<String, Integer> zeroCountMap() {
        Map<String, Integer> result = new HashMap<>();
        for (String number : numberPool()) {
            result.put(number, 0);
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
        double average = misses.isEmpty() ? 0 : total / (double) misses.size();
        double score = max > 0 ? Math.min(current, max) / (double) max : 0;
        if (average > 0 && current >= average) {
            score += 0.25;
        }
        return new Omission(current, max, average, maxConsecutive, score);
    }

    private Set<String> neighborNumbers(List<String> numbers) {
        Set<String> result = new HashSet<>();
        for (String number : numbers) {
            int value = Integer.parseInt(number);
            if (value > 1) {
                result.add(normalize(value - 1));
            }
            if (value < MAX_NUMBER) {
                result.add(normalize(value + 1));
            }
        }
        result.removeAll(numbers);
        return result;
    }

    private double zoneScore(String number) {
        int value = Integer.parseInt(number);
        return 1 + (value - 1) / 10.0;
    }

    private double tailScore(String number) {
        return Integer.parseInt(number) % 10 + 1;
    }

    private List<String> balls(Kl8Record record) {
        return Arrays.asList(
                record.getNum1(), record.getNum2(), record.getNum3(), record.getNum4(), record.getNum5(),
                record.getNum6(), record.getNum7(), record.getNum8(), record.getNum9(), record.getNum10(),
                record.getNum11(), record.getNum12(), record.getNum13(), record.getNum14(), record.getNum15(),
                record.getNum16(), record.getNum17(), record.getNum18(), record.getNum19(), record.getNum20()
        );
    }

    private String normalize(int value) {
        return String.format("%02d", value);
    }

    private static class Pair {
        private final List<String> current;
        private final List<String> next;

        private Pair(List<String> current, List<String> next) {
            this.current = current;
            this.next = next;
        }
    }

    private static class Omission {
        private final int current;
        private final int max;
        private final double average;
        private final int maxConsecutive;
        private final double score;

        private Omission(int current, int max, double average, int maxConsecutive, double score) {
            this.current = current;
            this.max = max;
            this.average = average;
            this.maxConsecutive = maxConsecutive;
            this.score = score;
        }
    }

    private static class Candidate implements Comparable<Candidate> {
        private final String number;
        private double nextScore;
        private double nextRateScore;
        private double coScore;
        private double coRateScore;
        private double omissionScore;
        private double omissionAverageScore;
        private double omissionMaxScore;
        private double maxConsecutiveScore;
        private double runToMaxScore;
        private double heatScore;
        private double repeatScore;
        private double currentRun;
        private double recentRepeatScore;
        private double neighborScore;
        private double hitRowsScore;
        private double zoneScore;
        private double tailScore;
        private double score;

        private Candidate(String number) {
            this.number = number;
        }

        private double value(String key) {
            switch (key) {
                case "next":
                    return nextScore;
                case "nextRate":
                    return nextRateScore;
                case "co":
                    return coScore;
                case "coRate":
                    return coRateScore;
                case "omission":
                    return omissionScore;
                case "omissionAverage":
                    return omissionAverageScore;
                case "omissionMax":
                    return omissionMaxScore;
                case "maxConsecutive":
                    return maxConsecutiveScore;
                case "runToMax":
                    return runToMaxScore;
                case "heat":
                    return heatScore;
                case "recentRepeat":
                    return recentRepeatScore;
                case "hitRows":
                    return hitRowsScore;
                case "zone":
                    return zoneScore;
                case "tail":
                    return tailScore;
                default:
                    return 0;
            }
        }

        @Override
        public int compareTo(Candidate other) {
            int scoreCompare = Double.compare(other.score, score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int nextCompare = Double.compare(other.nextScore, nextScore);
            if (nextCompare != 0) {
                return nextCompare;
            }
            return Integer.compare(Integer.parseInt(number), Integer.parseInt(other.number));
        }
    }
}
