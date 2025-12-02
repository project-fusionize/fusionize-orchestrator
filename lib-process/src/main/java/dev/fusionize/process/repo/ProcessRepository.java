package dev.fusionize.process.repo;

import dev.fusionize.process.Process;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessRepository extends MongoRepository<Process, String> {
    Optional<Process> findByProcessId(String processId);
}

