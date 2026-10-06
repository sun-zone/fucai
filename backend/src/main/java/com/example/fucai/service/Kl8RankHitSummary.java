package com.example.fucai.service;

import com.example.fucai.entity.Kl8RankHitStat;

import java.util.List;

public class Kl8RankHitSummary {

    private final int windowSize;
    private final String beforeExpect;
    private final int sampleIssueCount;
    private final int totalHitCount;
    private final List<Bucket> tierBuckets;
    private final List<Bucket> overallRankBuckets;
    private final List<Bucket> tierRankBuckets;
    private final List<Kl8RankHitStat> details;

    public Kl8RankHitSummary(
            int windowSize,
            String beforeExpect,
            int sampleIssueCount,
            int totalHitCount,
            List<Bucket> tierBuckets,
            List<Bucket> overallRankBuckets,
            List<Bucket> tierRankBuckets,
            List<Kl8RankHitStat> details
    ) {
        this.windowSize = windowSize;
        this.beforeExpect = beforeExpect;
        this.sampleIssueCount = sampleIssueCount;
        this.totalHitCount = totalHitCount;
        this.tierBuckets = tierBuckets;
        this.overallRankBuckets = overallRankBuckets;
        this.tierRankBuckets = tierRankBuckets;
        this.details = details;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public String getBeforeExpect() {
        return beforeExpect;
    }

    public int getSampleIssueCount() {
        return sampleIssueCount;
    }

    public int getTotalHitCount() {
        return totalHitCount;
    }

    public List<Bucket> getTierBuckets() {
        return tierBuckets;
    }

    public List<Bucket> getOverallRankBuckets() {
        return overallRankBuckets;
    }

    public List<Bucket> getTierRankBuckets() {
        return tierRankBuckets;
    }

    public List<Kl8RankHitStat> getDetails() {
        return details;
    }

    public static class Bucket {
        private final String key;
        private final int count;
        private final double rate;

        public Bucket(String key, int count, double rate) {
            this.key = key;
            this.count = count;
            this.rate = rate;
        }

        public String getKey() {
            return key;
        }

        public int getCount() {
            return count;
        }

        public double getRate() {
            return rate;
        }
    }
}
