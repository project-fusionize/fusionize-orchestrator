package dev.fusionize.ai;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.WorkflowNodeType;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "DocumentExtractor",
        description = "DocumentExtractor is a multimodal processing component in Fusionize that takes files such as PDFs or images and extracts structured information from them using Spring AI. It provides a simple, unified interface for turning unstructured documents into machine-readable data that workflows and agents can consume. Whether the input is a scanned form, an ID photo, an invoice, or a multipage PDF, the DocumentExtractor analyzes the content using vision-language models and returns normalized text and fields that downstream steps can use for decisioning, automation, or verification.",
        type = DocumentExtractor.class,
        compatible = WorkflowNodeType.TASK
)
public class DocumentExtractorFactory implements ComponentRuntimeFactory<DocumentExtractor> {

    private final org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder;

    public DocumentExtractorFactory(org.springframework.ai.chat.client.ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Override
    public DocumentExtractor create() {
        return new DocumentExtractor(chatClientBuilder);
    }

}
