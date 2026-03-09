package at.cibseven.cibdemo.config;

import at.cibseven.cibdemo.GlobalTaskListenerParseListener;
import at.cibseven.cibdemo.service.ProcessStateNotificationService;
import at.cibseven.cibdemo.service.TaskNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.cibseven.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ProcessEnginePluginConfig extends AbstractProcessEnginePlugin {

    private final GlobalTaskListenerParseListener globalTaskListenerParseListener;

    public ProcessEnginePluginConfig(TaskNotificationService taskNotificationService,
                                     ProcessStateNotificationService processStateNotificationService) {
        this.globalTaskListenerParseListener = new GlobalTaskListenerParseListener(
                taskNotificationService, processStateNotificationService);
    }

    @Override
    public void preInit(ProcessEngineConfigurationImpl configuration) {
        List<BpmnParseListener> listeners = configuration.getCustomPreBPMNParseListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            configuration.setCustomPreBPMNParseListeners(listeners);
        }
        listeners.add(globalTaskListenerParseListener);
        log.info("Registered GlobalTaskListenerParseListener as BPMN parse listener");
    }
}
