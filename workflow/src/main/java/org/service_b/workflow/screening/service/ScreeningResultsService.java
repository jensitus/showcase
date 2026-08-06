package org.service_b.workflow.screening.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.config.NoveltyProperties;
import org.service_b.workflow.screening.dto.BatchDetail;
import org.service_b.workflow.screening.dto.BatchListItem;
import org.service_b.workflow.screening.dto.FlaggedRow;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Reads finished batch results from the work directory for the dashboard API. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningResultsService {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final NoveltyProperties props;
    private final ObjectMapper mapper;

    public List<BatchListItem> listBatches() {
        Path root = Paths.get(props.getWorkDir());
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<BatchListItem> out = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                Path results = dir.resolve("results.json");
                if (!Files.exists(results)) {
                    continue;
                }
                int total = mapper.readValue(results.toFile(), List.class).size();
                int flagged = (int) Math.max(0, countLines(dir.resolve("flagged.csv")) - 1);  // minus header
                out.add(new BatchListItem(dir.getFileName().toString(), total, flagged));
            }
        } catch (IOException e) {
            log.warn("could not list batches: {}", e.getMessage());
        }
        out.sort(Comparator.comparing(BatchListItem::batchId));
        return out;
    }

    public BatchDetail getBatch(String batchId) throws IOException {
        if (!SAFE_ID.matcher(batchId).matches()) {
            throw new IllegalArgumentException("invalid batch id");
        }
        Path dir = Paths.get(props.getWorkDir(), batchId);
        List<Map<String, Object>> reports = mapper.readValue(
                dir.resolve("results.json").toFile(), new TypeReference<>() { });
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Map<String, Object> r : reports) {
            counts.merge(String.valueOf(r.get("verdict")), 1, Integer::sum);
        }
        List<FlaggedRow> rows = readFlagged(dir.resolve("flagged.csv"));
        return new BatchDetail(batchId, reports.size(), rows.size(), counts, rows);
    }

    private List<FlaggedRow> readFlagged(Path csv) throws IOException {
        List<FlaggedRow> rows = new ArrayList<>();
        if (!Files.exists(csv)) {
            return rows;
        }
        List<String> lines = Files.readAllLines(csv);
        for (int i = 1; i < lines.size(); i++) {  // skip header
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> f = parseCsvLine(lines.get(i));
            rows.add(new FlaggedRow(cell(f, 0), cell(f, 1), cell(f, 2), cell(f, 3),
                    cell(f, 4), cell(f, 5), cell(f, 6), cell(f, 7)));
        }
        return rows;
    }

    private static String cell(List<String> f, int i) {
        return i < f.size() ? f.get(i) : "";
    }

    private static long countLines(Path p) throws IOException {
        if (!Files.exists(p)) {
            return 0;
        }
        try (Stream<String> s = Files.lines(p)) {
            return s.count();
        }
    }

    /** Minimal CSV field split with quoted-field support (cells have no embedded newlines). */
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
