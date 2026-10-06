package com.example.fucai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(
        name = "kl8_feature_weight",
        indexes = {
                @Index(name = "idx_kl8_feature_weight_run", columnList = "run_id"),
                @Index(name = "idx_kl8_feature_weight_name", columnList = "feature_name")
        }
)
public class Kl8FeatureWeight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    @JsonIgnore
    private Kl8WeightRun run;

    @Column(name = "feature_name", nullable = false, length = 60)
    private String featureName;

    @Column(name = "hit_average")
    private Double hitAverage;

    @Column(name = "miss_average")
    private Double missAverage;

    @Column
    private Double contribution;

    @Column
    private Double weight;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Kl8WeightRun getRun() {
        return run;
    }

    public void setRun(Kl8WeightRun run) {
        this.run = run;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

    public Double getHitAverage() {
        return hitAverage;
    }

    public void setHitAverage(Double hitAverage) {
        this.hitAverage = hitAverage;
    }

    public Double getMissAverage() {
        return missAverage;
    }

    public void setMissAverage(Double missAverage) {
        this.missAverage = missAverage;
    }

    public Double getContribution() {
        return contribution;
    }

    public void setContribution(Double contribution) {
        this.contribution = contribution;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
