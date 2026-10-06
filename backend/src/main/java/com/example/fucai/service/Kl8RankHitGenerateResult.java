package com.example.fucai.service;

public class Kl8RankHitGenerateResult {

    private final int windowSize;
    private final int issueCount;
    private final int detailCount;

    public Kl8RankHitGenerateResult(int windowSize, int issueCount, int detailCount) {
        this.windowSize = windowSize;
        this.issueCount = issueCount;
        this.detailCount = detailCount;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public int getIssueCount() {
        return issueCount;
    }

    public int getDetailCount() {
        return detailCount;
    }
}
