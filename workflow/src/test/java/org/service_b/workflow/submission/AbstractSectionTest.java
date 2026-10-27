package org.service_b.workflow.submission;

import org.junit.jupiter.api.Test;
import org.service_b.workflow.submission.dto.SectionDto;
import org.service_b.workflow.submission.persistence.AbstractSection;
import org.service_b.workflow.submission.persistence.SubmissionSection;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractSectionTest {

    private static SectionDto s(String shortname, String content) {
        return new SectionDto(shortname, null, content);
    }

    private static List<SectionDto> required() {
        return new ArrayList<>(List.of(
                s("CONCLUSION", "It works."),          // out of order on purpose
                s("PURPOSE", "To test it."),
                s("METHODS", "We tested it."),
                s("RESULTS", "It passed.")));
    }

    @Test
    void keepsCatalogueOrderAndDropsBlankOptionalSections() {
        List<SectionDto> in = required();
        in.add(s("LIMITATIONS", "   "));
        in.add(s("FUNDING", " None. "));

        List<SubmissionSection> out = AbstractSection.toEntities(in);

        assertThat(out).extracting(SubmissionSection::getShortname)
                .containsExactly("PURPOSE", "METHODS", "RESULTS", "CONCLUSION", "FUNDING");
        assertThat(out).extracting(SubmissionSection::getOrderNr).containsExactly(10, 20, 30, 40, 60);
        assertThat(out.get(4).getContent()).isEqualTo("None.");       // stripped
        assertThat(out.get(0).getName()).isEqualTo("Purpose or Learning Objective");
    }

    @Test
    void flattensWithTheLabelsTheScreeningSplitsBy() {
        String text = AbstractSection.flatten(AbstractSection.toEntities(required()));

        assertThat(text).isEqualTo("""
                Purpose or Learning Objective: To test it.

                Methods or Background: We tested it.

                Results or Findings: It passed.

                Conclusion: It works.""");
    }

    @Test
    void rejectsAMissingRequiredSection() {
        List<SectionDto> in = required();
        in.removeIf(x -> x.getShortname().equals("RESULTS"));
        assertThatThrownBy(() -> AbstractSection.toEntities(in))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Results or Findings");
    }

    @Test
    void rejectsUnknownAndRepeatedSections() {
        List<SectionDto> unknown = required();
        unknown.add(s("BACKGROUND", "x"));
        assertThatThrownBy(() -> AbstractSection.toEntities(unknown)).isInstanceOf(IllegalArgumentException.class);

        List<SectionDto> twice = required();
        twice.add(s("PURPOSE", "again"));
        assertThatThrownBy(() -> AbstractSection.toEntities(twice)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesTheSectionAndTotalLimits() {
        List<SectionDto> longSection = required();
        longSection.set(1, s("PURPOSE", "x".repeat(AbstractSection.MAX_REQUIRED + 1)));
        assertThatThrownBy(() -> AbstractSection.toEntities(longSection)).hasMessageContaining("at most 2500");

        List<SectionDto> longOptional = required();
        longOptional.add(s("ETHICS", "x".repeat(AbstractSection.MAX_OPTIONAL + 1)));
        assertThatThrownBy(() -> AbstractSection.toEntities(longOptional)).hasMessageContaining("at most 1500");

        // every section within its limit, the sum over the total
        List<SectionDto> longTotal = new ArrayList<>(List.of(
                s("PURPOSE", "x".repeat(2000)), s("METHODS", "x".repeat(2000)),
                s("RESULTS", "x".repeat(2000)), s("CONCLUSION", "x".repeat(1001))));
        assertThatThrownBy(() -> AbstractSection.toEntities(longTotal)).hasMessageContaining("at most 7000");

        // the largest abstract allowed still fits abstract_text (8,192) with its labels
        List<SectionDto> maximal = new ArrayList<>(List.of(
                s("PURPOSE", "x".repeat(2000)), s("METHODS", "x".repeat(2000)),
                s("RESULTS", "x".repeat(2000)), s("CONCLUSION", "x".repeat(1000))));
        assertThat(AbstractSection.flatten(AbstractSection.toEntities(maximal)).length()).isLessThan(8192);
    }
}
