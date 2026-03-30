package dev.fusionize.orchestrator.process;

import dev.fusionize.process.Process;
import dev.fusionize.process.ProcessConverter;
import dev.fusionize.process.registry.ProcessRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessControllerTest {

    @Mock
    private ProcessRegistry processRegistry;

    @Mock
    private ProcessConverter processConverter;

    @InjectMocks
    private ProcessController processController;

    @Test
    void shouldGetAllProcesses() {
        // setup
        var process = new Process();
        when(processRegistry.getAll()).thenReturn(List.of(process));

        // expectation
        var result = processController.getAll();

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).containsExactly(process);
    }

    @Test
    void shouldGetProcessById() {
        // setup
        var process = new Process();
        when(processRegistry.getProcess("p-1")).thenReturn(process);

        // expectation
        var result = processController.get("p-1");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEqualTo(process);
    }

    @Test
    void shouldReturn404_whenProcessNotFound() {
        // setup
        when(processRegistry.getProcess("unknown")).thenReturn(null);

        // expectation
        var result = processController.get("unknown");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void shouldRegisterProcess() throws Exception {
        // setup
        var process = new Process();
        var saved = new Process();
        when(processConverter.convert("definition")).thenReturn(process);
        when(processRegistry.register(process)).thenReturn(saved);

        // expectation
        var result = processController.registerProcess("definition");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEqualTo(saved);
    }

    @Test
    void shouldReturn400_whenConvertFails() throws Exception {
        // setup
        when(processConverter.convert("bad")).thenThrow(new RuntimeException("parse error"));

        // expectation
        var result = processController.registerProcess("bad");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void shouldAnnotateProcess() throws Exception {
        // setup
        var process = new Process();
        var saved = new Process();
        when(processRegistry.getProcess("p-1")).thenReturn(process);
        when(processRegistry.register(process)).thenReturn(saved);

        // expectation
        var result = processController.annotateProcess("p-1", "yaml-content");

        // validation
        verify(processConverter).annotate(process, "yaml-content");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getMessage()).isEqualTo(saved);
    }

    @Test
    void shouldReturn404_whenAnnotateProcessNotFound() {
        // setup
        when(processRegistry.getProcess("unknown")).thenReturn(null);

        // expectation
        var result = processController.annotateProcess("unknown", "yaml");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void shouldReturn400_whenAnnotateFails() throws Exception {
        // setup
        var process = new Process();
        when(processRegistry.getProcess("p-1")).thenReturn(process);
        doThrow(new RuntimeException("annotate error")).when(processConverter).annotate(process, "bad-yaml");

        // expectation
        var result = processController.annotateProcess("p-1", "bad-yaml");

        // validation
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }
}
