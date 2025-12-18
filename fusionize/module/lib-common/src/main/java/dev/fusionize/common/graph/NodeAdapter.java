package dev.fusionize.common.graph;

import java.util.Collection;

public interface NodeAdapter<N, ID> {
    ID getId(N node);

    Collection<N> getChildren(N node);
    void setChildren(N node, Collection<N> children);

    Collection<ID> getChildrenIds(N node);
    void setChildrenIds(N node, Collection<ID> ids);
}

