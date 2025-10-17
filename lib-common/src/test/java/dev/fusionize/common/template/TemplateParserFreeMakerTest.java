package dev.fusionize.common.template;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateParserFreeMakerTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("templates");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null) {
            Files.walk(tempDir)
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    void testFileTemplateLoader() throws Exception {
        // Arrange: create a temp file template
        Path templateFile = tempDir.resolve("greeting.ftl");
        Files.writeString(templateFile, "Hello ${name}!");

        TemplateParserFreeMaker parser = new TemplateParserFreeMaker.Builder()
                .withFileTemplateDir(tempDir.toString())
                .build();

        Map<String, Object> model = Map.of("name", "Amir");
        StringWriter out = new StringWriter();

        // Act
        parser.parse(model, "greeting.ftl", out);

        // Assert
        assertEquals("Hello Amir!", out.toString().trim());
    }

    @Test
    void testStringTemplateLoader() throws Exception {
        TemplateParserFreeMaker parser = new TemplateParserFreeMaker.Builder()
                .withStringTemplate("inline.ftl", "Inline Hello ${target}!")
                .build();

        Map<String, Object> model = Map.of("target", "World");
        StringWriter out = new StringWriter();

        parser.parse(model, "inline.ftl", out);

        assertEquals("Inline Hello World!", out.toString().trim());
    }

    @Test
    void testUrlTemplateLoader() throws Exception {
        // Create a local temp template file and serve it via file:// URL
        Path templateFile = tempDir.resolve("remote.ftl");
        Files.createFile(templateFile);
        Files.writeString(templateFile, "Hi from URL, ${who}!");
        URL baseUrl = tempDir.toUri().toURL();

        TemplateParserFreeMaker parser = new TemplateParserFreeMaker.Builder()
                .withUrlTemplate(baseUrl)
                .build();

        Map<String, Object> model = Map.of("who", "Fusionize");
        StringWriter out = new StringWriter();

        parser.parse(model, "remote.ftl", out);

        assertEquals("Hi from URL, Fusionize!", out.toString().trim());
    }

    @Test
    void testInvalidTemplateThrowsException() throws Exception {
        TemplateParserFreeMaker parser = new TemplateParserFreeMaker.Builder()
                .withStringTemplate("bad.ftl", "Hello ${name") // Missing closing brace
                .build();

        Map<String, Object> model = Map.of("name", "Amir");
        StringWriter out = new StringWriter();

        assertThrows(TemplateParserException.class, () -> parser.parse(model, "bad.ftl", out));
    }

    @Test
    void testBuilderWithoutLoaderThrows() {
        assertThrows(IllegalStateException.class, () ->
                new TemplateParserFreeMaker.Builder().build()
        );
    }
}
