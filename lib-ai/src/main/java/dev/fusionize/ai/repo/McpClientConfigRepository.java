package dev.fusionize.ai.repo;

import dev.fusionize.ai.model.McpClientConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface McpClientConfigRepository extends MongoRepository<McpClientConfig, String> {
    Optional<McpClientConfig> findByKey(String key);
}
