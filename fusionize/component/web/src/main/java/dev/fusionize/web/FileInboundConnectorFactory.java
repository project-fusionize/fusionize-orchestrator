package dev.fusionize.web;


import dev.fusionize.web.services.FileInboundConnectorService;
import dev.fusionize.web.services.HttpInboundConnectorService;
import dev.fusionize.worker.component.annotations.RuntimeComponentDefinition;
import dev.fusionize.workflow.component.Actor;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeFactory;
import org.springframework.stereotype.Component;

@Component
@RuntimeComponentDefinition(
        type = FileInboundConnector.class,
        name = "File Inbound Connector",
        description = "File Inbound Connector component",
        actors = { Actor.SYSTEM })
public class FileInboundConnectorFactory implements ComponentRuntimeFactory<FileInboundConnector> {
    private final FileInboundConnectorService fileInboundConnectorService;

    public FileInboundConnectorFactory(FileInboundConnectorService fileInboundConnectorService) {
        this.fileInboundConnectorService = fileInboundConnectorService;
    }


    @Override
    public FileInboundConnector create() {
        return new FileInboundConnector(fileInboundConnectorService);
    }
}
