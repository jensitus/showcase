package org.service_b.workflow.workflow.persistence.entity;

import org.service_b.workflow.workflow.domain.ResponsibleForType;

import java.util.UUID;

public interface Processable {
    UUID getId();
    ResponsibleForType getType();
}
