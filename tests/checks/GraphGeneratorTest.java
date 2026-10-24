package checks;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import java.util.HashSet;
import java.util.Set;
import metrics.OperationCounts;
import mst.Kruskal;
import mst.Prim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GraphGeneratorTest {
    @Test
    void generatesAllRequiredGraphConfigurations() {
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            Graph connected = GraphGenerator.connectedUndirected(size[0], size[1], 20261024L + size[0]);
            Graph dag = GraphGenerator.dag(size[0], size[1], 20261025L + size[0]);
            Graph cyclic = GraphGenerator.directedCyclic(size[0], size[1], 20261026L + size[0]);
            checkEdges(connected, size[1]);
            checkEdges(dag, size[1]);
            checkEdges(cyclic, size[1]);
            assertTrue(Kruskal.solve(connected, new OperationCounts()).connected);
            assertEquals(Kruskal.solve(connected, new OperationCounts()).totalWeight,
                    Prim.solve(connected, new OperationCounts()).totalWeight);
            assertTrue(TopologicalSort.solve(dag, new OperationCounts()).acyclic);
            assertFalse(TopologicalSort.solve(cyclic, new OperationCounts()).acyclic);
            assertEquals(1, Kosaraju.solve(cyclic, new OperationCounts()).size());
        }
    }

    @Test
    void reproducesTheSameEdgesAndWeightsWithTheSameSeed() {
        assertSameEdges(GraphGenerator.connectedUndirected(10, 30, 123), GraphGenerator.connectedUndirected(10, 30, 123));
        assertSameEdges(GraphGenerator.dag(10, 30, 123), GraphGenerator.dag(10, 30, 123));
        assertSameEdges(GraphGenerator.directedCyclic(10, 30, 123), GraphGenerator.directedCyclic(10, 30, 123));
    }

    @Test
    void acceptsSingleVertexWhenNoCycleIsRequired() {
        assertEquals(0, GraphGenerator.connectedUndirected(1, 0, 1).edgeCount());
        assertEquals(0, GraphGenerator.dag(1, 0, 1).edgeCount());
    }

    @Test
    void rejectsImpossibleEdgeCounts() {
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(5, 3, 1));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(5, 11, 1));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.dag(5, 11, 1));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.directedCyclic(5, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> GraphGenerator.directedCyclic(5, 21, 1));
    }

    private void checkEdges(Graph graph, int expected) {
        assertEquals(expected, graph.edgeCount());
        Set<Long> used = new HashSet<>();
        for (Edge edge : graph.edges()) {
            assertNotEquals(edge.from, edge.to);
            int from = graph.isDirected() ? edge.from : Math.min(edge.from, edge.to);
            int to = graph.isDirected() ? edge.to : Math.max(edge.from, edge.to);
            assertTrue(used.add((long) from * graph.vertexCount() + to));
            assertTrue(edge.weight >= 1 && edge.weight <= 100);
        }
    }

    private void assertSameEdges(Graph first, Graph second) {
        assertEquals(first.edgeCount(), second.edgeCount());
        for (int i = 0; i < first.edgeCount(); i++) {
            Edge a = first.edges().get(i);
            Edge b = second.edges().get(i);
            assertEquals(a.from, b.from);
            assertEquals(a.to, b.to);
            assertEquals(a.weight, b.weight);
        }
    }
}
