package dev.fusionize.process.registry;

import dev.fusionize.process.Process;
import dev.fusionize.process.repo.ProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessRepoRegistryTest {

    @Mock
    private ProcessRepository repository;

    @InjectMocks
    private ProcessRepoRegistry registry;

    @Test
    void shouldGetProcess() {
        // setup
        Process process = new Process();
        process.setProcessId("proc-1");
        when(repository.findByProcessId("proc-1")).thenReturn(Optional.of(process));

        // expectation
        Process result = registry.getProcess("proc-1");

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getProcessId()).isEqualTo("proc-1");
        verify(repository).findByProcessId("proc-1");
    }

    @Test
    void shouldReturnNull_whenProcessIdBlank() {
        // setup
        // no setup needed

        // expectation
        Process result = registry.getProcess("");

        // validation
        assertThat(result).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnNull_whenProcessNotFound() {
        // setup
        when(repository.findByProcessId("unknown")).thenReturn(Optional.empty());

        // expectation
        Process result = registry.getProcess("unknown");

        // validation
        assertThat(result).isNull();
    }

    @Test
    void shouldGetAllProcesses() {
        // setup
        Process p1 = new Process();
        p1.setProcessId("proc-1");
        Process p2 = new Process();
        p2.setProcessId("proc-2");
        when(repository.findAll()).thenReturn(List.of(p1, p2));

        // expectation
        List<Process> result = registry.getAll();

        // validation
        assertThat(result).hasSize(2);
        verify(repository).findAll();
    }

    @Test
    void shouldRegisterProcess() {
        // setup
        Process process = new Process();
        process.setProcessId("proc-1");
        when(repository.save(process)).thenReturn(process);

        // expectation
        Process result = registry.register(process);

        // validation
        assertThat(result).isNotNull();
        assertThat(result.getProcessId()).isEqualTo("proc-1");
        verify(repository).save(process);
    }

    @Test
    void shouldReturnNull_whenRegisteringNull() {
        // setup
        // no setup needed

        // expectation
        Process result = registry.register(null);

        // validation
        assertThat(result).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldUpsertOnDuplicateKey() {
        // setup
        Process process = new Process();
        process.setProcessId("proc-1");

        Process existing = new Process();
        existing.setId("mongo-id-123");
        existing.setProcessId("proc-1");

        when(repository.save(process))
                .thenThrow(new DuplicateKeyException("duplicate"))
                .thenReturn(process);
        when(repository.findByProcessId("proc-1")).thenReturn(Optional.of(existing));

        // expectation
        Process result = registry.register(process);

        // validation
        assertThat(result).isNotNull();
        verify(repository, times(2)).save(process);
        verify(repository).findByProcessId("proc-1");
    }

    @Test
    void shouldRethrowDuplicateKey_whenExistingNotFound() {
        // setup
        Process process = new Process();
        process.setProcessId("proc-1");

        when(repository.save(process)).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByProcessId("proc-1")).thenReturn(Optional.empty());

        // expectation & validation
        assertThrows(DuplicateKeyException.class, () -> registry.register(process));
    }
}
