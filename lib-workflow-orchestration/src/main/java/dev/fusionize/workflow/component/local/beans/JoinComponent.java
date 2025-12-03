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
import java.util.Comparator;
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
    public static final String CONF_WAIT_MODE = "waitMode";
    public static final String CONF_THRESHOLD_CT = "thresholdCount";

    public static final String CONF_AWAIT = "await";
    private MergeStrategy mergeStrategy = MergeStrategy.PICK_LAST;
    private WaitMode waitMode = WaitMode.ALL;
    private int thresholdCount = 1;

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

    public enum WaitMode {
        ALL,
        ANY,
        THRESHOLD
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_MERGE_STRATEGY)
                .ifPresent(strategy -> this.mergeStrategy = TextUtil.matchesFlexible("PICK_LAST", strategy)
                        ? MergeStrategy.PICK_LAST
                        : MergeStrategy.PICK_FIRST);
        config.varString(CONF_WAIT_MODE)
                .ifPresent(mode -> this.waitMode = TextUtil.matchesFlexible("THRESHOLD", mode)
                        ? WaitMode.THRESHOLD
                        : (TextUtil.matchesFlexible("ANY", mode)
                                ? WaitMode.ANY
                                : WaitMode.ALL));
        config.varList(CONF_AWAIT).ifPresent(items -> awaits.addAll(
                items.stream().filter(i -> i instanceof String).toList()));
        config.varInt(CONF_THRESHOLD_CT).ifPresent(c -> thresholdCount = c);
    }

    @Override
    public void canActivate(Context context, ComponentUpdateEmitter emitter) {
        if (awaits.isEmpty()) {
            return;
        }

        String nodeId = context.getRuntimeData().getWorkflowNodeId();

        // Check if any of the awaited nodes are in the history of the current nodes
        boolean anyAwaitedNodeFound = context.currentNodes().stream()
                .anyMatch(node -> isNodeInHistory(node, awaits, nodeId));

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

        // Sort executions by ID to ensure deterministic processing order
        matchingNodeExecution.sort(Comparator.comparing(WorkflowNodeExecution::getWorkflowNodeExecutionId));

        List<Context> contextsToMerge = new ArrayList<>();
        List<String> satisfiedAwaits = new ArrayList<>();
        String triggerExecutionId = null;

        for (WorkflowNodeExecution exec : matchingNodeExecution) {
            Context ctx = exec.getStageContext();
            contextsToMerge.add(ctx);

            // Collect awaited nodes found in this execution's history
            for (WorkflowGraphNodeRecursive node : ctx.currentNodes()) {
                collectFoundAwaitedNodes(node, awaits, satisfiedAwaits, nodeId);
            }

            // Check if condition is met with the accumulated executions so far
            if (isConditionMet(satisfiedAwaits)) {
                triggerExecutionId = exec.getWorkflowNodeExecutionId();
                break;
            }
        }

        if (triggerExecutionId != null) {
            // If the condition is met, we check if WE are the one who triggered it (or the
            // representative for it)
            // The triggerExecutionId corresponds to the execution that *completed* the
            // condition.
            // Since we sorted by ID, this is the "earliest" set of executions that
            // satisfies the condition.

            if (triggerExecutionId.equals(currentExecutionNodeId)) {
                Context mergedContext = mergeContexts(contextsToMerge);
                emitter.success(mergedContext);
            } else {
                emitter.logger().info("Condition met by earlier execution {}. Current {} skipping.", triggerExecutionId,
                        currentExecutionNodeId);
            }
        } else {
            emitter.logger().info("Wait condition not yet met. Awaited: {}, Found: {}, Mode: {}", awaits,
                    satisfiedAwaits.stream().distinct().toList(), waitMode);
        }
    }

    private boolean isConditionMet(List<String> satisfiedAwaits) {
        long uniqueFound = satisfiedAwaits.stream().distinct().count();

        return switch (waitMode) {
            case ANY -> uniqueFound >= 1;
            case THRESHOLD -> uniqueFound >= thresholdCount;
            default -> uniqueFound == awaits.stream().distinct().count();
        };
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

    private boolean isNodeInHistory(WorkflowGraphNodeRecursive node, List<String> targets,
            String currentWorkflowNodeId) {
        // Stop recursion if we hit the current node ID (previous cycle)
        if (node.getNode().equals(currentWorkflowNodeId)) {
            return false;
        }

        if (targets.contains(node.getNode())) {
            return true;
        }
        if (node.getParents() == null || node.getParents().isEmpty()) {
            return false;
        }
        for (WorkflowGraphNodeRecursive parent : node.getParents()) {
            if (isNodeInHistory(parent, targets, currentWorkflowNodeId)) {
                return true;
            }
        }
        return false;
    }

    private void collectFoundAwaitedNodes(WorkflowGraphNodeRecursive node, List<String> targets, List<String> found,
            String currentWorkflowNodeId) {
        // Stop recursion if we hit the current node ID (previous cycle)
        if (node.getNode().equals(currentWorkflowNodeId)) {
            return;
        }

        if (targets.contains(node.getNode())) {
            found.add(node.getNode());
        }
        if (node.getParents() != null) {
            for (WorkflowGraphNodeRecursive parent : node.getParents()) {
                collectFoundAwaitedNodes(parent, targets, found, currentWorkflowNodeId);
            }
        }
    }
}
