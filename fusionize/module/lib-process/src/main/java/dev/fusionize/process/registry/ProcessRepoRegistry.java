package dev.fusionize.process.registry;

import dev.fusionize.process.Process;
import dev.fusionize.process.repo.ProcessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class ProcessRepoRegistry implements ProcessRegistry {

    private static final Logger log = LoggerFactory.getLogger(ProcessRepoRegistry.class);
    private final ProcessRepository repository;

    public ProcessRepoRegistry(ProcessRepository repository) {
        this.repository = repository;
    }

    @Override
    public Process getProcess(String processId) {
        if (!StringUtils.hasText(processId))
            return null;
        return repository.findByProcessId(processId).orElse(null);
    }


    @Override
    public List<Process> getAll() {
        return repository.findAll();
    }

    @Override
    public Process register(Process process) {
        if (process == null)
            return null;

        try {
            return repository.save(process);
        } catch (DuplicateKeyException ex) {
            log.warn("Duplicate key detected for processId='{}'. Attempting upsert.",
                    process.getProcessId());

            Process existing = null;
            if (StringUtils.hasText(process.getProcessId())) {
                existing = repository.findByProcessId(process.getProcessId()).orElse(null);
            }

            if (existing != null) {
                // Determine what to update on existing process from the new one
                // For now, let's assume we update content similar to Workflow.mergeFrom if needed
                // But Process entity doesn't have mergeFrom method yet.
                // We'll update basic fields for now or just save if ID matches (which save does effectively for @Id but here processId is secondary unique index)
                
                // Unlike Workflow, Process uses 'id' (mongo ID) and 'processId' (business ID).
                // Existing has the DB ID. New 'process' might not have it.
                process.setId(existing.getId());
                
                try {
                    return repository.save(process);
                } catch (Exception saveEx) {
                    log.error("Upsert failed after duplicate key detection for process '{}'.",
                            process.getProcessId(), saveEx);
                    throw saveEx;
                }
            } else {
                log.error(
                        "Duplicate key exception occurred, but no existing document found for processId='{}'. Re-throwing.",
                        process.getProcessId());
                throw ex;
            }

        } catch (Exception e) {
            log.error("Failed to register process '{}'", process.getProcessId(), e);
            throw e;
        }
    }
}
