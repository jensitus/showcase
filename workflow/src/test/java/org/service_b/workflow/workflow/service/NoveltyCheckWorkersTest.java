package org.service_b.workflow.workflow.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoveltyCheckWorkersTest {

    @Test
    void retriesThreeTimesThenGivesUp() {
        // first attempt: the engine reports no retries yet
        assertThat(NoveltyCheckWorkers.retriesLeft(null)).isEqualTo(3);
        // after each failure the engine hands back what we set
        assertThat(NoveltyCheckWorkers.retriesLeft("3")).isEqualTo(2);
        assertThat(NoveltyCheckWorkers.retriesLeft("2")).isEqualTo(1);
        assertThat(NoveltyCheckWorkers.retriesLeft("1")).isZero();   // -> BPMN error PIPELINE_UNAVAILABLE
    }
}
