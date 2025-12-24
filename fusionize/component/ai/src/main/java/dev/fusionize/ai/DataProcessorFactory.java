package dev.fusionize.ai;


import dev.fusionize.ai.service.DataProcessorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        name = "data-processor",
        description = "process data and create output",
        domain = "fuz.ai.DataProcessor",
        type = DataProcessor.class,
        actors = { Actor.AI })
public class DataProcessorFactory implements ComponentRuntimeFactory<DataProcessor> {

    private final DataProcessorService dataProcessorService;

    public DataProcessorFactory(DataProcessorService dataProcessorService) {
        this.dataProcessorService = dataProcessorService;
    }


    @Override
    public DataProcessor create() {
        return new DataProcessor(dataProcessorService);
    }

}
