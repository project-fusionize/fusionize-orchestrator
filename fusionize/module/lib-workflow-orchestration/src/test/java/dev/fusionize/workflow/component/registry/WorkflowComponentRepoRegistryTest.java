package dev.fusionize.workflow.component.registry;

import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.repo.WorkflowComponentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowComponentRepoRegistryTest {

    @Mock
    private WorkflowComponentRepository repository;

    @InjectMocks
    private WorkflowComponentRepoRegistry registry;

    @Test
    void shouldGetAllComponents() {
        // setup
        var component = mock(WorkflowComponent.class);
        when(repository.findAllByDomainStartingWith("")).thenReturn(List.of(component));

        // expectation
        List<WorkflowComponent> result = registry.getComponents();

        // validation
        assertThat(result).hasSize(1);
        verify(repository).findAllByDomainStartingWith("");
    }

    @Test
    void shouldGetComponentById() {
        // setup
        var component = mock(WorkflowComponent.class);
        when(repository.findById("comp-1")).thenReturn(Optional.of(component));

        // expectation
        WorkflowComponent result = registry.getWorkflowComponentById("comp-1");

        // validation
        assertThat(result).isEqualTo(component);
    }

    @Test
    void shouldReturnNull_whenIdIsBlank() {
        // setup
        // no setup needed

        // expectation
        WorkflowComponent resultNull = registry.getWorkflowComponentById(null);
        WorkflowComponent resultEmpty = registry.getWorkflowComponentById("");
        WorkflowComponent resultBlank = registry.getWorkflowComponentById("   ");

        // validation
        assertThat(resultNull).isNull();
        assertThat(resultEmpty).isNull();
        assertThat(resultBlank).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldReturnNull_whenIdNotFound() {
        // setup
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        // expectation
        WorkflowComponent result = registry.getWorkflowComponentById("missing-id");

        // validation
        assertThat(result).isNull();
    }

    @Test
    void shouldGetComponentByDomain() {
        // setup
        var component = mock(WorkflowComponent.class);
        when(repository.findByDomain("my.domain")).thenReturn(Optional.of(component));

        // expectation
        WorkflowComponent result = registry.getWorkflowComponentByDomain("MY.DOMAIN");

        // validation
        assertThat(result).isEqualTo(component);
        verify(repository).findByDomain("my.domain");
    }

    @Test
    void shouldReturnNull_whenDomainIsBlank() {
        // setup
        // no setup needed

        // expectation
        WorkflowComponent resultNull = registry.getWorkflowComponentByDomain(null);
        WorkflowComponent resultEmpty = registry.getWorkflowComponentByDomain("");
        WorkflowComponent resultBlank = registry.getWorkflowComponentByDomain("   ");

        // validation
        assertThat(resultNull).isNull();
        assertThat(resultEmpty).isNull();
        assertThat(resultBlank).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldRegisterComponent() {
        // setup
        var component = mock(WorkflowComponent.class);
        when(repository.save(component)).thenReturn(component);

        // expectation
        WorkflowComponent result = registry.register(component);

        // validation
        assertThat(result).isEqualTo(component);
        verify(repository).save(component);
    }

    @Test
    void shouldReturnNull_whenRegisteringNull() {
        // setup
        // no setup needed

        // expectation
        WorkflowComponent result = registry.register(null);

        // validation
        assertThat(result).isNull();
        verifyNoInteractions(repository);
    }

    @Test
    void shouldUpsertOnDuplicateKey() {
        // setup
        var component = mock(WorkflowComponent.class);
        var existing = mock(WorkflowComponent.class);
        when(component.getDomain()).thenReturn("my.domain");
        when(repository.save(component)).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByDomain("my.domain")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        // expectation
        WorkflowComponent result = registry.register(component);

        // validation
        assertThat(result).isEqualTo(existing);
        verify(existing).mergeFrom(component);
        verify(repository).save(existing);
    }

    @Test
    void shouldRethrowDuplicateKey_whenExistingNotFound() {
        // setup
        var component = mock(WorkflowComponent.class);
        when(component.getDomain()).thenReturn("my.domain");
        when(repository.save(component)).thenThrow(new DuplicateKeyException("duplicate"));
        when(repository.findByDomain("my.domain")).thenReturn(Optional.empty());

        // expectation + validation
        assertThatThrownBy(() -> registry.register(component))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
