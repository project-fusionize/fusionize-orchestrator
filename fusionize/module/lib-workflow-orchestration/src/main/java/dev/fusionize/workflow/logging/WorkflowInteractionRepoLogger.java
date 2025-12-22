package dev.fusionize.workflow.logging;

import dev.fusionize.workflow.WorkflowInteraction;
import dev.fusionize.workflow.WorkflowInteractionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class WorkflowInteractionRepoLogger implements WorkflowInteractionLogger {
    private final WorkflowInteractionRepository repository;
    private final List<InteractionListener> listeners = new CopyOnWriteArrayList<>();

    public WorkflowInteractionRepoLogger(WorkflowInteractionRepository repository, List<InteractionListener> listeners) {
        this.repository = repository;
        if (listeners != null) {
            this.listeners.addAll(listeners);
        }
    }

    @Override
    public void addListener(InteractionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InteractionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void log(String workflowId, String workflowDomain, String workflowExecutionId, String workflowNodeId,
                    String nodeKey, String component, String actor,
                    WorkflowInteraction.InteractionType type, WorkflowInteraction.Visibility visibility, Object content) {
        WorkflowInteraction interaction = WorkflowInteraction.create(workflowId, workflowDomain, workflowExecutionId,
                workflowNodeId, nodeKey, component, actor, type, visibility, content);
        
        Logger logger = LoggerFactory.getLogger(component);
        logger.debug("[{}] {} ({}): {}", type, actor, visibility, content);

        CompletableFuture.runAsync(() -> {
            repository.save(interaction);
            listeners.forEach(l -> l.onInteraction(interaction));
        });
    }

    @Override
    public List<WorkflowInteraction> getInteractions(String workflowExecutionId) {
        return repository.findByWorkflowExecutionIdOrderByTimestampAsc(workflowExecutionId);
    }
}
