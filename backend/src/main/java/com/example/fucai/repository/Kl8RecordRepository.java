package com.example.fucai.repository;

import com.example.fucai.entity.Kl8Record;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Kl8RecordRepository extends JpaRepository<Kl8Record, Long> {

    Optional<Kl8Record> findByExpect(String expect);
}
