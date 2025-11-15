package dev.fusionize.process;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

public class ProcessConverter {
    private static final Logger log = LoggerFactory.getLogger(ProcessConverter.class);

    /**
     * Convert BPMN XML string to Process object
     *
     * @param xmlString The BPMN XML content
     * @return Process object containing the BpmnModel
     * @throws XMLStreamException if XML parsing fails
     */
    public Process convert(String xmlString) throws XMLStreamException {
        return convert(null, xmlString);
    }

    /**
     * Convert BPMN XML string to Process object with a specific process ID
     *
     * @param processId Optional process ID (if null, will be generated)
     * @param xmlString The BPMN XML content
     * @return Process object containing the BpmnModel
     * @throws XMLStreamException if XML parsing fails
     */
    public Process convert(String processId, String xmlString) throws XMLStreamException {
        if (xmlString == null || xmlString.trim().isEmpty()) {
            throw new IllegalArgumentException("BPMN XML string cannot be null or empty");
        }

        StringReader stringReader = new StringReader(xmlString);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader xmlReader = factory.createXMLStreamReader(stringReader);
        
        BpmnXMLConverter converter = new BpmnXMLConverter();
        BpmnModel model = converter.convertToBpmnModel(xmlReader);
        
        if (model == null) {
            throw new IllegalStateException("Failed to convert BPMN XML to BpmnModel");
        }

        org.flowable.bpmn.model.Process process = model.getMainProcess();
        if (process == null) {
            throw new IllegalStateException("BPMN model has no main process");
        }

        // Log process elements for debugging
        log.debug("Converting BPMN process: {}", process.getId());
        for (FlowElement flowElement : process.getFlowElements()) {
            log.debug("Process element: {} - {}", flowElement.getId(), flowElement.getName());
        }

        return Process.of(processId, xmlString, model);
    }
}
