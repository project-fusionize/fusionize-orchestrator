package dev.fusionize.ai;


import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;

import org.springframework.stereotype.Component;

import dev.fusionize.ai.service.DocumentExtractorService;

@Component
@RuntimeComponentDefinition(
        name = "document-extractor",
        description = "Extracts data from documents",
        domain = "fuz.ai.DocumentExtractor",
        type = DocumentExtractor.class,
        actors = { Actor.AI })
public class DocumentExtractorFactory implements ComponentRuntimeFactory<DocumentExtractor> {

    private final DocumentExtractorService documentExtractorService;

    public DocumentExtractorFactory(DocumentExtractorService documentExtractorService) {
        this.documentExtractorService = documentExtractorService;
    }

    @Override
    public DocumentExtractor create() {
        return new DocumentExtractor(documentExtractorService);
    }

}
