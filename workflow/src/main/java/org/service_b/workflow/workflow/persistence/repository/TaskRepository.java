package org.service_b.workflow.workflow.persistence.repository;

import org.service_b.workflow.workflow.persistence.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {
    // Simple list with sorting by created date descending
    List<TaskEntity> findByTenantIdOrderByCreatedDesc(String tenantId);

    // Paginated version
    Page<TaskEntity> findByTenantId(String tenantId, Pageable pageable);

    // Paginated with assignee filter
    Page<TaskEntity> findByTenantIdAndAssignee(String tenantId, String assignee, Pageable pageable);

    // Paginated for unassigned tasks
    Page<TaskEntity> findByTenantIdAndAssigneeIsNull(String tenantId, Pageable pageable);

    // Filter by tenant and task state
    List<TaskEntity> findByTenantIdAndTaskStateOrderByCreatedDesc(String tenantId, String taskState);

    // Count tasks for a tenant
    long countByTenantId(String tenantId);

    // find by task id
    Optional<TaskEntity> findByTaskId(String taskId);

    // find by process instance id
    List<TaskEntity> findByProcessInstanceId(String processInstanceId);
}
