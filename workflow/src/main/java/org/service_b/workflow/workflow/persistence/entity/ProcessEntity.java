package org.service_b.workflow.workflow.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.service_b.workflow.workflow.domain.ResponsibleForType;

import java.util.UUID;

@Entity
@Table(name = "PROCESSES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessEntity {
    @Id
    @Column(name = "id", nullable = false, length = 255)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "process_instance_id", length = 255)
    private String processInstanceId;

    @Column(name = "process_definition_id", length = 255)
    private String processDefinitionId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "responsible_for_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ResponsibleForType responsibleForType;

    @Column(name = "responsible_for_id", nullable = false)
    private UUID responsibleForId;

}
