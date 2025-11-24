package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.common.utility.TextUtil;
import dev.fusionize.workflow.WorkflowExecution;
import dev.fusionize.workflow.WorkflowNodeExecution;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;
import dev.fusionize.workflow.context.WorkflowGraphNodeRecursive;
import dev.fusionize.workflow.registry.WorkflowExecutionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JoinComponent handles the synchronization of multiple parallel execution
 * paths in a workflow.
 * It waits for a specified set of parent nodes to complete before merging their
 * contexts and proceeding.
 * <p>
 * Configuration:
 * <ul>
 * <li>{@code await}: List of node IDs to wait for.</li>
 * <li>{@code mergeStrategy}: Strategy to merge data from incoming contexts
 * (PICK_FIRST or PICK_LAST).</li>
 * </ul>
 */
public class JoinComponent implements LocalComponentRuntime {
    private final WorkflowExecutionRegistry workflowExecutionRegistry;
    public static final String CONF_MERGE_STRATEGY = "mergeStrategy";
    public static final String CONF_AWAIT = "await";
    private MergeStrategy mergeStrategy = MergeStrategy.PICK_LAST;
    private final List<String> awaits = new ArrayList<>();

    public JoinComponent(WorkflowExecutionRegistry workflowExecutionRegistry) {
        this.workflowExecutionRegistry = workflowExecutionRegistry;
    }

    /**
     * Strategy for merging data when keys collide in incoming contexts.
     */
    public enum MergeStrategy {
        /**
         * Retain the value from the first context encountered.
         */
        PICK_FIRST,
        /**
         * Overwrite with the value from the last context encountered (default).
         */
        PICK_LAST,
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_MERGE_STRATEGY)
                .ifPresent(strategy -> this.mergeStrategy = TextUtil.matchesFlexible("PICK_LAST", strategy)
                        ? MergeStrategy.PICK_LAST
                        : MergeStrategy.PICK_FIRST);
        config.varList(CONF_AWAIT).ifPresent(items -> awaits.addAll(
                items.stream().filter(i -> i instanceof String).toList()));
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (awaits.isEmpty()) {
            return;
        }

        // Check if any of the awaited nodes are in the history of the current nodes
        boolean anyAwaitedNodeFound = context.currentNodes().stream()
                .anyMatch(node -> isNodeInHistory(node, awaits));

        if (anyAwaitedNodeFound) {
            emitter.success(context);
        }
    }

    @Override
    public void run(Context context, ComponentUpdateEmitter emitter) {
        if (awaits.isEmpty()) {
            return;
        }

        String executionId = context.getRuntimeData().getWorkflowExecutionId();
        String nodeId = context.getRuntimeData().getWorkflowNodeId();
        String currentExecutionNodeId = context.getRuntimeData().getWorkflowNodeExecutionId();

        if (executionId == null || nodeId == null) {
            emitter.failure(new Exception("JoinComponent requires WorkflowExecutionId and WorkflowNodeId in context"));
            return;
        }

        WorkflowExecution workflowExecution = workflowExecutionRegistry.getWorkflowExecution(executionId);
        if (workflowExecution == null) {
            emitter.failure(new Exception("JoinComponent requires WorkflowNodeExecution to exist"));
            return;
        }

        List<WorkflowNodeExecution> matchingNodeExecution = workflowExecution.findNodesByWorkflowNodeId(nodeId);
        List<Context> contexts = matchingNodeExecution.stream()
                .map(WorkflowNodeExecution::getStageContext).toList();

        emitter.logger().info("Found matching contexts: {}", contexts.size());
        // Check if ALL awaited nodes are in the history of the COMBINED contexts
        List<String> foundAwaitedNodes = new ArrayList<>();
        for (Context ctx : contexts) {
            for (WorkflowGraphNodeRecursive node : ctx.currentNodes()) {
                collectFoundAwaitedNodes(node, awaits, foundAwaitedNodes);
            }
        }

        boolean allAwaitedNodesFound = foundAwaitedNodes.stream().distinct().count() == awaits.stream().distinct()
                .count();

        if (allAwaitedNodesFound) {
            if (!isLeaderExecution(matchingNodeExecution, currentExecutionNodeId, emitter)) {
                return;
            }

            Context mergedContext = mergeContexts(contexts);
            emitter.success(mergedContext);
        }
        // If not all found, we just wait (do nothing, effectively holding the context
        // in the map)
    }

    /**
     * Determines if the current execution is the "leader" responsible for emitting
     * the merged context.
     * This prevents multiple emissions when multiple parents finish concurrently.
     *
     * @param matchingExecutions List of all concurrent executions for this Join
     *                           node.
     * @param currentId          The ID of the current execution.
     * @param emitter            The emitter for logging.
     * @return true if this is the leader, false otherwise.
     */
    private boolean isLeaderExecution(List<WorkflowNodeExecution> matchingExecutions, String currentId,
            ComponentUpdateEmitter emitter) {
        String leaderId = matchingExecutions.stream()
                .map(WorkflowNodeExecution::getWorkflowNodeExecutionId)
                .max(String::compareTo)
                .orElse(null);

        if (leaderId != null && !leaderId.equals(currentId)) {
            emitter.logger().info("Not the leader execution. Current: {}, Leader: {}. Skipping emission.",
                    currentId, leaderId);
            return false;
        }
        return true;
    }

    /**
     * Merges multiple contexts into a single context based on the configured
     * strategy.
     *
     * @param contexts List of contexts to merge.
     * @return A new merged Context.
     */
    private Context mergeContexts(List<Context> contexts) {
        Context mergedContext = new Context();

        // Merge Data
        if (mergeStrategy == MergeStrategy.PICK_FIRST) {
            for (Context ctx : contexts) {
                for (Map.Entry<String, Object> entry : ctx.getData().entrySet()) {
                    if (!mergedContext.contains(entry.getKey())) {
                        mergedContext.set(entry.getKey(), entry.getValue());
                    }
                }
            }
        } else { // PICK_LAST (Default)
            for (Context ctx : contexts) {
                mergedContext.getData().putAll(ctx.getData());
            }
        }

        // Merge Decisions and GraphNodes (Append all)
        // Merge Decisions and GraphNodes (Deduplicate by ID)
        Map<String, dev.fusionize.workflow.context.WorkflowDecision> decisionsMap = new java.util.HashMap<>();
        Map<String, dev.fusionize.workflow.context.WorkflowGraphNode> graphNodesMap = new java.util.HashMap<>();

        for (Context ctx : contexts) {
            if (ctx.getDecisions() != null) {
                for (dev.fusionize.workflow.context.WorkflowDecision d : ctx.getDecisions()) {
                    decisionsMap.putIfAbsent(d.getDecisionNode(), d);
                }
            }
            if (ctx.getGraphNodes() != null) {
                for (dev.fusionize.workflow.context.WorkflowGraphNode n : ctx.getGraphNodes()) {
                    graphNodesMap.compute(n.getNode(), (key, existing) -> {
                        if (existing == null) {
                            return n;
                        } else {
                            // Merge parents
                            List<String> mergedParents = new ArrayList<>(existing.getParents());
                            if (n.getParents() != null) {
                                for (String parent : n.getParents()) {
                                    if (!mergedParents.contains(parent)) {
                                        mergedParents.add(parent);
                                    }
                                }
                            }
                            existing.setParents(mergedParents);
                            return existing;
                        }
                    });
                }
            }
        }

        mergedContext.getDecisions().addAll(decisionsMap.values());
        mergedContext.getGraphNodes().addAll(graphNodesMap.values());

        return mergedContext;
    }

    private boolean isNodeInHistory(WorkflowGraphNodeRecursive node, List<String> targets) {
        if (targets.contains(node.getNode())) {
            return true;
        }
        if (node.getParents() == null || node.getParents().isEmpty()) {
            return false;
        }
        for (WorkflowGraphNodeRecursive parent : node.getParents()) {
            if (isNodeInHistory(parent, targets)) {
                return true;
            }
        }
        return false;
    }

    private void collectFoundAwaitedNodes(WorkflowGraphNodeRecursive node, List<String> targets, List<String> found) {
        if (targets.contains(node.getNode())) {
            found.add(node.getNode());
        }
        if (node.getParents() != null) {
            for (WorkflowGraphNodeRecursive parent : node.getParents()) {
                collectFoundAwaitedNodes(parent, targets, found);
            }
        }
    }
}
