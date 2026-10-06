package com.example.fucai.repository;

import com.example.fucai.entity.Kl8WeightRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Kl8WeightRunRepository extends JpaRepository<Kl8WeightRun, Long> {

    Optional<Kl8WeightRun> findTopByWindowSizeOrderByCreatedAtDesc(Integer windowSize);

    List<Kl8WeightRun> findTop10ByOrderByCreatedAtDesc();
}
