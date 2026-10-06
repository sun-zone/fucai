package com.example.fucai.service;

import java.util.List;

public class RepeatStat {

    private final String expect;
    private final List<String> frontBalls;
    private final List<String> backBalls;
    private final int repeatCount;
    private final List<RepeatMatch> matches;

    public RepeatStat(String expect, List<String> frontBalls, List<String> backBalls, List<RepeatMatch> matches) {
        this.expect = expect;
        this.frontBalls = frontBalls;
        this.backBalls = backBalls;
        this.matches = matches;
        this.repeatCount = matches.size();
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

    public int getRepeatCount() {
        return repeatCount;
    }

    public List<RepeatMatch> getMatches() {
        return matches;
    }
}
