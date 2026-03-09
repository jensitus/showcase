package at.cibseven.cibdemo;

import lombok.extern.slf4j.Slf4j;
import org.cibseven.bpm.engine.delegate.ExecutionListener;
import org.cibseven.bpm.engine.delegate.TaskListener;
import org.cibseven.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.cibseven.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.cibseven.bpm.engine.impl.pvm.process.ActivityImpl;
import org.cibseven.bpm.engine.impl.pvm.process.ScopeImpl;
import org.cibseven.bpm.engine.impl.util.xml.Element;

@Slf4j
public class GlobalTaskListenerParseListener extends AbstractBpmnParseListener {
    private final TaskListener taskListener;
    private final ExecutionListener processStateListener;

    public GlobalTaskListenerParseListener(TaskListener taskListener, ExecutionListener processStateListener) {
        this.taskListener = taskListener;
        this.processStateListener = processStateListener;
        log.info("GlobalTaskListenerParseListener initialized");
    }

    @Override
    public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
        processDefinition.addListener(ExecutionListener.EVENTNAME_END, processStateListener);
        log.info("Registered process state listener on END for: {}", processDefinition.getKey());
    }

    @Override
    public void parseUserTask(Element userTaskElement, ScopeImpl scope, ActivityImpl activity) {
        // task notifications handled via Spring @EventListener on DelegateTask
    }
}
