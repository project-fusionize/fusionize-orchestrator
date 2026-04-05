package dev.fusionize.common.graph;

import java.util.Collection;

public interface NodeAdapter<N, ID> {
    ID getId(N node);

    Collection<N> getChildren(N node);
    void setChildren(N node, Collection<N> children);

    Collection<ID> getChildrenIds(N node);
    void setChildrenIds(N node, Collection<ID> ids);

    default Collection<N> getSecondaryChildren(N node) { return null; }
    default void setSecondaryChildren(N node, Collection<N> children) {}

    default Collection<ID> getSecondaryChildrenIds(N node) { return null; }
    default void setSecondaryChildrenIds(N node, Collection<ID> ids) {}
}

