package com.example.fucai.service;

import com.example.fucai.entity.Kl8FeatureWeight;
import com.example.fucai.entity.Kl8WeightRun;

import java.util.List;

public class Kl8WeightOptimizationResult {

    private final Kl8WeightRun run;
    private final List<Kl8FeatureWeight> weights;

    public Kl8WeightOptimizationResult(Kl8WeightRun run, List<Kl8FeatureWeight> weights) {
        this.run = run;
        this.weights = weights;
    }

    public Kl8WeightRun getRun() {
        return run;
    }

    public List<Kl8FeatureWeight> getWeights() {
        return weights;
    }
}
