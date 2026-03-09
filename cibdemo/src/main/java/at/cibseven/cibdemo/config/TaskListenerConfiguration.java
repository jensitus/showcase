package at.cibseven.cibdemo.config;

import at.cibseven.cibdemo.service.TaskNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.delegate.DelegateTask;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.Map;

@Configuration
@Slf4j
public class TaskListenerConfiguration {
    private final TaskNotificationService taskNotificationService;

    public TaskListenerConfiguration(TaskNotificationService taskNotificationService) {
        this.taskNotificationService = taskNotificationService;
        log.info("TaskListenerConfiguration initialized with TaskNotificationService");
    }

    @EventListener
    public void onTaskEvent(DelegateTask delegateTask) {
        log.info("DelegateTask event received for task: {}, event: {}", delegateTask.getName(), delegateTask.getEventName());
        Map<String, Object> variables = delegateTask.getVariables();
        taskNotificationService.notify(delegateTask);
    }
}
