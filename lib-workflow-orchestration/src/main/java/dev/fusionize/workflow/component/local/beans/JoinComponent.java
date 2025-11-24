package dev.fusionize.workflow.component.local.beans;

import dev.fusionize.common.utility.TextUtil;
import dev.fusionize.workflow.component.local.LocalComponentRuntime;
import dev.fusionize.workflow.component.runtime.ComponentRuntimeConfig;
import dev.fusionize.workflow.component.runtime.interfaces.ComponentUpdateEmitter;
import dev.fusionize.workflow.context.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class JoinComponent implements LocalComponentRuntime {
    // Key: executionId + ":" + nodeId -> List of Contexts
    private static final ConcurrentHashMap<String, List<Context>> pendingJoins = new ConcurrentHashMap<>();

    public static final String CONF_MERGE_STRATEGY = "mergeStrategy";
    public static final String CONF_AWAIT = "await";
    private MergeStrategy mergeStrategy = MergeStrategy.PICK_LAST;
    private final List<String> awaits = new ArrayList<>();

    public enum MergeStrategy {
        PICK_FIRST,
        PICK_LAST,
    }

    @Override
    public void configure(ComponentRuntimeConfig config) {
        config.varString(CONF_MERGE_STRATEGY)
                .ifPresent(mergeStrategy -> this.mergeStrategy = TextUtil.matchesFlexible("PICK_LAST", mergeStrategy)
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

        String executionId = context.varString("_executionId").orElse(null);
        String nodeId = context.varString("_nodeId").orElse(null);

        if (executionId == null || nodeId == null) {
            emitter.failure(new Exception("JoinComponent requires _executionId and _nodeId in context"));
            return;
        }

        String key = executionId + ":" + nodeId;
        List<Context> contexts = pendingJoins.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        contexts.add(context);

        // Check if ALL awaited nodes are in the history of the COMBINED contexts
        List<String> foundAwaitedNodes = new ArrayList<>();
        for (Context ctx : contexts) {
            for (dev.fusionize.workflow.context.WorkflowGraphNodeRecursive node : ctx.currentNodes()) {
                collectFoundAwaitedNodes(node, awaits, foundAwaitedNodes);
            }
        }

        boolean allAwaitedNodesFound = foundAwaitedNodes.stream().distinct().count() == awaits.stream().distinct()
                .count();

        if (allAwaitedNodesFound) {
            // Merge logic
            Context mergedContext = new Context();

            // Merge Strategy for Data
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
            for (Context ctx : contexts) {
                mergedContext.getDecisions().addAll(ctx.getDecisions());
                mergedContext.getGraphNodes().addAll(ctx.getGraphNodes());
            }

            // Clean up
            pendingJoins.remove(key);

            emitter.success(mergedContext);
        }
        // If not all found, we just wait (do nothing, effectively holding the context
        // in the map)
    }

    private boolean isNodeInHistory(dev.fusionize.workflow.context.WorkflowGraphNodeRecursive node,
            List<String> targets) {
        if (targets.contains(node.getNode())) {
            return true;
        }
        if (node.getParents() == null || node.getParents().isEmpty()) {
            return false;
        }
        for (dev.fusionize.workflow.context.WorkflowGraphNodeRecursive parent : node.getParents()) {
            if (isNodeInHistory(parent, targets)) {
                return true;
            }
        }
        return false;
    }

    private void collectFoundAwaitedNodes(dev.fusionize.workflow.context.WorkflowGraphNodeRecursive node,
            List<String> targets, List<String> found) {
        if (targets.contains(node.getNode())) {
            found.add(node.getNode());
        }
        if (node.getParents() != null) {
            for (dev.fusionize.workflow.context.WorkflowGraphNodeRecursive parent : node.getParents()) {
                collectFoundAwaitedNodes(parent, targets, found);
            }
        }
    }
}
