package dev.fusionize.workflow.repo;

import dev.fusionize.workflow.WorkflowExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {
    WorkflowExecution save(WorkflowExecution workflowExecution);
    Optional<WorkflowExecution> findByWorkflowExecutionId(String id);
    List<WorkflowExecution> findByWorkflowIdIn(List<String> ids);
}
