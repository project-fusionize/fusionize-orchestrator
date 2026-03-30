package dev.fusionize.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectDescriptorTest {

    @Test
    void shouldSetAndGetAllFields() {
        // setup
        var descriptor = new ProjectDescriptor();

        // expectation
        descriptor.setId("desc-1");
        descriptor.setDescription("A test project");
        descriptor.setName("Test Project");
        descriptor.setCover("cover.png");
        descriptor.setDomain("test-domain");

        // validation
        assertThat(descriptor.getId()).isEqualTo("desc-1");
        assertThat(descriptor.getDescription()).isEqualTo("A test project");
        assertThat(descriptor.getName()).isEqualTo("Test Project");
        assertThat(descriptor.getCover()).isEqualTo("cover.png");
        assertThat(descriptor.getDomain()).isEqualTo("test-domain");
    }

    @Test
    void shouldSerializeToYaml() {
        // setup
        var descriptor = new ProjectDescriptor();
        descriptor.setId("yaml-id");
        descriptor.setName("Yaml Project");
        descriptor.setDescription("A yaml description");
        descriptor.setDomain("yaml-domain");
        descriptor.setCover("yaml-cover.png");

        // expectation
        var yaml = descriptor.toYaml();

        // validation
        assertThat(yaml).contains("yaml-id");
        assertThat(yaml).contains("Yaml Project");
        assertThat(yaml).contains("A yaml description");
        assertThat(yaml).contains("yaml-domain");
        assertThat(yaml).contains("yaml-cover.png");
    }

    @Test
    void shouldParseFromFile(@TempDir Path tempDir) throws IOException {
        // setup
        var yamlContent = """
                id: file-id
                name: File Project
                description: From file
                domain: file-domain
                cover: file-cover.png
                """;
        var file = tempDir.resolve("descriptor.yml").toFile();
        Files.writeString(file.toPath(), yamlContent);

        // expectation
        var descriptor = ProjectDescriptor.fromFile(file);

        // validation
        assertThat(descriptor.getId()).isEqualTo("file-id");
        assertThat(descriptor.getName()).isEqualTo("File Project");
        assertThat(descriptor.getDescription()).isEqualTo("From file");
        assertThat(descriptor.getDomain()).isEqualTo("file-domain");
        assertThat(descriptor.getCover()).isEqualTo("file-cover.png");
    }

    @Test
    void shouldThrowForEmptyFile(@TempDir Path tempDir) throws IOException {
        // setup
        var file = tempDir.resolve("empty.yml").toFile();
        Files.writeString(file.toPath(), "");

        // expectation
        // validation
        assertThatThrownBy(() -> ProjectDescriptor.fromFile(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not readable");
    }
}
