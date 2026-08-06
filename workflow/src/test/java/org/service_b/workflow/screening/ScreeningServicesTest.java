package org.service_b.workflow.screening;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.service_b.workflow.screening.dto.BatchSummary;
import org.service_b.workflow.screening.dto.Submission;
import org.service_b.workflow.screening.service.IngestService;
import org.service_b.workflow.screening.service.ReportService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit tests for ingest + report (no Spring context, no external deps). */
class ScreeningServicesTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final IngestService ingest = new IngestService(mapper);
    private final ReportService report = new ReportService(mapper);

    @Test
    void ingestReadsJsonlAndCsv(@TempDir Path dir) throws Exception {
        Path jsonl = dir.resolve("subs.jsonl");
        Files.writeString(jsonl,
                "{\"id\":\"a1\",\"title\":\"T1\",\"abstract\":\"body one\",\"year\":2020}\n" +
                "{\"id\":\"a2\",\"title\":\"T2\",\"abstract\":\"body two\"}\n");
        List<Submission> fromJsonl = ingest.read(jsonl);
        assertEquals(2, fromJsonl.size());
        assertEquals("a1", fromJsonl.get(0).getId());
        assertEquals("body one", fromJsonl.get(0).getAbstractText());  // maps the "abstract" key

        Path csv = dir.resolve("subs.csv");
        Files.writeString(csv,
                "id,title,abstract\n" +
                "c1,\"Title, with comma\",\"an abstract\"\n" +
                "c2,Plain,another\n");
        List<Submission> fromCsv = ingest.read(csv);
        assertEquals(2, fromCsv.size());
        assertEquals("Title, with comma", fromCsv.get(0).getTitle());  // quoted field preserved

        // round-trip write
        Path out = dir.resolve("nested/written.jsonl");
        ingest.writeJsonl(fromJsonl, out);
        assertEquals(2, Files.readAllLines(out).size());
    }

    @Test
    void reportWritesResultsAndFlaggedCsv(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("submissions.jsonl"),
                "{\"id\":\"s1\",\"title\":\"Dup title\",\"abstract\":\"x\"}\n" +
                "{\"id\":\"s2\",\"title\":\"Overlap title\",\"abstract\":\"y\"}\n" +
                "{\"id\":\"s3\",\"title\":\"Novel title\",\"abstract\":\"z\"}\n");
        Files.writeString(dir.resolve("results.jsonl"),
                "{\"submission_id\":\"s3\",\"verdict\":\"likely_novel\",\"novelty_score\":0.9,\"rationale\":\"r3\",\"top_matches\":[]}\n" +
                "{\"submission_id\":\"s1\",\"verdict\":\"likely_duplicate\",\"novelty_score\":0.1,\"rationale\":\"r1\",\"top_matches\":[{\"title\":\"Prior A\",\"year\":2019,\"similarity\":0.95}]}\n" +
                "{\"submission_id\":\"s2\",\"verdict\":\"possible_overlap\",\"novelty_score\":0.4,\"rationale\":\"r2\",\"top_matches\":[{\"title\":\"Prior B\",\"year\":2021,\"similarity\":0.83}]}\n");

        BatchSummary summary = report.build(dir.resolve("results.jsonl"), dir.resolve("submissions.jsonl"), dir);

        assertEquals(3, summary.getTotal());
        assertEquals(2, summary.getFlagged());
        assertTrue(Files.exists(dir.resolve("results.json")));

        List<String> csv = Files.readAllLines(dir.resolve("flagged.csv"));
        assertEquals(3, csv.size());  // header + 2 flagged
        assertTrue(csv.get(0).startsWith("submission_id,verdict"));
        // most-suspicious first: likely_duplicate (s1) before possible_overlap (s2)
        assertTrue(csv.get(1).startsWith("s1,likely_duplicate"));
        assertTrue(csv.get(1).contains("Dup title"));      // joined submission title
        assertTrue(csv.get(1).contains("Prior A"));        // matched prior title
        assertTrue(csv.get(2).startsWith("s2,possible_overlap"));
    }
}
