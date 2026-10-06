package com.example.fucai.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dlt_record", uniqueConstraints = @UniqueConstraint(columnNames = "expect"))
public class DltRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String expect;

    @Column(nullable = false, length = 2)
    private String front1;

    @Column(nullable = false, length = 2)
    private String front2;

    @Column(nullable = false, length = 2)
    private String front3;

    @Column(nullable = false, length = 2)
    private String front4;

    @Column(nullable = false, length = 2)
    private String front5;

    @Column(nullable = false, length = 2)
    private String back1;

    @Column(nullable = false, length = 2)
    private String back2;

    private LocalDate drawDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExpect() {
        return expect;
    }

    public void setExpect(String expect) {
        this.expect = expect;
    }

    public String getFront1() {
        return front1;
    }

    public void setFront1(String front1) {
        this.front1 = front1;
    }

    public String getFront2() {
        return front2;
    }

    public void setFront2(String front2) {
        this.front2 = front2;
    }

    public String getFront3() {
        return front3;
    }

    public void setFront3(String front3) {
        this.front3 = front3;
    }

    public String getFront4() {
        return front4;
    }

    public void setFront4(String front4) {
        this.front4 = front4;
    }

    public String getFront5() {
        return front5;
    }

    public void setFront5(String front5) {
        this.front5 = front5;
    }

    public String getBack1() {
        return back1;
    }

    public void setBack1(String back1) {
        this.back1 = back1;
    }

    public String getBack2() {
        return back2;
    }

    public void setBack2(String back2) {
        this.back2 = back2;
    }

    public LocalDate getDrawDate() {
        return drawDate;
    }

    public void setDrawDate(LocalDate drawDate) {
        this.drawDate = drawDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
