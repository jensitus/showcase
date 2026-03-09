package org.service_b.workflow.security.repository;

import org.service_b.workflow.security.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    Optional<ApiKey> findByServiceName(String serviceName);

    boolean existsByServiceName(String serviceName);
}
