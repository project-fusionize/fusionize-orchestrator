package dev.fusionize.workflow.registry;

import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowExecutionStatus;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.repo.WorkflowExecutionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class WorkflowExecutionRepoRegistry implements WorkflowExecutionRegistry {
    private final WorkflowExecutionRepository repository;
    private final MongoTemplate mongoTemplate;

    public WorkflowExecutionRepoRegistry(WorkflowExecutionRepository repository,
                                         @Qualifier("workerMongoTemplate") MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<WorkflowExecution> getWorkflowExecutions(String workflowId) {
        List<WorkflowExecution> executions = repository.findByWorkflowIdIn(List.of(workflowId));
        executions.forEach(WorkflowExecution::inflate);
        return executions;
    }

    public WorkflowExecution getWorkflowExecution(String workflowExecutionId) {
        WorkflowExecution execution = repository.findByWorkflowExecutionId(workflowExecutionId).orElse(null);
        if (execution != null) {
            execution.inflate();
        }
        return execution;
    }

    public WorkflowExecution register(WorkflowExecution workflowExecution) {
        synchronized (workflowExecution) {
            workflowExecution.flatten();
            workflowExecution.setUpdatedDate(Instant.now());
            if (workflowExecution.getCreatedDate() == null) {
                workflowExecution.setCreatedDate(Instant.now());
            }
            return repository.save(workflowExecution);
        }
    }

    @Override
    public void updateNodeExecution(String workflowExecutionId, WorkflowNodeExecution nodeExecution) {
        // Ensure updated date is set
        nodeExecution.setUpdatedDate(Instant.now());
        if (nodeExecution.getCreatedDate() == null) {
            nodeExecution.setCreatedDate(Instant.now());
        }

        // Prepare the update for the specific key in the map
        String mapKey = "nodeExecutionMap." + nodeExecution.getWorkflowNodeExecutionId();
        
        Query query = new Query(Criteria.where("workflowExecutionId").is(workflowExecutionId));
        Update update = new Update().set(mapKey, nodeExecution);
        
        mongoTemplate.updateFirst(query, update, WorkflowExecution.class);
    }

    @Override
    public void updateStatus(String workflowExecutionId, WorkflowExecutionStatus status) {
        Query query = new Query(Criteria.where("workflowExecutionId").is(workflowExecutionId));
        Update update = new Update()
                .set("status", status)
                .set("updatedDate", Instant.now());
        mongoTemplate.updateFirst(query, update, WorkflowExecution.class);
    }

    public void deleteIdlesFor(String workflowId) {
        repository.deleteByWorkflowIdAndStatus(workflowId, WorkflowExecutionStatus.IDLE.getName());
    }
}
