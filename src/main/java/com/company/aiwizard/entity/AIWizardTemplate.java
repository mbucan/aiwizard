package com.company.aiwizard.entity;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.reports.entity.DataSetType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.OffsetDateTime;
import java.util.UUID;

@JmixEntity
@Table(name = "AI_WIZARD_TEMPLATE")
@Entity
public class AIWizardTemplate {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @NotNull
    @InstanceName
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @NotNull
    @Column(name = "CONNECTION_", nullable = false)
    private String connection;

    @NotNull
    @Column(name = "OPERATION", nullable = false)
    private String operation;

    @NotNull
    @Column(name = "DATASET_TYPE", nullable = false)
    private Integer datasetType;

    @Column(name = "CONTEXT_PREFIX")
    @Lob
    private String contextPrefix;

    @CreatedBy
    @Column(name = "CREATED_BY")
    private String createdBy;

    @CreatedDate
    @Column(name = "CREATED_DATE")
    private OffsetDateTime createdDate;

    @LastModifiedBy
    @Column(name = "LAST_MODIFIED_BY")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "LAST_MODIFIED_DATE")
    private OffsetDateTime lastModifiedDate;

    public String getContextPrefix() {
        return contextPrefix;
    }

    public void setContextPrefix(String contextPrefix) {
        this.contextPrefix = contextPrefix;
    }

    public void setDatasetType(DataSetType datasetType) {
        this.datasetType = datasetType == null ? null : datasetType.getId();
    }

    public DataSetType getDatasetType() {
        return datasetType == null ? null : DataSetType.fromId(datasetType);
    }

    public AIWizardOperation getOperation() {
        return operation == null ? null : AIWizardOperation.fromId(operation);
    }

    public void setOperation(AIWizardOperation operation) {
        this.operation = operation == null ? null : operation.getId();
    }

    public AIWizardConnection getConnection() {
        return connection == null ? null : AIWizardConnection.fromId(connection);
    }

    public void setConnection(AIWizardConnection connection) {
        this.connection = connection == null ? null : connection.getId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public OffsetDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(OffsetDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

}