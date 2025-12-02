package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.user.activity.DomainEntity;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import org.flowable.bpmn.model.BpmnModel;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Document(collection = "process")
public class Process extends DomainEntity {
    @Id
    private String id;
    @Indexed(unique = true)
    private String processId;
    private String processDefinitionKey;

    private String description;
    private int version;
    private boolean active = true;
    private String bpmnXml;
    private String bpmnSupportYaml;
    @Transient
    private BpmnModel bpmnModel;
    @Transient
    private Map<String, WorkflowNodeDescription> definitions;

    public static Process of(String processId, String bpmnXml, BpmnModel bpmnModel) {
        Process process = new Process();
        process.processId = processId != null ? processId : KeyUtil.getTimestampId("PROC");
        process.bpmnXml = bpmnXml;
        process.bpmnModel = bpmnModel;
        process.processDefinitionKey = bpmnModel.getMainProcess() != null
                ? bpmnModel.getMainProcess().getId()
                : null;
        if (bpmnModel.getMainProcess() != null) {
            process.setName(bpmnModel.getMainProcess().getName());
        }
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

    public String getBpmnSupportYaml() {
        return bpmnSupportYaml;
    }

    public void setBpmnSupportYaml(String bpmnSupportYaml) {
        this.bpmnSupportYaml = bpmnSupportYaml;
    }

    public BpmnModel getBpmnModel() {
        return bpmnModel;
    }

    public void setBpmnModel(BpmnModel bpmnModel) {
        this.bpmnModel = bpmnModel;
    }

    public Map<String, WorkflowNodeDescription> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(Map<String, WorkflowNodeDescription> definitions) {
        this.definitions = definitions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Process process = (Process) o;
        return Objects.equals(processId, process.processId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId);
    }
}
