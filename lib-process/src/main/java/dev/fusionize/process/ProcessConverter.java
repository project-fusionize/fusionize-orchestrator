package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.workflow.Workflow;
import dev.fusionize.workflow.WorkflowNode;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.descriptor.WorkflowDescription;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import dev.fusionize.workflow.descriptor.WorkflowTransformer;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Workflow convertToWorkflow(Process process)  {
        if (process == null || process.getBpmnModel() == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }

        BpmnModel model = process.getBpmnModel();
        org.flowable.bpmn.model.Process bpmnProcess = model.getMainProcess();

        WorkflowDescription desc = new WorkflowDescription();
        desc.setName(bpmnProcess.getName() != null ? bpmnProcess.getName() : "Imported BPMN Workflow");
        desc.setDomain("bpmn");
        desc.setKey(process.getProcessId() != null ? process.getProcessId() : KeyUtil.getTimestampId("WFLO"));
        desc.setDescription("Imported from BPMN XML");
        desc.setVersion(1);
        desc.setActive(true);

        Map<String, WorkflowNodeDescription> nodeMap = new HashMap<>();
        Map<String, String> elementMap = new HashMap<>();

        // 1. Convert BPMN elements → WorkflowNodeDescriptions
        for (FlowElement element : bpmnProcess.getFlowElements()) {
            WorkflowNodeDescription node = mapBpmnElement(element);
            if (node.getType() == null) {
                log.warn("Skipping unsupported BPMN element: {} {}", element.getClass().getSimpleName(), element.getId());
                continue;
            }
            nodeMap.put(buildKey(element), node);
            elementMap.put(element.getId(), buildKey(element));
        }

        // 2. Build "next" transitions (SequenceFlow)
        for (FlowElement element : bpmnProcess.getFlowElements()) {
            if (!(element instanceof org.flowable.bpmn.model.FlowNode flowNode)) {
                continue;
            }

            WorkflowNodeDescription currentNode = nodeMap.get(elementMap.get(flowNode.getId()));
            if (currentNode == null) continue;

            List<String> next = new ArrayList<>();
            for (org.flowable.bpmn.model.SequenceFlow sf : flowNode.getOutgoingFlows()) {
                next.add(elementMap.get(sf.getTargetRef()));
            }

            currentNode.setNext(next);
        }

        desc.setNodes(nodeMap);
        return new WorkflowTransformer().toWorkflow(desc);
    }

    private String buildKey(FlowElement element) {
        String elementType = element.getClass().getSimpleName();
        elementType = Character.toLowerCase(elementType.charAt(0)) + elementType.substring(1);
        return elementType + "." + element.getId();
    }

    private WorkflowNodeDescription mapBpmnElement(FlowElement element){
        WorkflowNodeDescription node = new WorkflowNodeDescription();
        node.setComponentConfig(new HashMap<>());
        if (element instanceof org.flowable.bpmn.model.StartEvent) {
            node.setType(WorkflowNodeType.START);
        }
        if (element instanceof org.flowable.bpmn.model.EndEvent) {
            node.setType(WorkflowNodeType.END);
        }
        if (element instanceof org.flowable.bpmn.model.ExclusiveGateway) {
            node.setType(WorkflowNodeType.DECISION);
        }
        if (element instanceof org.flowable.bpmn.model.ReceiveTask
                || element instanceof org.flowable.bpmn.model.IntermediateCatchEvent) {
            node.setType(WorkflowNodeType.WAIT);
        }
        if (element instanceof org.flowable.bpmn.model.Task) {
            node.setType(WorkflowNodeType.TASK);
        }

        return node;
    }
}
