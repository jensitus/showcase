package at.cibseven.cibdemo.config;

import at.cibseven.cibdemo.dto.TaskDto;
import at.cibseven.cibdemo.service.TaskNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.cibseven.bpm.engine.delegate.TaskListener;
import org.cibseven.bpm.engine.impl.persistence.entity.TaskEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class TaskListenerConfiguration {

    private final TaskNotificationService taskNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Fires within CIB Seven's open transaction — command context is active here,
     * so getVariables() is safe. We snapshot all data into a plain TaskDto and
     * re-publish it as a Spring event so the AFTER_COMMIT handler can send the
     * HTTP notification without touching the (now-closed) command context.
     */
    @EventListener
    public void captureTaskEvent(DelegateTask delegateTask) {
        log.info("DelegateTask event captured for task: {}, event: {}", delegateTask.getName(), delegateTask.getEventName());
        TaskDto snapshot = buildSnapshot(delegateTask);
        eventPublisher.publishEvent(snapshot);
    }

    /**
     * Fires after CIB Seven's transaction has committed — execution is in the DB,
     * incident creation (on failure) will succeed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskEventAfterCommit(TaskDto snapshot) {
        log.info("Sending task notification after commit for task: {}, state: {}", snapshot.getName(), snapshot.getTaskState());
        taskNotificationService.notifyFromSnapshot(snapshot);
    }

    private TaskDto buildSnapshot(DelegateTask delegateTask) {
        TaskDto dto = new TaskDto();
        dto.setTaskId(delegateTask.getId());
        dto.setName(delegateTask.getName());
        dto.setTaskDefinitionKey(delegateTask.getTaskDefinitionKey());
        dto.setProcessInstanceId(delegateTask.getProcessInstanceId());
        dto.setProcessDefinitionId(delegateTask.getProcessDefinitionId());
        dto.setAssignee(delegateTask.getAssignee());
        dto.setExecutionId(delegateTask.getExecutionId());
        dto.setTenantId(delegateTask.getTenantId());
        dto.setVariables(delegateTask.getVariables());

        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            dto.setTaskState(TaskEntity.TaskState.STATE_CREATED.name());
            if (delegateTask.getCreateTime() != null) {
                dto.setCreated(delegateTask.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            dto.setTaskState(TaskEntity.TaskState.STATE_COMPLETED.name());
        }

        return dto;
    }
}
