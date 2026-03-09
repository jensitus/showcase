package org.service_b.workflow.shared.config;

//import io.vanillabp.springboot.modules.WorkflowModuleIdAwareProperties;
//import io.vanillabp.springboot.modules.WorkflowModuleProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InsuranceProperties  {

    public static final String WORKFLOW_MODULE_ID = "insurance";

    public String getWorkflowModuleId() {
        return WORKFLOW_MODULE_ID;
    }
}
