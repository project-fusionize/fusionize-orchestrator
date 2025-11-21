package dev.fusionize.process;

import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.descriptor.WorkflowDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ProcessConverterTest {
    ProcessConverter processConverter;
    @BeforeEach
    void setUp() {
        processConverter = new ProcessConverter();
    }

    @Test
    void convert() throws IOException, XMLStreamException {
        URL bpmnUrl = this.getClass().getResource("/diagram_email.bpmn");
        assertNotNull(bpmnUrl);
        String xml = Files.readString(new File(bpmnUrl.getFile()).toPath());
        Process process = new ProcessConverter().convert(xml);
        Workflow workflow= processConverter.convertToWorkflow(process);
        assertNotNull(workflow);
        System.out.println(new WorkflowDescriptor().toYamlDescription(workflow));
    }
}