package com.example.fucai.repository;

import com.example.fucai.entity.SsqRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SsqRecordRepository extends JpaRepository<SsqRecord, Long> {

    Optional<SsqRecord> findByExpect(String expect);
}
