package com.example.fucai.service;

import java.time.LocalDateTime;

public class Kl8WeightOptimizationStatus {

    private String status = "IDLE";
    private Integer currentWindow;
    private Integer finishedWindowCount = 0;
    private Integer totalWindowCount = 3;
    private String message = "未开始";
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCurrentWindow() {
        return currentWindow;
    }

    public void setCurrentWindow(Integer currentWindow) {
        this.currentWindow = currentWindow;
    }

    public Integer getFinishedWindowCount() {
        return finishedWindowCount;
    }

    public void setFinishedWindowCount(Integer finishedWindowCount) {
        this.finishedWindowCount = finishedWindowCount;
    }

    public Integer getTotalWindowCount() {
        return totalWindowCount;
    }

    public void setTotalWindowCount(Integer totalWindowCount) {
        this.totalWindowCount = totalWindowCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
