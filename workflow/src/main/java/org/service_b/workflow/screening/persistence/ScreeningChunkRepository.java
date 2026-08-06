package org.service_b.workflow.screening.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ScreeningChunkRepository extends JpaRepository<ScreeningChunk, UUID> {

    Optional<ScreeningChunk> findByBatchIdAndChunkNo(String batchId, int chunkNo);

    boolean existsByBatchIdAndChunkNoAndStatus(String batchId, int chunkNo, String status);
}
