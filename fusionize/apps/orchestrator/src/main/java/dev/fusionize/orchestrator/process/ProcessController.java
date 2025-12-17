package dev.fusionize.orchestrator.process;

import dev.fusionize.Application;
import dev.fusionize.common.payload.ServicePayload;
import dev.fusionize.common.payload.ServiceResponse;
import dev.fusionize.process.Process;
import dev.fusionize.process.ProcessConverter;
import dev.fusionize.process.registry.ProcessRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Application.API_PREFIX + "/process")
public class ProcessController {

    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    private final ProcessRegistry processRegistry;
    private final ProcessConverter processConverter;

    public ProcessController(ProcessRegistry processRegistry, ProcessConverter processConverter) {
        this.processRegistry = processRegistry;
        this.processConverter = processConverter;
    }

    @GetMapping
    public ServicePayload<List<Process>> getAll() {
        List<Process> processes = processRegistry.getAll();
        return new ServicePayload.Builder<List<Process>>()
                .response(new ServiceResponse.Builder<List<Process>>()
                        .status(200)
                        .message(processes)
                        .build())
                .build();
    }

    @GetMapping("/{processId}")
    public ServicePayload<Process> get(@PathVariable String processId) {
        Process process = processRegistry.getProcess(processId);
        if (process == null) {
            return new ServicePayload.Builder<Process>()
                    .response(new ServiceResponse.Builder<Process>()
                            .status(404)
                            .message(null)
                            .build())
                    .build();
        }
        return new ServicePayload.Builder<Process>()
                .response(new ServiceResponse.Builder<Process>()
                        .status(200)
                        .message(process)
                        .build())
                .build();
    }

    @PostMapping("/register")
    public ServicePayload<Process> registerProcess(@RequestBody String processDefinition) {
        Process process;
        try {
             process = processConverter.convert(processDefinition);
        } catch (Exception e) {
            log.error("Failed to parse process definition", e);
            return new ServicePayload.Builder<Process>()
                    .response(new ServiceResponse.Builder<Process>()
                            .status(400)
                            .message(null)
                            .build())
                    .build();
        }

        Process saved = processRegistry.register(process);

        return new ServicePayload.Builder<Process>()
                .response(new ServiceResponse.Builder<Process>()
                        .status(200)
                        .message(saved)
                        .build())
                .build();
    }

    @PatchMapping("/{processId}")
    public ServicePayload<Process> annotateProcess(@PathVariable String processId, @RequestBody String yaml) {
        Process process = processRegistry.getProcess(processId);
        if (process == null) {
            return new ServicePayload.Builder<Process>()
                    .response(new ServiceResponse.Builder<Process>()
                            .status(404)
                            .message(null)
                            .build())
                    .build();
        }

        try {
            processConverter.annotate(process, yaml);
        } catch (Exception e) {
            log.error("Failed to annotate process", e);
            return new ServicePayload.Builder<Process>()
                    .response(new ServiceResponse.Builder<Process>()
                            .status(400)
                            .message(null)
                            .build())
                    .build();
        }

        Process saved = processRegistry.register(process);

        return new ServicePayload.Builder<Process>()
                .response(new ServiceResponse.Builder<Process>()
                        .status(200)
                        .message(saved)
                        .build())
                .build();
    }
}
