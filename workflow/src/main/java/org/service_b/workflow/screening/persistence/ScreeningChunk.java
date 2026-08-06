package org.service_b.workflow.screening.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Checkpoint for one screened chunk of a batch — lets a long run resume after a restart. */
@Entity
@Table(name = "screening_chunk")
@Getter
@Setter
public class ScreeningChunk {

    public static final String DONE = "done";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "chunk_no", nullable = false)
    private int chunkNo;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "screened_count")
    private int screenedCount;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
