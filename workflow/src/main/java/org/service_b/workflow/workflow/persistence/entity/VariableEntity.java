package org.service_b.workflow.workflow.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "VARIABLES",
        uniqueConstraints = @UniqueConstraint(
                name = "unique_process_variable_type",
                columnNames = {"process_id", "variable_name"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariableEntity {

    @Id
    @Column(name = "id", nullable = false, length = 255)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "process_id", length = 255)
    private UUID processId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Column(name = "variable_name", length = 255)
    private String variableName;

    @Column(name = "variable_value", length = 255)
    private String variableValue;

    @Column(name = "variable_type", length = 50)
    private String variableType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "process_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_process_id"),
            insertable = false,
            updatable = false
    )
    private ProcessEntity process;

}

