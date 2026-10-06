package com.example.fucai.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "kl8_weight_run",
        indexes = {
                @Index(name = "idx_kl8_weight_run_created", columnList = "created_at"),
                @Index(name = "idx_kl8_weight_run_window", columnList = "window_size")
        }
)
public class Kl8WeightRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_size", nullable = false)
    private Integer windowSize;

    @Column(name = "sample_issue_count", nullable = false)
    private Integer sampleIssueCount;

    @Column(name = "sample_number_count", nullable = false)
    private Integer sampleNumberCount;

    @Column(name = "hit_number_count", nullable = false)
    private Integer hitNumberCount;

    @Column(name = "average_hit_count")
    private Double averageHitCount;

    @Column(name = "start_expect", length = 20)
    private String startExpect;

    @Column(name = "end_expect", length = 20)
    private String endExpect;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }

    public Integer getSampleIssueCount() {
        return sampleIssueCount;
    }

    public void setSampleIssueCount(Integer sampleIssueCount) {
        this.sampleIssueCount = sampleIssueCount;
    }

    public Integer getSampleNumberCount() {
        return sampleNumberCount;
    }

    public void setSampleNumberCount(Integer sampleNumberCount) {
        this.sampleNumberCount = sampleNumberCount;
    }

    public Integer getHitNumberCount() {
        return hitNumberCount;
    }

    public void setHitNumberCount(Integer hitNumberCount) {
        this.hitNumberCount = hitNumberCount;
    }

    public Double getAverageHitCount() {
        return averageHitCount;
    }

    public void setAverageHitCount(Double averageHitCount) {
        this.averageHitCount = averageHitCount;
    }

    public String getStartExpect() {
        return startExpect;
    }

    public void setStartExpect(String startExpect) {
        this.startExpect = startExpect;
    }

    public String getEndExpect() {
        return endExpect;
    }

    public void setEndExpect(String endExpect) {
        this.endExpect = endExpect;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
