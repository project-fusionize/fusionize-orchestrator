package dev.fusionize.ai;

import dev.fusionize.ai.service.ClassifierService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "classifier",
        description = "Classifies input data into categories with confidence scoring",
        domain = "fuz.ai.Classifier",
        type = Classifier.class,
        actors = {Actor.AI})
public class ClassifierFactory implements ComponentRuntimeFactory<Classifier> {

    private final ClassifierService classifierService;

    public ClassifierFactory(ClassifierService classifierService) {
        this.classifierService = classifierService;
    }

    @Override
    public Classifier create() {
        return new Classifier(classifierService);
    }
}
