package dev.fusionize.ai.repo;

import dev.fusionize.ai.model.AgentConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentConfigRepository extends MongoRepository<AgentConfig, String> {
    List<AgentConfig> findByDomainStartingWith(String domain);
    Optional<AgentConfig> findByDomain(String domain);
    void deleteByDomain(String domain);
}
