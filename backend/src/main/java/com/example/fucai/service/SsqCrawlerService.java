package com.example.fucai.service;

import com.example.fucai.entity.SsqRecord;
import com.example.fucai.repository.SsqRecordRepository;
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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SsqCrawlerService {

    private static final Pattern ONE_OR_TWO_DIGITS = Pattern.compile("\\d{1,2}");
    private static final String FIRST_EXPECT = "03001";
    private static final String LATEST_EXPECT = "99999";

    private final SsqRecordRepository repository;

    @Value("${fucai.ssq-url}")
    private String ssqUrl;

    @Value("${fucai.ssq-history-url}")
    private String ssqHistoryUrl;

    public SsqCrawlerService(SsqRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SsqFetchResult fetchAndSaveRecent(int expect) throws IOException {
        List<SsqRecord> records = parseTrendDocument(fetchTrendDocument(expect));
        return saveRecords(records);
    }

    @Transactional
    public SsqFetchResult syncHistory() throws IOException {
        return saveRecords(parseHistoryDocument(fetchHistoryDocument()));
    }

    private SsqFetchResult saveRecords(List<SsqRecord> records) {
        int inserted = 0;

        for (SsqRecord record : records) {
            SsqRecord saved = repository.findByExpect(record.getExpect()).orElse(null);
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

    public List<SsqRecord> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "expect"));
    }

    public List<RepeatStat> analyzeRepeats(int matchCount) {
        if (matchCount < 2 || matchCount > 6) {
            throw new IllegalArgumentException("双色球重复数范围必须是 2 到 6");
        }

        List<SsqRecord> records = findAll();
        List<List<String>> ballsList = new ArrayList<>();
        for (SsqRecord record : records) {
            ballsList.add(redBalls(record));
        }

        List<RepeatStat> stats = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            List<RepeatMatch> matches = new ArrayList<>();
            for (int j = 0; j < records.size(); j++) {
                List<String> matchedBalls = intersectionBalls(ballsList.get(i), ballsList.get(j));
                if (i != j && matchedBalls.size() == matchCount) {
                    SsqRecord matchedRecord = records.get(j);
                    matches.add(new RepeatMatch(
                            matchedRecord.getExpect(),
                            ballsList.get(j),
                            Arrays.asList(matchedRecord.getBlue()),
                            matchedBalls
                    ));
                }
            }
            SsqRecord record = records.get(i);
            stats.add(new RepeatStat(record.getExpect(), ballsList.get(i), Arrays.asList(record.getBlue()), matches));
        }
        return stats;
    }

    private Document fetchTrendDocument(int expect) throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl(ssqUrl)
                .queryParam("expect", expect)
                .toUriString();

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(20000)
                .get();
    }

    private List<SsqRecord> parseTrendDocument(Document document) {
        Elements rows = document.select("#tdata tr");
        if (rows.isEmpty()) {
            rows = document.select("tbody tr");
        }

        List<SsqRecord> records = new ArrayList<>();
        for (Element row : rows) {
            SsqRecord record = parseRow(row);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private Document fetchHistoryDocument() throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl(ssqHistoryUrl)
                .queryParam("start", FIRST_EXPECT)
                .queryParam("end", LATEST_EXPECT)
                .toUriString();

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .maxBodySize(0)
                .get();
    }

    private List<SsqRecord> parseHistoryDocument(Document document) {
        List<SsqRecord> records = new ArrayList<>();
        for (Element row : document.select("#tdata tr")) {
            SsqRecord record = parseHistoryRow(row);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private SsqRecord parseHistoryRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 8) {
            return null;
        }

        String expect = cells.get(0).text().trim();
        if (expect.isEmpty() || !expect.matches("\\d+")) {
            return null;
        }

        SsqRecord record = new SsqRecord();
        record.setExpect(expect);
        record.setRed1(normalizeBall(cells.get(1).text()));
        record.setRed2(normalizeBall(cells.get(2).text()));
        record.setRed3(normalizeBall(cells.get(3).text()));
        record.setRed4(normalizeBall(cells.get(4).text()));
        record.setRed5(normalizeBall(cells.get(5).text()));
        record.setRed6(normalizeBall(cells.get(6).text()));
        record.setBlue(normalizeBall(cells.get(7).text()));
        record.setDrawDate(parseDrawDate(cells.get(cells.size() - 1).text()));
        return record;
    }

    private SsqRecord parseRow(Element row) {
        Elements cells = row.select("td");
        if (cells.isEmpty()) {
            return null;
        }

        String expect = cells.get(0).text().trim();
        if (expect.isEmpty() || !expect.matches("\\d+")) {
            return null;
        }

        List<String> reds = new ArrayList<>();
        for (Element red : row.select(".chartBall01")) {
            String text = red.text().trim();
            if (ONE_OR_TWO_DIGITS.matcher(text).matches()) {
                reds.add(normalizeBall(text));
            }
        }

        List<String> blues = new ArrayList<>();
        for (Element blue : row.select(".chartBall02")) {
            String text = blue.text().trim();
            if (ONE_OR_TWO_DIGITS.matcher(text).matches()) {
                blues.add(normalizeBall(text));
            }
        }

        if (reds.size() < 6 || blues.isEmpty()) {
            return null;
        }

        SsqRecord record = new SsqRecord();
        record.setExpect(expect);
        record.setRed1(reds.get(0));
        record.setRed2(reds.get(1));
        record.setRed3(reds.get(2));
        record.setRed4(reds.get(3));
        record.setRed5(reds.get(4));
        record.setRed6(reds.get(5));
        record.setBlue(blues.get(0));
        return record;
    }

    private void copyRecord(SsqRecord source, SsqRecord target) {
        target.setRed1(source.getRed1());
        target.setRed2(source.getRed2());
        target.setRed3(source.getRed3());
        target.setRed4(source.getRed4());
        target.setRed5(source.getRed5());
        target.setRed6(source.getRed6());
        target.setBlue(source.getBlue());
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

    private List<String> redBalls(SsqRecord record) {
        return Arrays.asList(
                record.getRed1(),
                record.getRed2(),
                record.getRed3(),
                record.getRed4(),
                record.getRed5(),
                record.getRed6()
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
