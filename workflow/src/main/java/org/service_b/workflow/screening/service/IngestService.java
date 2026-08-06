package org.service_b.workflow.screening.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.screening.dto.Submission;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a submissions export into a normalised {@code List<Submission>} and can
 * write it back as JSONL for the screen step. Supports the formats the pipeline
 * itself uses: {@code .jsonl} (one JSON object per line), {@code .json} (an array),
 * and {@code .csv} (id/title/abstract columns; id optional). A raw congress export
 * is expected to be pre-flattened by convert.py into one of these first.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {

    private final ObjectMapper mapper;

    public List<Submission> read(Path path) throws IOException {
        String name = path.getFileName().toString().toLowerCase();
        List<Submission> subs;
        if (name.endsWith(".csv")) {
            subs = readCsv(path);
        } else if (name.endsWith(".json")) {
            subs = mapper.readValue(path.toFile(),
                    mapper.getTypeFactory().constructCollectionType(List.class, Submission.class));
        } else {
            subs = readJsonl(path);
        }
        log.info("ingested {} submissions from {}", subs.size(), path);
        return subs;
    }

    /** Write the normalised submissions as JSONL for the screen step. */
    public void writeJsonl(List<Submission> submissions, Path out) throws IOException {
        Files.createDirectories(out.getParent());
        StringBuilder sb = new StringBuilder();
        for (Submission s : submissions) {
            sb.append(mapper.writeValueAsString(s)).append('\n');
        }
        Files.writeString(out, sb.toString());
    }

    private List<Submission> readJsonl(Path path) throws IOException {
        List<Submission> out = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                out.add(mapper.readValue(line, Submission.class));
            }
        }
        return out;
    }

    private List<Submission> readCsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        List<Submission> out = new ArrayList<>();
        if (lines.isEmpty()) {
            return out;
        }
        List<String> header = parseCsvLine(lines.get(0));
        int iId = indexOfIgnoreCase(header, "id");
        int iTitle = indexOfIgnoreCase(header, "title");
        int iAbstract = indexOfIgnoreCase(header, "abstract");
        for (int r = 1; r < lines.size(); r++) {
            if (lines.get(r).isBlank()) {
                continue;
            }
            List<String> f = parseCsvLine(lines.get(r));
            String id = iId >= 0 && iId < f.size() ? f.get(iId).trim() : "sub-" + r;
            out.add(new Submission(
                    id.isEmpty() ? "sub-" + r : id,
                    iTitle >= 0 && iTitle < f.size() ? f.get(iTitle).trim() : "",
                    iAbstract >= 0 && iAbstract < f.size() ? f.get(iAbstract).trim() : ""));
        }
        return out;
    }

    private static int indexOfIgnoreCase(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).trim().equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }

    /** Minimal CSV field split with quoted-field support (no embedded newlines). */
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
