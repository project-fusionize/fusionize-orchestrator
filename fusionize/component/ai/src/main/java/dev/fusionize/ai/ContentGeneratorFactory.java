package dev.fusionize.ai;

import dev.fusionize.ai.service.ContentGeneratorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "content-generator",
        description = "Generates text content from templates and context data",
        domain = "fuz.ai.ContentGenerator",
        type = ContentGenerator.class,
        actors = {Actor.AI})
public class ContentGeneratorFactory implements ComponentRuntimeFactory<ContentGenerator> {

    private final ContentGeneratorService contentGeneratorService;

    public ContentGeneratorFactory(ContentGeneratorService contentGeneratorService) {
        this.contentGeneratorService = contentGeneratorService;
    }

    @Override
    public ContentGenerator create() {
        return new ContentGenerator(contentGeneratorService);
    }
}
