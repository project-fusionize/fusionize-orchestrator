package dev.fusionize.project;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    Project save(Project project);
    List<Project> findAll();
    List<Project> findAllByIdInAndDomainStartsWith(List<String> ids, String domain);
    List<Project> findAllByDomainStartsWith(String domain);
    Optional<Project> findById(String id);
    Optional<Project> findByDomain(String domain);
}
