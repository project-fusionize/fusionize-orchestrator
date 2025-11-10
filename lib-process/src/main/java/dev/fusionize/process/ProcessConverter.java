package dev.fusionize.process;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

public class ProcessConverter {

    public Process convert(String xmlString) throws XMLStreamException {
        StringReader stringReader = new StringReader(xmlString);
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader xmlReader = factory.createXMLStreamReader(stringReader);
        BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(xmlReader);
        org.flowable.bpmn.model.Process process = model.getMainProcess();
        for (FlowElement flowElement : process.getFlowElements()) {
            System.out.println(flowElement.getId() +" " +flowElement.getName());
        }
        return null;
    }
}
