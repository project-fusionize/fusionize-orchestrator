package dev.fusionize.ai;

import dev.fusionize.ai.service.DataValidatorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "data-validator",
        description = "Validates data against business rules using AI reasoning",
        domain = "fuz.ai.DataValidator",
        type = DataValidator.class,
        actors = {Actor.AI})
public class DataValidatorFactory implements ComponentRuntimeFactory<DataValidator> {

    private final DataValidatorService dataValidatorService;

    public DataValidatorFactory(DataValidatorService dataValidatorService) {
        this.dataValidatorService = dataValidatorService;
    }

    @Override
    public DataValidator create() {
        return new DataValidator(dataValidatorService);
    }
}
