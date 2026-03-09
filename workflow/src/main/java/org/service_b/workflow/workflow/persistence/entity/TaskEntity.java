package org.service_b.workflow.workflow.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TASKS")
@Data
public class TaskEntity {

    @Id
    @Column(name = "ID", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id")
    private String taskId;

    @Column(name = "name")
    private String name;

    @Column(name = "assignee")
    private String assignee;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "execution_id")
    private String executionId;

    @Column(name = "process_definition_id")
    private String processDefinitionId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "task_definition_key")
    private String taskDefinitionKey;

    @Column(name = "form_key")
    private String formKey;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "task_state")
    private String taskState;

    @Column(name = "additional_info")
    private String additionalInfo;

    @Column(name = "config")
    private String config;

    @Column(name = "config_data")
    private String configData;

    /*
    t.string :id
      t.string :name
      t.string :assignee
      t.datetime :created
      t.string :execution_id
      t.string :process_definition_id
      t.string :process_instance_id
      t.string :task_definition_key
      t.string :form_key
      t.string :tenant_id
      t.string :task_state
     */

}
