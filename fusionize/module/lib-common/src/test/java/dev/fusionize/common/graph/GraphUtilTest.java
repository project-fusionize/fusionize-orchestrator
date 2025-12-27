package dev.fusionize.common.graph;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GraphUtilTest {
    static class TestNode {
        private final String id;
        private List<TestNode> children = new ArrayList<>();
        private List<String> childrenIds = new ArrayList<>();

        TestNode(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }

        List<TestNode> getChildren() {
            return children;
        }

        void setChildren(Collection<TestNode> children) {
            this.children = new ArrayList<>(children);
        }

        List<String> getChildrenIds() {
            return childrenIds;
        }

        void setChildrenIds(Collection<String> ids) {
            this.childrenIds = new ArrayList<>(ids);
        }
    }

    private static final NodeAdapter<TestNode, String> ADAPTER =
            new NodeAdapter<>() {
                @Override
                public String getId(TestNode node) {
                    return node.getId();
                }

                @Override
                public Collection<TestNode> getChildren(TestNode node) {
                    return node.getChildren();
                }

                @Override
                public void setChildren(TestNode node, Collection<TestNode> children) {
                    node.setChildren(children);
                }

                @Override
                public Collection<String> getChildrenIds(TestNode node) {
                    return node.getChildrenIds();
                }

                @Override
                public void setChildrenIds(TestNode node, Collection<String> ids) {
                    node.setChildrenIds(ids);
                }
            };
    @Test
    void flatten_simpleTree() {
        TestNode root = node("A",
                node("B"),
                node("C")
        );

        FlattenResult<TestNode, String> result =
                GraphUtil.flatten(List.of(root), ADAPTER);

        assertEquals(Set.of("A"), result.rootIds());
        assertEquals(Set.of("A", "B", "C"), result.nodeMap().keySet());

        TestNode a = result.nodeMap().get("A");
        assertEquals(List.of("B", "C"), a.getChildrenIds());
    }

    @Test
    void inflate_simpleTree() {
        TestNode a = new TestNode("A");
        TestNode b = new TestNode("B");
        TestNode c = new TestNode("C");

        a.setChildrenIds(List.of("B", "C"));

        Map<String, TestNode> map = Map.of(
                "A", a,
                "B", b,
                "C", c
        );

        Collection<TestNode> roots =
                GraphUtil.inflate(map, List.of("A"), ADAPTER);

        TestNode root = roots.iterator().next();
        assertEquals(2, root.getChildren().size());
        assertEquals(Set.of("B", "C"),
                ids(root.getChildren()));
    }

    @Test
    void flatten_multipleRoots() {
        TestNode a = node("A");
        TestNode b = node("B");

        FlattenResult<TestNode, String> result =
                GraphUtil.flatten(List.of(a, b), ADAPTER);

        assertEquals(Set.of("A", "B"), result.rootIds());
        assertEquals(2, result.nodeMap().size());
    }

    @Test
    void flatten_dag_sharedChild() {
        TestNode c = node("C");
        TestNode a = node("A", c);
        TestNode b = node("B", c);

        FlattenResult<TestNode, String> result =
                GraphUtil.flatten(List.of(a, b), ADAPTER);

        assertEquals(Set.of("A", "B", "C"), result.nodeMap().keySet());

        assertEquals(List.of("C"), result.nodeMap().get("A").getChildrenIds());
        assertEquals(List.of("C"), result.nodeMap().get("B").getChildrenIds());
    }

    @Test
    void flatten_cycle_doesNotInfiniteLoop() {
        TestNode a = new TestNode("A");
        TestNode b = new TestNode("B");

        a.setChildren(List.of(b));
        b.setChildren(List.of(a)); // cycle

        FlattenResult<TestNode, String> result =
                GraphUtil.flatten(List.of(a), ADAPTER);

        assertEquals(Set.of("A", "B"), result.nodeMap().keySet());
        assertEquals(List.of("B"), result.nodeMap().get("A").getChildrenIds());
        assertEquals(List.of("A"), result.nodeMap().get("B").getChildrenIds());
    }

    @Test
    void roundTrip_flattenThenInflate_preservesStructure() {
        TestNode root = node("A",
                node("B",
                        node("C")
                ),
                node("D")
        );

        FlattenResult<TestNode, String> flat =
                GraphUtil.flatten(List.of(root), ADAPTER);

        Collection<TestNode> inflated =
                GraphUtil.inflate(
                        flat.nodeMap(),
                        flat.rootIds(),
                        ADAPTER
                );

        TestNode inflatedRoot = inflated.iterator().next();

        assertEquals("A", inflatedRoot.getId());
        assertEquals(2, inflatedRoot.getChildren().size());

        TestNode b = inflatedRoot.getChildren().stream()
                .filter(n -> n.getId().equals("B"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, b.getChildren().size());
        assertEquals("C", b.getChildren().getFirst().getId());
    }

    /* ===================== helpers ===================== */

    private static TestNode node(String id, TestNode... children) {
        TestNode n = new TestNode(id);
        n.setChildren(List.of(children));
        return n;
    }

    private static Set<String> ids(Collection<TestNode> nodes) {
        Set<String> ids = new HashSet<>();
        for (TestNode n : nodes) {
            ids.add(n.getId());
        }
        return ids;
    }


}