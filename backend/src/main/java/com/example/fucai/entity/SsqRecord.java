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
@Table(name = "ssq_record", uniqueConstraints = @UniqueConstraint(columnNames = "expect"))
public class SsqRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String expect;

    @Column(nullable = false, length = 2)
    private String red1;

    @Column(nullable = false, length = 2)
    private String red2;

    @Column(nullable = false, length = 2)
    private String red3;

    @Column(nullable = false, length = 2)
    private String red4;

    @Column(nullable = false, length = 2)
    private String red5;

    @Column(nullable = false, length = 2)
    private String red6;

    @Column(nullable = false, length = 2)
    private String blue;

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

    public String getRed1() {
        return red1;
    }

    public void setRed1(String red1) {
        this.red1 = red1;
    }

    public String getRed2() {
        return red2;
    }

    public void setRed2(String red2) {
        this.red2 = red2;
    }

    public String getRed3() {
        return red3;
    }

    public void setRed3(String red3) {
        this.red3 = red3;
    }

    public String getRed4() {
        return red4;
    }

    public void setRed4(String red4) {
        this.red4 = red4;
    }

    public String getRed5() {
        return red5;
    }

    public void setRed5(String red5) {
        this.red5 = red5;
    }

    public String getRed6() {
        return red6;
    }

    public void setRed6(String red6) {
        this.red6 = red6;
    }

    public String getBlue() {
        return blue;
    }

    public void setBlue(String blue) {
        this.blue = blue;
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
