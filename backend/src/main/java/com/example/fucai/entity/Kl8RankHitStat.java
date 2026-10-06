package com.example.fucai.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "kl8_rank_hit_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"window_size", "target_expect", "number"}),
        indexes = {
                @Index(name = "idx_kl8_rank_hit_window_expect", columnList = "window_size,target_expect"),
                @Index(name = "idx_kl8_rank_hit_overall_rank", columnList = "window_size,overall_rank"),
                @Index(name = "idx_kl8_rank_hit_tier_rank", columnList = "window_size,tier_name,tier_rank")
        }
)
public class Kl8RankHitStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "window_size", nullable = false)
    private Integer windowSize;

    @Column(name = "target_expect", nullable = false, length = 20)
    private String targetExpect;

    @Column(name = "base_expect", nullable = false, length = 20)
    private String baseExpect;

    @Column(nullable = false, length = 2)
    private String number;

    @Column(name = "tier_name", nullable = false, length = 20)
    private String tierName;

    @Column(name = "tier_rank", nullable = false)
    private Integer tierRank;

    @Column(name = "overall_rank", nullable = false)
    private Integer overallRank;

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

    public String getTargetExpect() {
        return targetExpect;
    }

    public void setTargetExpect(String targetExpect) {
        this.targetExpect = targetExpect;
    }

    public String getBaseExpect() {
        return baseExpect;
    }

    public void setBaseExpect(String baseExpect) {
        this.baseExpect = baseExpect;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTierName() {
        return tierName;
    }

    public void setTierName(String tierName) {
        this.tierName = tierName;
    }

    public Integer getTierRank() {
        return tierRank;
    }

    public void setTierRank(Integer tierRank) {
        this.tierRank = tierRank;
    }

    public Integer getOverallRank() {
        return overallRank;
    }

    public void setOverallRank(Integer overallRank) {
        this.overallRank = overallRank;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
