package dev.fusionize.workflow.repo;

import dev.fusionize.workflow.Workflow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {
    Workflow save(Workflow workflow);
    Optional<Workflow> findByWorkflowId(String id);
    Optional<Workflow> findByDomain(String id);

}
