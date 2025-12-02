package dev.fusionize.process;

import dev.fusionize.common.utility.KeyUtil;
import dev.fusionize.process.converters.events.EndEventConverter;
import dev.fusionize.process.converters.events.IntermediateCatchEventConverter;
import dev.fusionize.process.converters.events.StartEventConverter;
import dev.fusionize.process.converters.gateways.ComplexGatewayConverter;
import dev.fusionize.process.converters.gateways.ExclusiveGatewayConverter;
import dev.fusionize.process.converters.gateways.InclusiveGatewayConverter;
import dev.fusionize.process.converters.gateways.ParallelGatewayConverter;
import dev.fusionize.process.converters.tasks.ManualTaskConverter;
import dev.fusionize.process.converters.tasks.ScriptTaskConverter;
import dev.fusionize.process.converters.tasks.ServiceTaskConverter;
import dev.fusionize.workflow.Workflow;

import dev.fusionize.common.parser.YamlParser;
import dev.fusionize.workflow.descriptor.WorkflowDescription;
import dev.fusionize.workflow.descriptor.WorkflowNodeDescription;
import dev.fusionize.workflow.descriptor.WorkflowTransformer;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProcessConverter {
    private static final Logger log = LoggerFactory.getLogger(ProcessConverter.class);

    public static String buildKey(FlowElement element) {
        String elementType = element.getClass().getSimpleName();
        elementType = Character.toLowerCase(elementType.charAt(0)) + elementType.substring(1);
        return elementType + "#" + element.getId();
    }

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

    public Workflow convertToWorkflow(Process process) {
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
            if (element instanceof SequenceFlow) {
                continue;
            }
            WorkflowNodeDescription node = mapBpmnElement(element, model);
            if (node == null || node.getType() == null) {
                log.warn("Skipping unsupported BPMN element: {} {}", element.getClass().getSimpleName(),
                        element.getId());
                continue;
            }
            nodeMap.put(buildKey(element), node);
            elementMap.put(element.getId(), buildKey(element));
        }

        // 1.5 Override with YAML definitions
        if (process.getDefinitions() != null) {
            for (Map.Entry<String, WorkflowNodeDescription> entry : process.getDefinitions().entrySet()) {
                String key = entry.getKey();
                WorkflowNodeDescription override = entry.getValue();
                WorkflowNodeDescription existing = nodeMap.get(key);
                if (existing == null) {
                    continue;
                }

                if (override.getComponent() != null) {
                    existing.setComponent(override.getComponent());
                }
                if (override.getType() != null) {
                    existing.setType(override.getType());
                }
                if (override.getComponentConfig() != null && !override.getComponentConfig().isEmpty()) {
                    if (existing.getComponentConfig() == null) {
                        existing.setComponentConfig(new HashMap<>());
                    }
                    existing.getComponentConfig().putAll(override.getComponentConfig());
                }
            }
        }

        // 2. Build "next" transitions (SequenceFlow)
        for (FlowElement element : bpmnProcess.getFlowElements()) {
            if (!(element instanceof org.flowable.bpmn.model.FlowNode flowNode)) {
                continue;
            }

            WorkflowNodeDescription currentNode = nodeMap.get(elementMap.get(flowNode.getId()));
            if (currentNode == null)
                continue;

            List<String> next = new ArrayList<>();
            for (org.flowable.bpmn.model.SequenceFlow sf : flowNode.getOutgoingFlows()) {
                next.add(elementMap.get(sf.getTargetRef()));
            }

            currentNode.setNext(next);
        }

        desc.setNodes(nodeMap);
        return new WorkflowTransformer().toWorkflow(desc);
    }

    private WorkflowNodeDescription mapBpmnElement(FlowElement element, BpmnModel model) {
        List<ProcessNodeConverter<?>> converters = List.of(
                new ComplexGatewayConverter(),
                new InclusiveGatewayConverter(),
                new ExclusiveGatewayConverter(),
                new ParallelGatewayConverter(),
                new ScriptTaskConverter(),
                new ServiceTaskConverter(),
                new StartEventConverter(),
                new EndEventConverter(),
                new IntermediateCatchEventConverter(),
                new ManualTaskConverter());

        for (ProcessNodeConverter<?> converter : converters) {
            if (converter.canConvert(element)) {
                // Unchecked cast is safe because canConvert checks the type
                @SuppressWarnings("unchecked")
                ProcessNodeConverter<FlowElement> typedConverter = (ProcessNodeConverter<FlowElement>) converter;
                return typedConverter.convert(element, model);
            }
        }

        return new WorkflowNodeDescription();
    }

    public void annotate(Process process, String bpmnSupportYaml) {
        if (process == null) {
            throw new IllegalArgumentException("Process cannot be null");
        }
        process.setBpmnSupportYaml(bpmnSupportYaml);

        if (bpmnSupportYaml != null && !bpmnSupportYaml.trim().isEmpty()) {
            YamlParser<Map> yamlParser = new YamlParser<>();
            Map<String, Object> rawMap = yamlParser.fromYaml(bpmnSupportYaml, Map.class);

            if (rawMap != null) {
                YamlParser<WorkflowNodeDescription> nodeParser = new YamlParser<>();
                Map<String, WorkflowNodeDescription> definitions = new HashMap<>();

                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    String nodeYaml = yamlParser.toYaml((Map) entry.getValue());
                    WorkflowNodeDescription node = nodeParser.fromYaml(nodeYaml, WorkflowNodeDescription.class);
                    definitions.put(entry.getKey(), node);
                }
                process.setDefinitions(definitions);
            }
        }
    }
}
