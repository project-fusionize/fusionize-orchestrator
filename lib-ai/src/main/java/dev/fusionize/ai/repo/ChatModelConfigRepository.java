package dev.fusionize.ai.repo;

import dev.fusionize.ai.model.ChatModelConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatModelConfigRepository extends MongoRepository<ChatModelConfig, String> {
    Optional<ChatModelConfig> findByKey(String key);
}
