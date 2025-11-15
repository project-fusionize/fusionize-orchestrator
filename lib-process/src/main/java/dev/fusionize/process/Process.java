package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "process")
public class Process extends DomainEntity {
    @Id
    private String id;
    @Indexed(unique = true)
    private String processId;
    private String processDefinitionKey;
    private String name;
    private String description;
    private int version;
    private boolean active = true;
    private String bpmnXml;
    @Transient
    private BpmnModel bpmnModel;

    public static Process of(String processId, String bpmnXml, BpmnModel bpmnModel) {
        Process process = new Process();
        process.processId = processId != null ? processId : KeyUtil.getTimestampId("PROC");
        process.bpmnXml = bpmnXml;
        process.bpmnModel = bpmnModel;
        process.processDefinitionKey = bpmnModel.getMainProcess() != null 
                ? bpmnModel.getMainProcess().getId() 
                : null;
        process.name = bpmnModel.getMainProcess() != null 
                ? bpmnModel.getMainProcess().getName() 
                : null;
        process.version = 1;
        process.active = true;
        return process;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public BpmnModel getBpmnModel() {
        return bpmnModel;
    }

    public void setBpmnModel(BpmnModel bpmnModel) {
        this.bpmnModel = bpmnModel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Process process = (Process) o;
        return Objects.equals(processId, process.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId);
    }
}

