package dev.fusionize.storage.repo;

import dev.fusionize.storage.StorageConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageConfigRepository extends MongoRepository<StorageConfig, String> {
    List<StorageConfig> findByDomainStartingWith(String domain);

    Optional<StorageConfig> findByDomain(String domain);

    void deleteByDomain(String domain);
}
