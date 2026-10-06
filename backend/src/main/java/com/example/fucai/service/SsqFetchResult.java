package com.example.fucai.service;

public class SsqFetchResult {

    private final int parsedCount;
    private final int insertedCount;
    private final int skippedCount;

    public SsqFetchResult(int parsedCount, int insertedCount, int skippedCount) {
        this.parsedCount = parsedCount;
        this.insertedCount = insertedCount;
        this.skippedCount = skippedCount;
    }

    public int getParsedCount() {
        return parsedCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }
}
