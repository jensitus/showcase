package org.service_b.workflow.screening.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScreeningChunkRepository extends JpaRepository<ScreeningChunk, UUID> {

    Optional<ScreeningChunk> findByBatchIdAndChunkNo(String batchId, int chunkNo);

    boolean existsByBatchIdAndChunkNoAndStatus(String batchId, int chunkNo, String status);

    /** Completed chunks of a batch — how many result rows the checkpoints account for. */
    List<ScreeningChunk> findByBatchIdAndStatus(String batchId, String status);
}
