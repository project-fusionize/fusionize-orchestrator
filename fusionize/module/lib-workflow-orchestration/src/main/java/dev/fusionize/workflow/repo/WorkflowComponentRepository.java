package dev.fusionize.workflow.repo;

import dev.fusionize.workflow.component.WorkflowComponent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowComponentRepository extends MongoRepository<WorkflowComponent, String> {
    WorkflowComponent save(WorkflowComponent component);
    Optional<WorkflowComponent> findByComponentId(String id);
    Optional<WorkflowComponent> findByDomain(String id);
    List<WorkflowComponent> findAllByDomainStartingWith(String domain);

}
