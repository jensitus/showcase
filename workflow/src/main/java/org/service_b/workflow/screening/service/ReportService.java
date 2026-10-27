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
 *   flagged.csv  — the flagged rows, most suspicious first, with the submission title
 *                  and matched prior abstract joined in: possible_overlap /
 *                  likely_duplicate from screening, plus — when the batch ran the 35B
 *                  step — the abstracts it found "nothing new" or "only partly new".
 * "Triage evidence for reviewers, never an accept/reject."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ObjectMapper mapper;

    private static final List<String> FLAG = List.of("possible_overlap", "likely_duplicate");
    /** The 35B step's answers that count as flagged (decided: "partly" counts too). */
    private static final List<String> LLM_FLAG = List.of("no", "partly");
    private static final Map<String, Integer> SEVERITY =
            Map.of("likely_duplicate", 0, "possible_overlap", 1, "llm:no", 2, "llm:partly", 3);
    private static final List<String> CSV_HEADER = List.of(
            "submission_id", "verdict", "novelty_score", "title",
            "matched_prior_title", "matched_year", "similarity", "rationale",
            "llm_adds_new", "llm_says");

    /** As {@link #build(Path, Path, Path, Path)} for a batch without the 35B step. */
    public BatchSummary build(Path resultsJsonl, Path submissionsJsonl, Path outDir) throws IOException {
        return build(resultsJsonl, submissionsJsonl, null, outDir);
    }

    /**
     * @param resultsJsonl     the accumulated NoveltyReports (one JSON per line)
     * @param submissionsJsonl the screened submissions (to join titles by id)
     * @param answersJsonl     the 35B step's answers (ask_new.jsonl), or null / missing when
     *                         the batch ran without it. Merged into each report as
     *                         {@code novelty_q}; "nothing new" and "only partly new" are flagged.
     * @param outDir           where results.json + flagged.csv are written
     */
    public BatchSummary build(Path resultsJsonl, Path submissionsJsonl, Path answersJsonl, Path outDir)
            throws IOException {
        List<Map<String, Object>> reports = readJsonlObjects(resultsJsonl);
        Map<String, String> titleById = readTitles(submissionsJsonl);
        Map<String, Map<String, Object>> answerById = new LinkedHashMap<>();
        for (Map<String, Object> a : answersJsonl == null ? List.<Map<String, Object>>of() : readJsonlObjects(answersJsonl)) {
            if (a.get("adds_new") != null) {
                answerById.put(String.valueOf(a.get("key")), a);
            }
        }
        for (Map<String, Object> r : reports) {
            Map<String, Object> a = answerById.get(String.valueOf(r.get("submission_id")));
            if (a != null) {
                r.put("novelty_q", a);
            }
        }
        Files.createDirectories(outDir);

        // results.json — the full machine-readable set
        mapper.writerWithDefaultPrettyPrinter().writeValue(outDir.resolve("results.json").toFile(), reports);

        // flagged.csv — most suspicious first: screening's flags, then the 35B's
        List<Map<String, Object>> flagged = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> r : reports) {
            String v = String.valueOf(r.get("verdict"));
            counts.merge(v, 1, Integer::sum);
            if (FLAG.contains(v) || LLM_FLAG.contains(llmAnswer(r))) {
                flagged.add(r);
            }
        }
        flagged.sort(Comparator
                .comparingInt((Map<String, Object> r) -> SEVERITY.getOrDefault(severityKey(r), 9))
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
                    String.valueOf(r.getOrDefault("rationale", "")).replace('\n', ' '),
                    llmAnswer(r),
                    String.valueOf(llm(r).getOrDefault("adds_what", "")).replace('\n', ' '));
            csv.append(row.stream().map(ReportService::csvCell).reduce((a, b) -> a + "," + b).orElse("")).append('\n');
        }
        Files.writeString(outDir.resolve("flagged.csv"), csv.toString());

        log.info("report: {} reports, {} flagged -> {}", reports.size(), flagged.size(), outDir);
        return new BatchSummary(reports.size(), flagged.size(), counts);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> llm(Map<String, Object> report) {
        return report.get("novelty_q") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    /** The 35B's adds_new answer (yes / partly / no), or "" when it was not asked. */
    private static String llmAnswer(Map<String, Object> report) {
        return String.valueOf(llm(report).getOrDefault("adds_new", ""));
    }

    /** A screening flag ranks by its verdict; a row flagged only by the 35B by its answer. */
    private static String severityKey(Map<String, Object> report) {
        String v = String.valueOf(report.get("verdict"));
        return FLAG.contains(v) ? v : "llm:" + llmAnswer(report);
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
