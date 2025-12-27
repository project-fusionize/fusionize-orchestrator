package dev.fusionize.process.registry;

import dev.fusionize.process.Process;

import java.util.List;

public interface ProcessRegistry {
    Process getProcess(String processId);

    Process register(Process process);

    List<Process> getAll();
}
