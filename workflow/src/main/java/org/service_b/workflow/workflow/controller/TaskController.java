package org.service_b.workflow.workflow.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.workflow.dto.CompleteTaskDto;
import org.service_b.workflow.workflow.dto.CreateTaskRequest;
import org.service_b.workflow.workflow.dto.TaskDto;
import org.service_b.workflow.workflow.dto.TaskUpdateRequest;
import org.service_b.workflow.workflow.persistence.entity.TaskEntity;
import org.service_b.workflow.workflow.rest.CibSevenRestClient;
import org.service_b.workflow.workflow.service.TaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final CibSevenRestClient cibSevenRestClient;

    @Value("${showcase.simulate-task-failure:false}")
    private boolean simulateTaskFailure;

    @PostMapping
    public ResponseEntity<TaskDto> createTask(@RequestBody CreateTaskRequest request) {
        try {
            TaskDto createdTask = taskService.createTask(request);
            if (simulateTaskFailure) {
                throw new IllegalStateException("Simulated task creation failure (showcase.simulate-task-failure=true)");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (Exception e) {
            log.error("Failed to create task {} (executionId={}): {}",
                      request.getTaskId(), request.getExecutionId(), e.getMessage(), e);
            if (request.getProcessInstanceId() != null) {
                cibSevenRestClient.createIncident(
                        request.getProcessInstanceId(),
                        "taskCreationFailed",
                        "Workflow service failed to create task: " + e.getMessage()
                );
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{task_id}")
    public ResponseEntity<TaskDto> updateTask(@PathVariable("task_id") String taskId,
                                              @RequestBody TaskUpdateRequest updateRequest) {
        TaskDto updatedTask = taskService.updateTask(taskId, updateRequest);
        return ResponseEntity.ok(updatedTask);
    }

    @PatchMapping("/{task_id}")
    public ResponseEntity<TaskDto> updateTaskPartial(@PathVariable("task_id") String taskId,
                                                     @RequestBody Map<String, Object> updates) {
        TaskDto updatedTask = taskService.updateTaskPartial(taskId, updates);
        return ResponseEntity.ok(updatedTask);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto> getTask(@PathVariable String id) {
        TaskDto task = taskService.getTaskByTaskId(id);
        return ResponseEntity.ok(task);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        List<TaskDto> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("tenant_id/{tenant_id}")
    public ResponseEntity<List<TaskDto>> getTasksByTenant(@PathVariable("tenant_id") @NotBlank String tenantId) {
        List<TaskDto> tasks = taskService.getTasksByTenantId(tenantId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("tenant_id/{tenant_id}/paginated")
    public ResponseEntity<Page<TaskDto>> getTasksByTenantPaginated(@PathVariable("tenant_id") @NotBlank String tenantId,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(defaultValue = "created,desc") String[] sort,
                                                                   @RequestParam(required = false) String assignee) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<TaskDto> tasks = taskService.getTasksByTenantIdPaginated(tenantId, pageable, assignee);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("tenant_id/{tenant_id}/by-state")
    public ResponseEntity<List<TaskEntity>> getTasksByTenantAndState(@PathVariable("tenant_id") @NotBlank String tenantId,
                                                                     @RequestParam @NotBlank String taskState) {
        List<TaskEntity> tasks = taskService.getTasksByTenantIdAndState(tenantId, taskState);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/by-insurance/{insuranceId}")
    public ResponseEntity<List<TaskDto>> getTasksByInsuranceId(@PathVariable UUID insuranceId) {
        return ResponseEntity.ok(taskService.getActiveTasksByInsuranceId(insuranceId));
    }

    @PostMapping("/{task_id}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable("task_id") String taskId, @RequestBody CompleteTaskDto completeTaskDto) {
        taskService.completeTask(taskId, completeTaskDto);
        return null;
    }

}
