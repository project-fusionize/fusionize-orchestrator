package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowLogRepository extends MongoRepository<WorkflowLog, String> {
    List<WorkflowLog> findByWorkflowExecutionIdOrderByTimestampAsc(String workflowExecutionId);
}
