package dev.fusionize.common.graph;

import java.util.Map;
import java.util.Set;

public record FlattenResult<N, ID>(
        Map<ID, N> nodeMap,
        Set<ID> rootIds
) {}
