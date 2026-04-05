package dev.fusionize.common.graph;

import java.util.*;

public final class GraphUtil {

    private GraphUtil() {}

    /* ===================== FLATTEN ===================== */

    public static <N, ID> FlattenResult<N, ID> flatten(
            Collection<N> roots,
            NodeAdapter<N, ID> adapter
    ) {
        Map<ID, N> nodeMap = new LinkedHashMap<>();
        Set<ID> rootIds = new LinkedHashSet<>();

        for (N root : roots) {
            ID rootId = adapter.getId(root);
            rootIds.add(rootId);
            flattenNode(root, adapter, nodeMap);
        }

        return new FlattenResult<>(nodeMap, rootIds);
    }

    private static <N, ID> void flattenNode(
            N node,
            NodeAdapter<N, ID> adapter,
            Map<ID, N> nodeMap
    ) {
        ID id = adapter.getId(node);

        if (nodeMap.containsKey(id)) {
            return; // cycle / already visited
        }

        nodeMap.put(id, node);

        Collection<ID> childIds = new ArrayList<>();
        Collection<N> children = adapter.getChildren(node);

        if (children != null) {
            for (N child : children) {
                childIds.add(adapter.getId(child));
                flattenNode(child, adapter, nodeMap);
            }
        }

        adapter.setChildrenIds(node, childIds);

        // Flatten secondary edges (e.g. compensation nodes)
        Collection<N> secondaryChildren = adapter.getSecondaryChildren(node);
        if (secondaryChildren != null && !secondaryChildren.isEmpty()) {
            Collection<ID> secondaryIds = new ArrayList<>();
            for (N secondary : secondaryChildren) {
                secondaryIds.add(adapter.getId(secondary));
                flattenNode(secondary, adapter, nodeMap);
            }
            adapter.setSecondaryChildrenIds(node, secondaryIds);
        }
    }

    /* ===================== INFLATE ===================== */

    public static <N, ID> Collection<N> inflate(
            Map<ID, N> nodeMap,
            Collection<ID> rootIds,
            NodeAdapter<N, ID> adapter
    ) {
        // Link children and secondary children
        for (N node : nodeMap.values()) {
            Collection<N> children = new ArrayList<>();
            Collection<ID> childIds = adapter.getChildrenIds(node);

            if (childIds != null) {
                for (ID childId : childIds) {
                    N child = nodeMap.get(childId);
                    if (child != null) {
                        children.add(child);
                    }
                }
            }

            adapter.setChildren(node, children);

            Collection<ID> secondaryIds = adapter.getSecondaryChildrenIds(node);
            if (secondaryIds != null && !secondaryIds.isEmpty()) {
                Collection<N> secondaryChildren = new ArrayList<>();
                for (ID secId : secondaryIds) {
                    N secChild = nodeMap.get(secId);
                    if (secChild != null) {
                        secondaryChildren.add(secChild);
                    }
                }
                adapter.setSecondaryChildren(node, secondaryChildren);
            }
        }

        // Resolve roots
        List<N> roots = new ArrayList<>();
        for (ID rootId : rootIds) {
            N root = nodeMap.get(rootId);
            if (root != null) {
                roots.add(root);
            }
        }

        return roots;
    }
}

