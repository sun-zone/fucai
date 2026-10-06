package com.example.fucai.service;

import com.example.fucai.entity.Kl8Record;
import com.example.fucai.repository.Kl8RecordRepository;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class Kl8CrawlerService {

    private static final String FIRST_EXPECT = "2021313";
    private static final String LATEST_EXPECT = "9999999";
    private static final int BALL_COUNT = 20;

    private final Kl8RecordRepository repository;

    @Value("${fucai.kl8-history-url}")
    private String kl8HistoryUrl;

    @Value("${fucai.kl8-draw-date-url}")
    private String kl8DrawDateUrl;

    public Kl8CrawlerService(Kl8RecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SsqFetchResult syncHistory() throws IOException {
        List<Kl8Record> records = parseHistoryDocument(fetchHistoryDocument());
        fillDrawDates(records);
        return saveRecords(records);
    }

    @Transactional
    public SsqFetchResult fetchAndSaveRecent(int expect) throws IOException {
        List<Kl8Record> records = parseHistoryDocument(fetchHistoryDocument());
        int fromIndex = Math.max(0, records.size() - Math.max(expect, 1));
        List<Kl8Record> recentRecords = records.subList(fromIndex, records.size());
        fillDrawDates(recentRecords);
        return saveRecords(recentRecords);
    }

    public List<Kl8Record> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "expect"));
    }

    public List<RepeatStat> analyzeRepeats(int matchCount) {
        if (matchCount < 2 || matchCount > BALL_COUNT) {
            throw new IllegalArgumentException("快乐8重复数范围必须是 2 到 20");
        }

        List<Kl8Record> records = findAll();
        List<List<String>> ballsList = new ArrayList<>();
        for (Kl8Record record : records) {
            ballsList.add(balls(record));
        }

        List<RepeatStat> stats = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            List<RepeatMatch> matches = new ArrayList<>();
            for (int j = 0; j < records.size(); j++) {
                List<String> matchedBalls = intersectionBalls(ballsList.get(i), ballsList.get(j));
                if (i != j && matchedBalls.size() == matchCount) {
                    Kl8Record matchedRecord = records.get(j);
                    matches.add(new RepeatMatch(
                            matchedRecord.getExpect(),
                            ballsList.get(j),
                            Collections.emptyList(),
                            matchedBalls
                    ));
                }
            }
            Kl8Record record = records.get(i);
            stats.add(new RepeatStat(record.getExpect(), ballsList.get(i), Collections.emptyList(), matches));
        }
        return stats;
    }

    private SsqFetchResult saveRecords(List<Kl8Record> records) {
        int inserted = 0;

        for (Kl8Record record : records) {
            Kl8Record saved = repository.findByExpect(record.getExpect()).orElse(null);
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
        String url = UriComponentsBuilder.fromHttpUrl(kl8HistoryUrl)
                .queryParam("from", FIRST_EXPECT)
                .queryParam("to", LATEST_EXPECT)
                .queryParam("shujcount", 0)
                .queryParam("sort", 0)
                .toUriString();

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .maxBodySize(0)
                .get();
    }

    private List<Kl8Record> parseHistoryDocument(Document document) {
        List<Kl8Record> records = new ArrayList<>();
        for (Element row : document.select("#tdata tr")) {
            Kl8Record record = parseHistoryRow(row);
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    private void fillDrawDates(List<Kl8Record> records) throws IOException {
        if (records.isEmpty()) {
            return;
        }

        Map<String, LocalDate> drawDates = new HashMap<>();
        for (int i = 0; i < records.size(); i += 30) {
            String startExpect = records.get(i).getExpect();
            String endExpect = records.get(Math.min(i + 29, records.size() - 1)).getExpect();
            drawDates.putAll(parseDrawDateDocument(fetchDrawDateDocument(startExpect, endExpect)));
        }
        for (Kl8Record record : records) {
            record.setDrawDate(drawDates.get(record.getExpect()));
        }
    }

    private Document fetchDrawDateDocument(String startExpect, String endExpect) throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl(kl8DrawDateUrl)
                .queryParam("view", "previous")
                .queryParam("start_issue", startExpect)
                .queryParam("end_issue", endExpect)
                .queryParam("limit", 30)
                .toUriString();

        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(30000)
                .maxBodySize(0)
                .ignoreContentType(true)
                .get();
    }

    private Map<String, LocalDate> parseDrawDateDocument(Document document) {
        Map<String, LocalDate> drawDates = new HashMap<>();
        for (Element row : document.select(".lottery-history-table tbody tr")) {
            Elements cells = row.select("td");
            if (cells.size() < 2) {
                continue;
            }
            String expect = cells.get(0).text().trim();
            String drawDateText = cells.get(1).text().trim();
            if (expect.matches("\\d+") && drawDateText.length() >= 10) {
                drawDates.put(expect, LocalDate.parse(drawDateText.substring(0, 10)));
            }
        }
        return drawDates;
    }

    private Kl8Record parseHistoryRow(Element row) {
        Elements cells = row.select("td");
        if (cells.size() < 81) {
            return null;
        }

        String expect = cells.get(0).text().trim();
        if (expect.isEmpty() || !expect.matches("\\d+")) {
            return null;
        }

        List<String> balls = new ArrayList<>();
        for (Element ball : row.select(".chartBall01")) {
            String text = ball.text().trim();
            if (text.matches("\\d{1,2}")) {
                balls.add(normalizeBall(text));
            }
        }
        if (balls.size() != BALL_COUNT) {
            return null;
        }

        Kl8Record record = new Kl8Record();
        record.setExpect(expect);
        setBalls(record, balls);
        return record;
    }

    private void setBalls(Kl8Record record, List<String> balls) {
        record.setNum1(balls.get(0));
        record.setNum2(balls.get(1));
        record.setNum3(balls.get(2));
        record.setNum4(balls.get(3));
        record.setNum5(balls.get(4));
        record.setNum6(balls.get(5));
        record.setNum7(balls.get(6));
        record.setNum8(balls.get(7));
        record.setNum9(balls.get(8));
        record.setNum10(balls.get(9));
        record.setNum11(balls.get(10));
        record.setNum12(balls.get(11));
        record.setNum13(balls.get(12));
        record.setNum14(balls.get(13));
        record.setNum15(balls.get(14));
        record.setNum16(balls.get(15));
        record.setNum17(balls.get(16));
        record.setNum18(balls.get(17));
        record.setNum19(balls.get(18));
        record.setNum20(balls.get(19));
    }

    private void copyRecord(Kl8Record source, Kl8Record target) {
        target.setNum1(source.getNum1());
        target.setNum2(source.getNum2());
        target.setNum3(source.getNum3());
        target.setNum4(source.getNum4());
        target.setNum5(source.getNum5());
        target.setNum6(source.getNum6());
        target.setNum7(source.getNum7());
        target.setNum8(source.getNum8());
        target.setNum9(source.getNum9());
        target.setNum10(source.getNum10());
        target.setNum11(source.getNum11());
        target.setNum12(source.getNum12());
        target.setNum13(source.getNum13());
        target.setNum14(source.getNum14());
        target.setNum15(source.getNum15());
        target.setNum16(source.getNum16());
        target.setNum17(source.getNum17());
        target.setNum18(source.getNum18());
        target.setNum19(source.getNum19());
        target.setNum20(source.getNum20());
        if (source.getDrawDate() != null) {
            target.setDrawDate(source.getDrawDate());
        }
    }

    private String normalizeBall(String text) {
        return String.format("%02d", Integer.parseInt(text.trim()));
    }

    private List<String> balls(Kl8Record record) {
        List<String> balls = new ArrayList<>();
        balls.add(record.getNum1());
        balls.add(record.getNum2());
        balls.add(record.getNum3());
        balls.add(record.getNum4());
        balls.add(record.getNum5());
        balls.add(record.getNum6());
        balls.add(record.getNum7());
        balls.add(record.getNum8());
        balls.add(record.getNum9());
        balls.add(record.getNum10());
        balls.add(record.getNum11());
        balls.add(record.getNum12());
        balls.add(record.getNum13());
        balls.add(record.getNum14());
        balls.add(record.getNum15());
        balls.add(record.getNum16());
        balls.add(record.getNum17());
        balls.add(record.getNum18());
        balls.add(record.getNum19());
        balls.add(record.getNum20());
        return balls;
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
