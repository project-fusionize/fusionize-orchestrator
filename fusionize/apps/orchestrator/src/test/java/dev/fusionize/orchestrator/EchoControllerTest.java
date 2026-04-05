package dev.fusionize.orchestrator;

import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.web.services.HttpInboundConnectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EchoControllerTest {

    @Mock
    private EmailBoxService emailBoxService;

    @Mock
    private HttpInboundConnectorService httpInboundConnectorService;

    @Mock
    private FileInboundConnectorService fileInboundConnectorService;

    @InjectMocks
    private EchoController echoController;

    @Test
    void shouldAddEmail() {
        // setup
        var message = "test";

        // expectation
        var result = echoController.email(message);

        // validation
        verify(emailBoxService).addInbox("test");
        assertThat(result).isEqualTo("added");
    }

    @Test
    void shouldInvokeHttpInbound() {
        // setup
        var body = Map.<String, Object>of("key", "value");
        var workflowKey = "wf-1";
        var workflowNodeKey = "node-1";

        // expectation
        var result = echoController.httpInbound(body, workflowKey, workflowNodeKey);

        // validation
        verify(httpInboundConnectorService).invoke(
                new HttpInboundConnectorService.HttpConnectorKey(workflowKey, workflowNodeKey), body);
        assertThat(result).isEqualTo("added");
    }

    @Test
    void shouldInvokeFileInbound() {
        // setup
        var file = mock(MultipartFile.class);
        var workflowKey = "wf-1";
        var workflowNodeKey = "node-1";

        // expectation
        var result = echoController.fileInbound(file, workflowKey, workflowNodeKey);

        // validation
        verify(fileInboundConnectorService).invoke(
                new FileInboundConnectorService.IngestKey(workflowKey, workflowNodeKey), file);
        assertThat(result).isEqualTo("added");
    }
}
