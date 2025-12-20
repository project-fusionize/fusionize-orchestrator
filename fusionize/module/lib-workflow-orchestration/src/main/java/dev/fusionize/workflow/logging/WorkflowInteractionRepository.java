package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowInteraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowInteractionRepository extends MongoRepository<WorkflowInteraction, String> {
    List<WorkflowInteraction> findByWorkflowExecutionIdOrderByTimestampAsc(String workflowExecutionId);
}
