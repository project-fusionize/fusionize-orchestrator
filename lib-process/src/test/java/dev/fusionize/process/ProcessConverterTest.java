package dev.fusionize.process;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ProcessConverterTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void convert() throws IOException, XMLStreamException {
        URL yamlUrl = this.getClass().getResource("/diagram_1.bpmn");
        assertNotNull(yamlUrl);
        String xml = Files.readString(new File(yamlUrl.getFile()).toPath());
        new ProcessConverter().convert(xml);
    }
}