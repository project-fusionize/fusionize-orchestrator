package dev.fusionize.ai;

import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(name = "document-extractor", description = "Extracts data from documents", type = DocumentExtractor.class, actors = {
        Actor.AI })
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
