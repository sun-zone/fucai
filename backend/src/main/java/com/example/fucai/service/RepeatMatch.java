package com.example.fucai.service;

import java.util.List;

public class RepeatMatch {

    private final String expect;
    private final List<String> frontBalls;
    private final List<String> backBalls;
    private final List<String> matchedBalls;
    private final int matchedCount;

    public RepeatMatch(String expect, List<String> frontBalls, List<String> backBalls, List<String> matchedBalls) {
        this.expect = expect;
        this.frontBalls = frontBalls;
        this.backBalls = backBalls;
        this.matchedBalls = matchedBalls;
        this.matchedCount = matchedBalls.size();
    }

    public String getExpect() {
        return expect;
    }

    public List<String> getFrontBalls() {
        return frontBalls;
    }

    public List<String> getBackBalls() {
        return backBalls;
    }

    public List<String> getMatchedBalls() {
        return matchedBalls;
    }

    public int getMatchedCount() {
        return matchedCount;
    }
}
