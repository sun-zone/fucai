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
@Table(name = "kl8_record", uniqueConstraints = @UniqueConstraint(columnNames = "expect"))
public class Kl8Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String expect;

    @Column(nullable = false, length = 2)
    private String num1;

    @Column(nullable = false, length = 2)
    private String num2;

    @Column(nullable = false, length = 2)
    private String num3;

    @Column(nullable = false, length = 2)
    private String num4;

    @Column(nullable = false, length = 2)
    private String num5;

    @Column(nullable = false, length = 2)
    private String num6;

    @Column(nullable = false, length = 2)
    private String num7;

    @Column(nullable = false, length = 2)
    private String num8;

    @Column(nullable = false, length = 2)
    private String num9;

    @Column(nullable = false, length = 2)
    private String num10;

    @Column(nullable = false, length = 2)
    private String num11;

    @Column(nullable = false, length = 2)
    private String num12;

    @Column(nullable = false, length = 2)
    private String num13;

    @Column(nullable = false, length = 2)
    private String num14;

    @Column(nullable = false, length = 2)
    private String num15;

    @Column(nullable = false, length = 2)
    private String num16;

    @Column(nullable = false, length = 2)
    private String num17;

    @Column(nullable = false, length = 2)
    private String num18;

    @Column(nullable = false, length = 2)
    private String num19;

    @Column(nullable = false, length = 2)
    private String num20;

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

    public String getNum1() {
        return num1;
    }

    public void setNum1(String num1) {
        this.num1 = num1;
    }

    public String getNum2() {
        return num2;
    }

    public void setNum2(String num2) {
        this.num2 = num2;
    }

    public String getNum3() {
        return num3;
    }

    public void setNum3(String num3) {
        this.num3 = num3;
    }

    public String getNum4() {
        return num4;
    }

    public void setNum4(String num4) {
        this.num4 = num4;
    }

    public String getNum5() {
        return num5;
    }

    public void setNum5(String num5) {
        this.num5 = num5;
    }

    public String getNum6() {
        return num6;
    }

    public void setNum6(String num6) {
        this.num6 = num6;
    }

    public String getNum7() {
        return num7;
    }

    public void setNum7(String num7) {
        this.num7 = num7;
    }

    public String getNum8() {
        return num8;
    }

    public void setNum8(String num8) {
        this.num8 = num8;
    }

    public String getNum9() {
        return num9;
    }

    public void setNum9(String num9) {
        this.num9 = num9;
    }

    public String getNum10() {
        return num10;
    }

    public void setNum10(String num10) {
        this.num10 = num10;
    }

    public String getNum11() {
        return num11;
    }

    public void setNum11(String num11) {
        this.num11 = num11;
    }

    public String getNum12() {
        return num12;
    }

    public void setNum12(String num12) {
        this.num12 = num12;
    }

    public String getNum13() {
        return num13;
    }

    public void setNum13(String num13) {
        this.num13 = num13;
    }

    public String getNum14() {
        return num14;
    }

    public void setNum14(String num14) {
        this.num14 = num14;
    }

    public String getNum15() {
        return num15;
    }

    public void setNum15(String num15) {
        this.num15 = num15;
    }

    public String getNum16() {
        return num16;
    }

    public void setNum16(String num16) {
        this.num16 = num16;
    }

    public String getNum17() {
        return num17;
    }

    public void setNum17(String num17) {
        this.num17 = num17;
    }

    public String getNum18() {
        return num18;
    }

    public void setNum18(String num18) {
        this.num18 = num18;
    }

    public String getNum19() {
        return num19;
    }

    public void setNum19(String num19) {
        this.num19 = num19;
    }

    public String getNum20() {
        return num20;
    }

    public void setNum20(String num20) {
        this.num20 = num20;
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
