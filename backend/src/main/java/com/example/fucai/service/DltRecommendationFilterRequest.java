package com.example.fucai.service;

import java.util.ArrayList;
import java.util.List;

public class DltRecommendationFilterRequest {

    private String beforeExpect;
    private List<RecommendationGroup> groups = new ArrayList<>();

    public String getBeforeExpect() {
        return beforeExpect;
    }

    public void setBeforeExpect(String beforeExpect) {
        this.beforeExpect = beforeExpect;
    }

    public List<RecommendationGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<RecommendationGroup> groups) {
        this.groups = groups;
    }

    public static class RecommendationGroup {

        private Integer index;
        private List<RecommendationArea> areas = new ArrayList<>();

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public List<RecommendationArea> getAreas() {
            return areas;
        }

        public void setAreas(List<RecommendationArea> areas) {
            this.areas = areas;
        }
    }

    public static class RecommendationArea {

        private String key;
        private String label;
        private List<String> numbers = new ArrayList<>();
        private String source;
        private String totalScore;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<String> getNumbers() {
            return numbers;
        }

        public void setNumbers(List<String> numbers) {
            this.numbers = numbers;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(String totalScore) {
            this.totalScore = totalScore;
        }
    }
}
