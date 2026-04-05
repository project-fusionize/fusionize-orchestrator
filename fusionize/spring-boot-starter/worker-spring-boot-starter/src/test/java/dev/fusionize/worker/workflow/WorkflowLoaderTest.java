package dev.fusionize.worker.workflow;

import dev.fusionize.workflow.Workflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowLoaderTest {

    private final WorkflowLoader workflowLoader = new WorkflowLoader();

    @Test
    void shouldReturnEmptyList_whenDirNotExists() throws IOException {
        // setup
        var nonExistentPath = "/tmp/non-existent-dir-" + System.nanoTime();

        // expectation
        List<Workflow> result = workflowLoader.loadWorkflows(nonExistentPath);

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenNotDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        File tempFile = tempDir.resolve("not-a-directory.txt").toFile();
        tempFile.createNewFile();

        // expectation
        List<Workflow> result = workflowLoader.loadWorkflows(tempFile.getAbsolutePath());

        // validation
        assertThat(result).isEmpty();
    }

    @Test
    void shouldLoadWorkflowsFromDirectory(@TempDir Path tempDir) throws IOException {
        // setup
        String yamlContent = """
                kind: Workflow
                apiVersion: v1
                name: test-workflow
                domain: test-domain
                key: test-key
                version: 1
                active: true
                nodes:
                  start:
                    type: START
                    component: noop
                    next: [end]
                  end:
                    type: END
                    component: noop
                """;
        Path workflowFile = tempDir.resolve("test.workflow.yml");
        Files.writeString(workflowFile, yamlContent);

        // expectation
        List<Workflow> result = workflowLoader.loadWorkflows(tempDir.toString());

        // validation
        assertThat(result).isNotEmpty();
    }

    @Test
    void shouldSkipInvalidFiles(@TempDir Path tempDir) throws IOException {
        // setup
        String invalidYaml = "this is not valid: [yaml: content: {{{}}}";
        Path invalidFile = tempDir.resolve("invalid.workflow.yml");
        Files.writeString(invalidFile, invalidYaml);

        // expectation
        List<Workflow> result = workflowLoader.loadWorkflows(tempDir.toString());

        // validation
        assertThat(result).isEmpty();
    }
}
