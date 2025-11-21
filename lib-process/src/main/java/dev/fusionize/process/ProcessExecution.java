package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.context.Context;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "process-execution")
public class ProcessExecution {
    @Id
    private String id;
    private String processExecutionId;
    private String processId;
    private String processDefinitionKey;
    private ProcessExecutionStatus status;
    private Context context;
    private List<ProcessElementExecution> activeElements = new ArrayList<>();
    private List<ProcessElementExecution> completedElements = new ArrayList<>();
    @Transient
    private Process process;

    public static ProcessExecution of(Process process, Context initialContext) {
        ProcessExecution execution = new ProcessExecution();
        execution.process = process;
        execution.processId = process.getProcessId();
        execution.processDefinitionKey = process.getProcessDefinitionKey();
        execution.status = ProcessExecutionStatus.IDLE;
        execution.context = initialContext != null ? initialContext : new Context();
        execution.processExecutionId = KeyUtil.getTimestampId("PEXE");
        return execution;
    }

    public ProcessElementExecution findElement(String elementExecutionId) {
        ProcessElementExecution found = activeElements.stream()
                .map(elem -> elem.findElement(elementExecutionId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        
        if (found != null) {
            return found;
        }
        
        return completedElements.stream()
                .map(elem -> elem.findElement(elementExecutionId))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public void moveToCompleted(ProcessElementExecution element) {
        activeElements.remove(element);
        completedElements.add(element);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessExecutionId() {
        return processExecutionId;
    }

    public void setProcessExecutionId(String processExecutionId) {
        this.processExecutionId = processExecutionId;
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

    public ProcessExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessExecutionStatus status) {
        this.status = status;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public List<ProcessElementExecution> getActiveElements() {
        return activeElements;
    }

    public void setActiveElements(List<ProcessElementExecution> activeElements) {
        this.activeElements = activeElements;
    }

    public List<ProcessElementExecution> getCompletedElements() {
        return completedElements;
    }

    public void setCompletedElements(List<ProcessElementExecution> completedElements) {
        this.completedElements = completedElements;
    }

    public Process getProcess() {
        return process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }
}

