package com.example.fucai.service;

import com.example.fucai.entity.DltRecord;
import com.example.fucai.repository.DltRecordRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DltCrawlerService {

    private static final String FIRST_EXPECT = "07001";
    private static final String LATEST_EXPECT = "99999";

    private final DltRecordRepository repository;

    @Value("${fucai.dlt-history-url}")
    private String dltHistoryUrl;

    public DltCrawlerService(DltRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SsqFetchResult syncHistory() throws IOException {
        return saveRecords(parseHistoryDocument(fetchHistoryDocument()));
    }

    @Transactional
    public SsqFetchResult fetchAndSaveRecent(int expect) throws IOException {
        List<DltRecord> records = parseHistoryDocument(fetchHistoryDocument());
        return saveRecords(records.subList(0, Math.min(Math.max(expect, 1), records.size())));
    }

    public List<DltRecord> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "expect"));
    }

    public List<RepeatStat> analyzeRepeats(int matchCount) {
        if (matchCount < 2 || matchCount > 5) {
            throw new IllegalArgumentException("大乐透重复数范围必须是 2 到 5");
        }

        List<DltRecord> records = findAll();
        List<List<String>> ballsList = new ArrayList<>();
        for (DltRecord record : records) {
            ballsList.add(frontBalls(record));
        }

        List<RepeatStat> stats = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            List<RepeatMatch> matches = new ArrayList<>();
            for (int j = 0; j < records.size(); j++) {
                List<String> matchedBalls = intersectionBalls(ballsList.get(i), ballsList.get(j));
                if (i != j && matchedBalls.size() == matchCount) {
                    DltRecord matchedRecord = records.get(j);
                    matches.add(new RepeatMatch(
                            matchedRecord.getExpect(),
                            ballsList.get(j),
                            Arrays.asList(matchedRecord.getBack1(), matchedRecord.getBack2()),
                            matchedBalls
                    ));
                }
            }
            DltRecord record = records.get(i);
            stats.add(new RepeatStat(record.getExpect(), ballsList.get(i), Arrays.asList(record.getBack1(), record.getBack2()), matches));
        }
        return stats;
    }

    private SsqFetchResult saveRecords(List<DltRecord> records) {
        int inserted = 0;

        for (DltRecord record : records) {
            DltRecord saved = repository.findByExpect(record.getExpect()).orElse(null);
            if (saved != null) {
                copyRecord(record, saved);
                repository.save(saved);
            } else {
                repository.save(record);
                inserted++;
            }
        }

        return new SsqFetchResult(records.size(), inserted, records.size() - inserted);
    }

    private Document fetchHistoryDocument() throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl(dltHistoryUrl)
                .queryParam("start", FIRST_EXPECT)
                .queryParam("end", LATEST_EXPECT)
                .toUriString();

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .maxBodySize(0)
                .get();
    }

    private List<DltRecord> parseHistoryDocument(Document document) {
        List<DltRecord> records = new ArrayList<>();
        for (Element row : document.select("#tdata tr")) {
            DltRecord record = parseHistoryRow(row);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private DltRecord parseHistoryRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 8) {
            return null;
        }

        String expect = cells.get(0).text().trim();
        if (expect.isEmpty() || !expect.matches("\\d+")) {
            return null;
        }

        DltRecord record = new DltRecord();
        record.setExpect(expect);
        record.setFront1(normalizeBall(cells.get(1).text()));
        record.setFront2(normalizeBall(cells.get(2).text()));
        record.setFront3(normalizeBall(cells.get(3).text()));
        record.setFront4(normalizeBall(cells.get(4).text()));
        record.setFront5(normalizeBall(cells.get(5).text()));
        record.setBack1(normalizeBall(cells.get(6).text()));
        record.setBack2(normalizeBall(cells.get(7).text()));
        record.setDrawDate(parseDrawDate(cells.get(cells.size() - 1).text()));
        return record;
    }

    private void copyRecord(DltRecord source, DltRecord target) {
        target.setFront1(source.getFront1());
        target.setFront2(source.getFront2());
        target.setFront3(source.getFront3());
        target.setFront4(source.getFront4());
        target.setFront5(source.getFront5());
        target.setBack1(source.getBack1());
        target.setBack2(source.getBack2());
        if (source.getDrawDate() != null) {
            target.setDrawDate(source.getDrawDate());
        }
    }

    private LocalDate parseDrawDate(String text) {
        String value = text.trim();
        return value.matches("\\d{4}-\\d{2}-\\d{2}") ? LocalDate.parse(value) : null;
    }

    private String normalizeBall(String text) {
        return String.format("%02d", Integer.parseInt(text.trim()));
    }

    private List<String> frontBalls(DltRecord record) {
        return Arrays.asList(
                record.getFront1(),
                record.getFront2(),
                record.getFront3(),
                record.getFront4(),
                record.getFront5()
        );
    }

    private List<String> intersectionBalls(List<String> left, List<String> right) {
        List<String> matchedBalls = new ArrayList<>();
        for (String value : left) {
            if (right.contains(value)) {
                matchedBalls.add(value);
            }
        }
        return matchedBalls;
    }
}
