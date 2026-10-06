package com.example.fucai.repository;

import com.example.fucai.entity.Kl8RankHitStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Kl8RankHitStatRepository extends JpaRepository<Kl8RankHitStat, Long> {

    void deleteByWindowSize(Integer windowSize);

    List<Kl8RankHitStat> findByWindowSizeOrderByTargetExpectDescOverallRankAsc(Integer windowSize);

    List<Kl8RankHitStat> findByWindowSizeAndTargetExpectLessThanOrderByTargetExpectDescOverallRankAsc(
            Integer windowSize,
            String targetExpect
    );
}
