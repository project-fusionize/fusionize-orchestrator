package dev.fusionize.process.repo;

import dev.fusionize.process.ProcessExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessExecutionRepository extends MongoRepository<ProcessExecution, String> {
    ProcessExecution save(ProcessExecution processExecution);
    Optional<ProcessExecution> findByProcessExecutionId(String processExecutionId);
    List<ProcessExecution> findByProcessId(String processId);
    List<ProcessExecution> findByProcessIdAndStatus(String processId, dev.fusionize.process.ProcessExecutionStatus status);
}

