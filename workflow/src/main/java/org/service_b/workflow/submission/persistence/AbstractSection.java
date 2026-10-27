package org.service_b.workflow.submission.persistence;

import org.service_b.workflow.submission.dto.SectionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The sections of an abstract, in the order a congress abstract has them. One form serves
 * scientific studies and educational posters, hence the "or" labels.
 *
 * <p>Limits are generous on purpose: in two congresses' exports (~20,000 abstracts) the
 * longest main section had 2,056 characters and the longest abstract 5,923 in total.
 * The total also keeps the flattened {@code abstract_text} (labels included) below its
 * 8,192-character column.
 */
public enum AbstractSection {

    PURPOSE("Purpose or Learning Objective", true),
    METHODS("Methods or Background", true),
    RESULTS("Results or Findings", true),
    CONCLUSION("Conclusion", true),
    LIMITATIONS("Limitations", false),
    FUNDING("Funding for this study", false),
    ETHICS("Ethics committee - additional information", false);

    public static final int MAX_REQUIRED = 2500;
    public static final int MAX_OPTIONAL = 1500;
    public static final int MAX_TOTAL = 7000;

    private final String label;
    private final boolean required;

    AbstractSection(String label, boolean required) {
        this.label = label;
        this.required = required;
    }

    public String label() { return label; }
    public boolean required() { return required; }
    public int maxLength() { return required ? MAX_REQUIRED : MAX_OPTIONAL; }
    /** 10, 20, ... — gaps leave room for a section added in between. */
    public int orderNr() { return (ordinal() + 1) * 10; }

    /**
     * Validate the submitted sections and turn them into entities, in catalogue order.
     * Blank optional sections are dropped.
     *
     * @throws IllegalArgumentException (HTTP 400) on an unknown or repeated section, a
     *         missing required one, or a section or total over its limit
     */
    public static List<SubmissionSection> toEntities(List<SectionDto> input) {
        Map<AbstractSection, String> byKey = new EnumMap<>(AbstractSection.class);
        for (SectionDto s : input) {
            AbstractSection key = Arrays.stream(values())
                    .filter(v -> v.name().equals(s.getShortname()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("unknown abstract section: " + s.getShortname()));
            String text = s.getContent() == null ? "" : s.getContent().strip();
            if (byKey.putIfAbsent(key, text) != null) {
                throw new IllegalArgumentException("abstract section given twice: " + key.label);
            }
        }
        int total = 0;
        List<SubmissionSection> out = new ArrayList<>();
        for (AbstractSection key : values()) {
            String text = byKey.getOrDefault(key, "");
            if (text.isEmpty()) {
                if (key.required) {
                    throw new IllegalArgumentException("abstract section is required: " + key.label);
                }
                continue;
            }
            if (text.length() > key.maxLength()) {
                throw new IllegalArgumentException(key.label + " has " + text.length()
                        + " characters, at most " + key.maxLength() + " allowed");
            }
            total += text.length();
            SubmissionSection section = new SubmissionSection();
            section.setShortname(key.name());
            section.setName(key.label);
            section.setOrderNr(key.orderNr());
            section.setContent(text);
            out.add(section);
        }
        if (total > MAX_TOTAL) {
            throw new IllegalArgumentException("the abstract has " + total + " characters, at most "
                    + MAX_TOTAL + " allowed");
        }
        return out;
    }

    /**
     * The sections as one text, "Label: content" per section. This is what lands in
     * {@code abstract_text} — the submission list and the novelty screening read it, and
     * the screening splits it by exactly these labels again to judge by section.
     */
    public static String flatten(List<SubmissionSection> sections) {
        return sections.stream()
                .map(s -> s.getName() + ": " + s.getContent())
                .collect(Collectors.joining("\n\n"));
    }
}
