package org.service_b.workflow.workflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.service_b.workflow.sse.EventService;
import org.service_b.workflow.workflow.dto.*;
import org.service_b.workflow.workflow.exception.TaskNotFoundException;
import org.service_b.workflow.workflow.mapper.ProcessMapper;
import org.service_b.workflow.workflow.mapper.TaskMapper;
import org.service_b.workflow.workflow.mapper.VariableMapper;
import org.service_b.workflow.workflow.persistence.entity.TaskEntity;
import org.service_b.workflow.workflow.persistence.entity.VariableEntity;
import org.service_b.workflow.workflow.persistence.repository.TaskRepository;
import org.service_b.workflow.workflow.persistence.repository.VariableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProcessService processService;
    private final ProcessMapper processMapper;
    private final VariableRepository variableRepository;
    private final InsuranceWorkflowService insuranceWorkflowService;
    private final RestClientService restClientService;
    private final EventService eventService;

    @Transactional
    public TaskDto createTask(CreateTaskRequest request) {
        TaskEntity entity = taskMapper.toEntity(request);
        ProcessDto processDto = processService.getProcessByProcessInstanceId(request.getProcessInstanceId());

        updateOrCreateVariables(entity, request.getVariables(), processDto);

        TaskEntity savedEntity = taskRepository.save(entity);
        TaskDto taskDto = taskMapper.toDto(savedEntity);

        processService.findInsuranceByProcessInstanceId(request.getProcessInstanceId())
                      .ifPresent(insurance -> eventService.sendEvent(insurance, "insurance"));

        TaskDto enrichedTaskDto = enrichTaskWithWorkflowData(taskDto, request);

        if (enrichedTaskDto != null) {
            taskMapper.updateEntityFromEnrichedDto(enrichedTaskDto, savedEntity);
            return taskMapper.toDto(taskRepository.save(savedEntity));
        }

        return taskDto;
    }

    @Transactional
    public TaskDto updateTask(String task_id, TaskUpdateRequest updateRequest) {
        log.debug("Updating task with id: {}", task_id);

        TaskEntity entity = taskRepository.findByTaskId(task_id)
                                          .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + task_id));

        taskMapper.updateEntityFromDto(updateRequest, entity);

        TaskEntity savedEntity = taskRepository.save(entity);
        log.debug("Task updated successfully: {}", savedEntity.getId());

        return taskMapper.toDto(savedEntity);
    }

    @Transactional
    public TaskDto updateTaskPartial(String taskId, Map<String, Object> updates) {
        log.debug("Partially updating task with taskId: {}", taskId);

        TaskEntity entity = taskRepository.findByTaskId(taskId)
                                          .orElseThrow(() -> new TaskNotFoundException("Task not found with taskId: " + taskId));

        updates.forEach((key, value) -> {
            switch (key) {
                case "name" -> entity.setName((String) value);
                case "assignee" -> entity.setAssignee((String) value);
                case "taskState" -> entity.setTaskState((String) value);
                case "formKey" -> entity.setFormKey((String) value);
                default -> log.warn("Ignoring unknown field: {}", key);
            }
        });

        TaskEntity savedEntity = taskRepository.save(entity);
        log.debug("Task partially updated successfully: {}", taskId);

        return taskMapper.toDto(savedEntity);
    }

    public TaskDto getTaskByTaskId(String taskId) {
        TaskEntity entity = taskRepository.findByTaskId(taskId)
                                          .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        return taskMapper.toDto(entity);
    }

    public List<TaskDto> getAllTasks() {
        return taskRepository.findAll().stream()
                             .map(taskMapper::toDto)
                             .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByTenantId(String tenantId) {
        validateTenantId(tenantId);
        log.debug("Fetching tasks for tenant: {}", tenantId);
        List<TaskEntity> byTenantIdOrderByCreatedDesc = taskRepository.findByTenantIdOrderByCreatedDesc(tenantId);
        return taskMapper.toDto(byTenantIdOrderByCreatedDesc);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByTenantIdPaginated(String tenantId, Pageable pageable) {
        return getTasksByTenantIdPaginated(tenantId, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<TaskDto> getTasksByTenantIdPaginated(String tenantId, Pageable pageable, String assignee) {
        validateTenantId(tenantId);
        log.debug("Fetching paginated tasks for tenant: {} with page: {} and assignee: {}", tenantId, pageable, assignee);

        Page<TaskEntity> taskEntities;
        if (assignee == null) {
            // No filter - return all tasks
            taskEntities = taskRepository.findByTenantId(tenantId, pageable);
        } else if (assignee.isEmpty()) {
            // Empty string means unassigned tasks
            taskEntities = taskRepository.findByTenantIdAndAssigneeIsNull(tenantId, pageable);
        } else {
            // Filter by specific assignee
            taskEntities = taskRepository.findByTenantIdAndAssignee(tenantId, assignee, pageable);
        }

        return taskEntities.map(taskMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getActiveTasksByInsuranceId(UUID insuranceId) {
        return processService.findProcessInstanceIdByInsuranceId(insuranceId)
                             .map(processInstanceId -> taskRepository.findByProcessInstanceId(processInstanceId)
                                                                     .stream()
                                                                     .filter(t -> !"COMPLETED".equals(t.getTaskState())
                                                                               && !"CANCELLED".equals(t.getTaskState()))
                                                                     .map(taskMapper::toDto)
                                                                     .collect(Collectors.toList()))
                             .orElse(List.of());
    }

    @Transactional
    public void cancelTasksByProcessInstanceId(String processInstanceId) {
        taskRepository.findByProcessInstanceId(processInstanceId).stream()
                      .filter(t -> !"COMPLETED".equals(t.getTaskState()) && !"CANCELLED".equals(t.getTaskState()))
                      .forEach(t -> {
                          t.setTaskState("CANCELLED");
                          taskRepository.save(t);
                          log.info("Cancelled task {} for process instance {}", t.getTaskId(), processInstanceId);
                      });
    }

    @Transactional(readOnly = true)
    public List<TaskEntity> getTasksByTenantIdAndState(String tenantId, String taskState) {
        validateTenantId(tenantId);
        log.debug("Fetching tasks for tenant: {} with state: {}", tenantId, taskState);
        return taskRepository.findByTenantIdAndTaskStateOrderByCreatedDesc(tenantId, taskState);
    }

    @Transactional
    public void completeTask(String taskId, CompleteTaskDto completeTaskDto) {
        TaskEntity taskEntity = taskRepository.findByTaskId(taskId).orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + taskId));
        taskEntity.setTaskState("COMPLETED");

        restClientService.completeUserTask(taskId, completeTaskDto.getCompleteVars());
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
    }

    private VariableEntity createVariable(TaskEntity taskEntity, Map.Entry<String, Object> entry, ProcessDto processDto) {
        VariableEntity variableEntity = new VariableEntity();
        variableEntity.setProcessInstanceId(taskEntity.getProcessInstanceId());
        variableEntity.setProcessId(processDto.getId());
        variableEntity.setVariableName(entry.getKey());
        if (entry.getValue() == null) {
            variableEntity.setVariableValue(null);
        } else {
            variableEntity.setVariableValue(entry.getValue().toString());
            variableEntity.setVariableType(entry.getValue().getClass().getSimpleName());
        }

        variableEntity.setProcess(processMapper.toEntity(processDto));
        return variableRepository.save(variableEntity);
    }

    private void updateOrCreateVariables(TaskEntity entity,
                                         Map<String, Object> variables,
                                         ProcessDto processDto) {
        variables.forEach((key, value) -> {
            log.debug("Processing variable - Key: {}, Value: {}", key, value);

            VariableEntity variable = variableRepository.findByProcessInstanceIdAndVariableNameAndProcessId(
                    entity.getProcessInstanceId(),
                    key,
                    processDto.getId()
            );

            if (variable != null) {
                updateExistingVariable(variable, value);
            } else {
                createNewVariable(entity, key, value, processDto);
            }
        });
    }

    private void updateExistingVariable(VariableEntity variable, Object value) {
        variable.setVariableValue(value != null ? value.toString() : null);
        variableRepository.save(variable);
    }

    private void createNewVariable(TaskEntity entity, String key, Object value, ProcessDto processDto) {
        VariableEntity variableEntity = VariableEntity.builder()
                                                      .processInstanceId(entity.getProcessInstanceId())
                                                      .variableName(key)
                                                      .variableValue(value != null ? value.toString() : null)
                                                      .variableType(value != null ? value.getClass().getSimpleName() : null)
                                                      .processId(processDto.getId())
                                                      .build();
        variableRepository.save(variableEntity);
    }

    private TaskDto enrichTaskWithWorkflowData(TaskDto taskDto, CreateTaskRequest request) {
        UUID customerId = extractCustomerId(request.getVariables());
        if (customerId == null) {
            log.warn("No customerId found in variables for task: {}", taskDto.getTaskDefinitionKey());
            return null;
        }

        return switch (taskDto.getTaskDefinitionKey()) {
            case "ut_manual_creditworthiness_check" -> handleCreditworthinessCheck(customerId, request, taskDto);
            case "ut_manual_liability_check" -> handleLiabilityCheck(customerId, request, taskDto);
            case "ut_manual_household_check" -> handleRiskAssessment(customerId, request, taskDto);
            default -> {
                log.debug("No workflow enrichment needed for task: {}", taskDto.getTaskDefinitionKey());
                yield null;
            }
        };
    }

    private UUID extractCustomerId(Map<String, Object> variables) {
        try {
            Object customerIdValue = variables.get("customerId");
            return customerIdValue != null ? UUID.fromString(customerIdValue.toString()) : null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid customerId format", e);
            return null;
        }
    }

    private TaskDto handleCreditworthinessCheck(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        Boolean sufficientIncome = parseBoolean(request.getVariables().get("sufficientIncome"));
        String initiator = getStringValue(request.getVariables(), "initiator");
        return insuranceWorkflowService.manualCreditworthinessCheck(customerId,
                                                                    sufficientIncome,
                                                                    request.getTaskId(),
                                                                    initiator,
                                                                    taskDto
        );
    }

    private TaskDto handleLiabilityCheck(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        String insuranceCoverage = getStringValue(request.getVariables(), "insuranceCoverage");
        String insuranceSum = getStringValue(request.getVariables(), "insuranceSum");
        String initiator = getStringValue(request.getVariables(), "initiator");

        return insuranceWorkflowService.manualLiabilityCheck(customerId,
                                                             insuranceCoverage,
                                                             insuranceSum,
                                                             initiator,
                                                             taskDto
        );
    }

    private TaskDto handleRiskAssessment(UUID customerId, CreateTaskRequest request, TaskDto taskDto) {
        Boolean mudslideRisk = parseBoolean(request.getVariables().get("mudslideRisk"));
        Boolean floodRisk = parseBoolean(request.getVariables().get("floodRisk"));
        String initiator = getStringValue(request.getVariables(), "initiator");

        return insuranceWorkflowService.manualRiskAssessment(customerId,
                                                             mudslideRisk,
                                                             floodRisk,
                                                             initiator,
                                                             taskDto
        );
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        try {
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse boolean value: {}", value, e);
            return false;
        }
    }

    private String getStringValue(Map<String, Object> variables, String key) {
        Object value = variables.get(key);
        return value != null ? value.toString() : null;
    }

}
