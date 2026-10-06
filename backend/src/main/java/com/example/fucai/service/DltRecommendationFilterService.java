package com.example.fucai.service;

import com.example.fucai.entity.DltRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DltRecommendationFilterService {

    private static final int FRONT_MAX = 35;
    private static final int FRONT_SIZE = 5;

    private final DltCrawlerService crawlerService;

    public DltRecommendationFilterService(DltCrawlerService crawlerService) {
        this.crawlerService = crawlerService;
    }

    public List<DltRecommendationFilterRequest.RecommendationGroup> filter(
            DltRecommendationFilterRequest request
    ) {
        if (request == null || request.getGroups() == null || request.getGroups().isEmpty()) {
            return Collections.emptyList();
        }

        List<DltRecord> history = crawlerService.findAll().stream()
                .filter(record -> isBefore(record.getExpect(), request.getBeforeExpect()))
                .collect(Collectors.toList());
        Set<String> historicalFrontKeys = new HashSet<>();
        Set<String> historicalCompleteKeys = new HashSet<>();
        for (DltRecord record : history) {
            List<String> front = Arrays.asList(
                    record.getFront1(),
                    record.getFront2(),
                    record.getFront3(),
                    record.getFront4(),
                    record.getFront5()
            );
            List<String> back = Arrays.asList(record.getBack1(), record.getBack2());
            historicalFrontKeys.add(frontKey(front));
            historicalCompleteKeys.add(completeKey(front, back));
        }

        Set<String> usedFrontKeys = new HashSet<>();
        List<DltRecommendationFilterRequest.RecommendationGroup> result = new ArrayList<>();
        for (DltRecommendationFilterRequest.RecommendationGroup group : request.getGroups()) {
            if (group == null || group.getAreas() == null) {
                continue;
            }
            DltRecommendationFilterRequest.RecommendationArea frontArea = findArea(group, "front");
            DltRecommendationFilterRequest.RecommendationArea backArea = findArea(group, "back");
            if (frontArea == null || backArea == null) {
                continue;
            }

            List<String> originalFront = normalizeNumbers(frontArea.getNumbers(), FRONT_SIZE);
            List<String> originalBack = normalizeNumbers(backArea.getNumbers(), 2);
            List<String> selectedFront = chooseFront(
                    originalFront,
                    originalBack,
                    historicalFrontKeys,
                    historicalCompleteKeys,
                    usedFrontKeys
            );
            if (selectedFront.size() != FRONT_SIZE) {
                continue;
            }

            String selectedFrontKey = frontKey(selectedFront);
            usedFrontKeys.add(selectedFrontKey);
            frontArea.setNumbers(selectedFront);
            backArea.setNumbers(originalBack);
            result.add(group);
        }
        return result;
    }

    private DltRecommendationFilterRequest.RecommendationArea findArea(
            DltRecommendationFilterRequest.RecommendationGroup group,
            String key
    ) {
        return group.getAreas().stream()
                .filter(area -> area != null && key.equals(area.getKey()))
                .findFirst()
                .orElse(null);
    }

    private List<String> chooseFront(
            List<String> originalFront,
            List<String> originalBack,
            Set<String> historicalFrontKeys,
            Set<String> historicalCompleteKeys,
            Set<String> usedFrontKeys
    ) {
        if (isAllowed(originalFront, originalBack, historicalFrontKeys, historicalCompleteKeys)
                && !usedFrontKeys.contains(frontKey(originalFront))) {
            return originalFront;
        }

        List<String> best = findBestFront(
                originalFront,
                originalBack,
                historicalFrontKeys,
                historicalCompleteKeys,
                usedFrontKeys,
                true
        );
        if (best.isEmpty()) {
            best = findBestFront(
                    originalFront,
                    originalBack,
                    historicalFrontKeys,
                    historicalCompleteKeys,
                    usedFrontKeys,
                    false
            );
        }
        return best;
    }

    private List<String> findBestFront(
            List<String> originalFront,
            List<String> originalBack,
            Set<String> historicalFrontKeys,
            Set<String> historicalCompleteKeys,
            Set<String> usedFrontKeys,
            boolean avoidUsed
    ) {
        List<String> best = Collections.emptyList();
        int bestOverlap = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int first = 1; first <= FRONT_MAX - 4; first++) {
            for (int second = first + 1; second <= FRONT_MAX - 3; second++) {
                for (int third = second + 1; third <= FRONT_MAX - 2; third++) {
                    for (int fourth = third + 1; fourth <= FRONT_MAX - 1; fourth++) {
                        for (int fifth = fourth + 1; fifth <= FRONT_MAX; fifth++) {
                            List<String> candidate = Arrays.asList(
                                    format(first),
                                    format(second),
                                    format(third),
                                    format(fourth),
                                    format(fifth)
                            );
                            String key = frontKey(candidate);
                            if (avoidUsed && usedFrontKeys.contains(key)) {
                                continue;
                            }
                            if (!isAllowed(candidate, originalBack, historicalFrontKeys, historicalCompleteKeys)) {
                                continue;
                            }
                            int overlap = overlapCount(candidate, originalFront);
                            int distance = distance(candidate, originalFront);
                            if (overlap > bestOverlap
                                    || (overlap == bestOverlap && distance < bestDistance)) {
                                best = candidate;
                                bestOverlap = overlap;
                                bestDistance = distance;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean isAllowed(
            List<String> front,
            List<String> back,
            Set<String> historicalFrontKeys,
            Set<String> historicalCompleteKeys
    ) {
        if (front.size() != FRONT_SIZE || back.size() != 2) {
            return false;
        }
        if (containsFourConsecutive(front)) {
            return false;
        }
        if (historicalFrontKeys.contains(frontKey(front))) {
            return false;
        }
        return !historicalCompleteKeys.contains(completeKey(front, back));
    }

    private boolean containsFourConsecutive(List<String> numbers) {
        if (numbers.size() < 4) {
            return false;
        }
        for (int index = 0; index <= numbers.size() - 4; index++) {
            int first = Integer.parseInt(numbers.get(index));
            if (Integer.parseInt(numbers.get(index + 1)) == first + 1
                    && Integer.parseInt(numbers.get(index + 2)) == first + 2
                    && Integer.parseInt(numbers.get(index + 3)) == first + 3) {
                return true;
            }
        }
        return false;
    }

    private int overlapCount(List<String> left, List<String> right) {
        int count = 0;
        for (String number : left) {
            if (right.contains(number)) {
                count++;
            }
        }
        return count;
    }

    private int distance(List<String> left, List<String> right) {
        int distance = 0;
        for (int index = 0; index < left.size(); index++) {
            int leftNumber = Integer.parseInt(left.get(index));
            int rightNumber = index < right.size() ? Integer.parseInt(right.get(index)) : leftNumber;
            distance += Math.abs(leftNumber - rightNumber);
        }
        return distance;
    }

    private List<String> normalizeNumbers(List<String> numbers, int expectedSize) {
        if (numbers == null) {
            return Collections.emptyList();
        }
        return numbers.stream()
                .filter(number -> number != null && number.matches("\\d+"))
                .map(number -> format(Integer.parseInt(number)))
                .distinct()
                .sorted((left, right) -> Integer.compare(Integer.parseInt(left), Integer.parseInt(right)))
                .limit(expectedSize)
                .collect(Collectors.toList());
    }

    private String frontKey(List<String> numbers) {
        return normalizeNumbers(numbers, FRONT_SIZE).stream().collect(Collectors.joining(","));
    }

    private String completeKey(List<String> front, List<String> back) {
        return frontKey(front) + "|" + normalizeNumbers(back, 2).stream().collect(Collectors.joining(","));
    }

    private boolean isBefore(String expect, String beforeExpect) {
        if (beforeExpect == null || beforeExpect.trim().isEmpty()) {
            return true;
        }
        try {
            return Long.parseLong(expect) < Long.parseLong(beforeExpect);
        } catch (NumberFormatException ex) {
            return expect.compareTo(beforeExpect) < 0;
        }
    }

    private String format(int number) {
        return String.format("%02d", number);
    }
}
