package dev.fusionize.orchestrator.workflow;

import dev.fusionize.ai.exception.ChatModelException;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.workflow.agent.UserRequest;
import dev.fusionize.workflow.agent.WorkflowAgentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowAgentControllerTest {

    @Mock
    private WorkflowAgentService workflowAgentService;

    @InjectMocks
    private WorkflowAgentController workflowAgentController;

    @Test
    void shouldProcessPrompt() throws ChatModelException {
        // setup
        var userRequest = new UserRequest();
        userRequest.setMessage("test prompt");
        when(workflowAgentService.process(userRequest)).thenReturn("response");

        // expectation
        ServicePayload<String> result = workflowAgentController.processPrompt(userRequest);

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEqualTo("response");
    }

    @Test
    void shouldProcessPromptStream() throws ChatModelException {
        // setup
        var userRequest = new UserRequest();
        userRequest.setMessage("test prompt");
        Flux<String> expectedFlux = Flux.just("a", "b");
        when(workflowAgentService.processStream(userRequest)).thenReturn(expectedFlux);

        // expectation
        Flux<String> result = workflowAgentController.processPromptStream(userRequest);

        // validation
        StepVerifier.create(result)
                .expectNext("a")
                .expectNext("b")
                .verifyComplete();
    }

    @Test
    void shouldDelegateToService() throws ChatModelException {
        // setup
        var userRequest = new UserRequest();
        userRequest.setMessage("delegate test");
        when(workflowAgentService.process(userRequest)).thenReturn("delegated");

        // expectation
        workflowAgentController.processPrompt(userRequest);

        // validation
        verify(workflowAgentService).process(userRequest);
    }
}
