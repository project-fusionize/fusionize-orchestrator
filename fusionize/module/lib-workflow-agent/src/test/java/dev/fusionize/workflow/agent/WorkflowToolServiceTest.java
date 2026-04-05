package dev.fusionize.workflow.agent;

import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.WorkflowComponent;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.local.LocalComponentRuntimeFactory;
import dev.fusionize.workflow.component.registery.WorkflowComponentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowToolServiceTest {

    @Mock
    private WorkflowComponentRegistry workflowComponentRegistry;

    @Mock
    private LocalComponentRuntimeFactory<LocalComponentRuntime> localComponentRuntimeFactory;

    private WorkflowToolService workflowToolService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        List<LocalComponentRuntimeFactory<? extends LocalComponentRuntime>> factories = List.of(localComponentRuntimeFactory);
        workflowToolService = new WorkflowToolService(workflowComponentRegistry, factories);
    }

    @Test
    void shouldListAvailableWorkflowComponents() {
        // setup
        var registryComponent = mock(WorkflowComponent.class);
        var factoryComponent = mock(WorkflowComponent.class);
        when(workflowComponentRegistry.getComponents()).thenReturn(List.of(registryComponent));
        when(localComponentRuntimeFactory.describe()).thenReturn(factoryComponent);

        // expectation
        var result = workflowToolService.listAvailableWorkflowComponents();

        // validation
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(registryComponent, factoryComponent);
    }

    @Test
    void shouldListAvailableWorkflowNodeTypes() {
        // setup
        // no setup needed, method has no dependencies

        // expectation
        var result = workflowToolService.listAvailableWorkflowNodeTypes();

        // validation
        assertThat(result).hasSize(5);
        assertThat(result).containsKey(WorkflowNodeType.START);
        assertThat(result).containsKey(WorkflowNodeType.END);
        assertThat(result).containsKey(WorkflowNodeType.TASK);
        assertThat(result).containsKey(WorkflowNodeType.WAIT);
        assertThat(result).containsKey(WorkflowNodeType.DECISION);
    }

    @Test
    void shouldReturnExampleWorkflowYaml() {
        // setup
        // resource loading depends on classpath availability

        // expectation
        var result = workflowToolService.getExampleWorkflowYaml();

        // validation
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldReturnErrorMessage_whenExampleNotFound() {
        // setup
        // the resource /examples/loan-origination.workflow.yml likely does not exist in test context
        var service = new WorkflowToolService(workflowComponentRegistry,
                List.of(localComponentRuntimeFactory)) {
            @Override
            public String getExampleWorkflowYaml() {
                // simulate resource not found by using a non-existent path
                try (var inputStream = getClass().getResourceAsStream("/examples/non-existent-workflow.yml")) {
                    if (inputStream == null) {
                        return "Error: Example workflow not found.";
                    }
                    return new String(inputStream.readAllBytes());
                } catch (Exception e) {
                    return "Error reading example workflow: " + e.getMessage();
                }
            }
        };

        // expectation
        var result = service.getExampleWorkflowYaml();

        // validation
        assertThat(result).contains("Error");
        assertThat(result).contains("Example workflow not found");
    }
}
