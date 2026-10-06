package com.example.fucai.repository;

import com.example.fucai.entity.DltRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DltRecordRepository extends JpaRepository<DltRecord, Long> {

    Optional<DltRecord> findByExpect(String expect);
}
