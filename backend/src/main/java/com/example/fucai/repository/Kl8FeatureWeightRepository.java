package com.example.fucai.repository;

import com.example.fucai.entity.Kl8FeatureWeight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Kl8FeatureWeightRepository extends JpaRepository<Kl8FeatureWeight, Long> {

    List<Kl8FeatureWeight> findByRunIdOrderByContributionDesc(Long runId);
}
