package org.service_b.workflow.screening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.dto.BatchSummary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the committee-facing outputs for a finished batch (Java port of the
 * repo's flagged_report.py):
 *   results.json — every NoveltyReport, machine-readable
 *   flagged.csv  — the flagged (possible_overlap / likely_duplicate) rows, most
 *                  suspicious first, with the submission title and matched prior
 *                  abstract joined in.
 * "Triage evidence for reviewers, never an accept/reject."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ObjectMapper mapper;

    private static final List<String> FLAG = List.of("possible_overlap", "likely_duplicate");
    private static final Map<String, Integer> SEVERITY =
            Map.of("likely_duplicate", 0, "possible_overlap", 1, "likely_novel", 2);
    private static final List<String> CSV_HEADER = List.of(
            "submission_id", "verdict", "novelty_score", "title",
            "matched_prior_title", "matched_year", "similarity", "rationale");

    /**
     * @param resultsJsonl     the accumulated NoveltyReports (one JSON per line)
     * @param submissionsJsonl the screened submissions (to join titles by id)
     * @param outDir           where results.json + flagged.csv are written
     */
    public BatchSummary build(Path resultsJsonl, Path submissionsJsonl, Path outDir) throws IOException {
        List<Map<String, Object>> reports = readJsonlObjects(resultsJsonl);
        Map<String, String> titleById = readTitles(submissionsJsonl);
        Files.createDirectories(outDir);

        // results.json — the full machine-readable set
        mapper.writerWithDefaultPrettyPrinter().writeValue(outDir.resolve("results.json").toFile(), reports);

        // flagged.csv — most suspicious first
        List<Map<String, Object>> flagged = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> r : reports) {
            String v = String.valueOf(r.get("verdict"));
            counts.merge(v, 1, Integer::sum);
            if (FLAG.contains(v)) {
                flagged.add(r);
            }
        }
        flagged.sort(Comparator
                .comparingInt((Map<String, Object> r) -> SEVERITY.getOrDefault(String.valueOf(r.get("verdict")), 9))
                .thenComparing(r -> -toDouble(r.get("novelty_score"))));

        StringBuilder csv = new StringBuilder(String.join(",", CSV_HEADER)).append('\n');
        for (Map<String, Object> r : flagged) {
            String id = String.valueOf(r.getOrDefault("submission_id", ""));
            Map<String, Object> top = topMatch(r);
            List<String> row = List.of(
                    id,
                    String.valueOf(r.getOrDefault("verdict", "")),
                    String.valueOf(r.getOrDefault("novelty_score", "")),
                    titleById.getOrDefault(id, ""),
                    String.valueOf(top.getOrDefault("title", "")),
                    String.valueOf(top.getOrDefault("year", "")),
                    String.valueOf(top.getOrDefault("similarity", "")),
                    String.valueOf(r.getOrDefault("rationale", "")).replace('\n', ' '));
            csv.append(row.stream().map(ReportService::csvCell).reduce((a, b) -> a + "," + b).orElse("")).append('\n');
        }
        Files.writeString(outDir.resolve("flagged.csv"), csv.toString());

        log.info("report: {} reports, {} flagged -> {}", reports.size(), flagged.size(), outDir);
        return new BatchSummary(reports.size(), flagged.size(), counts);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> topMatch(Map<String, Object> report) {
        Object tm = report.get("top_matches");
        if (tm instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map) {
            return (Map<String, Object>) list.get(0);
        }
        return Map.of();
    }

    private List<Map<String, Object>> readJsonlObjects(Path path) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.exists(path)) {
            return out;
        }
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                out.add(mapper.readValue(line, Map.class));
            }
        }
        return out;
    }

    private Map<String, String> readTitles(Path submissionsJsonl) throws IOException {
        Map<String, String> byId = new LinkedHashMap<>();
        if (submissionsJsonl == null || !Files.exists(submissionsJsonl)) {
            return byId;
        }
        for (String line : Files.readAllLines(submissionsJsonl)) {
            if (line.isBlank()) {
                continue;
            }
            Map<String, Object> s = mapper.readValue(line, Map.class);
            if (s.get("id") != null) {
                byId.put(String.valueOf(s.get("id")), String.valueOf(s.getOrDefault("title", "")));
            }
        }
        return byId;
    }

    private static double toDouble(Object o) {
        try {
            return o == null ? 0 : Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Quote a CSV cell if it contains a comma, quote or newline. */
    private static String csvCell(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
