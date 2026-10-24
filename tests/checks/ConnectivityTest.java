package checks;

import connectivity.Kosaraju;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import metrics.OperationCounts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectivityTest {
    @Test
    void ordersDisconnectedDag() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(4, 5, 1);
        checkTopologicalOrder(graph);
    }

    @Test
    void validates100RandomDags() {
        Random random = new Random(3002);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(12, true);
            for (int from = 0; from < 12; from++) {
                for (int to = from + 1; to < 12; to++) {
                    if (random.nextInt(4) == 0) {
                        graph.addEdge(from, to, 1);
                    }
                }
            }
            checkTopologicalOrder(graph);
        }
    }

    @Test
    void detectsCycleEvenWithAnAcyclicComponent() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(3, 4, 1);
        TopologicalResult result = TopologicalSort.solve(graph, new OperationCounts());
        assertFalse(result.acyclic);
        assertTrue(result.order.isEmpty());
    }

    @Test
    void detectsSelfLoop() {
        Graph graph = new Graph(1, true);
        graph.addEdge(0, 0, 1);
        assertFalse(TopologicalSort.solve(graph, new OperationCounts()).acyclic);
        checkScc(graph);
    }

    @Test
    void findsKnownComponentsAndIsolatedVertex() {
        Graph graph = new Graph(6, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        assertEquals(3, Kosaraju.solve(graph, new OperationCounts()).size());
        checkScc(graph);
    }

    @Test
    void matchesMutualReachabilityOn100RandomGraphs() {
        Random random = new Random(3003);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(8, true);
            for (int from = 0; from < 8; from++) {
                for (int to = 0; to < 8; to++) {
                    if (random.nextInt(4) == 0) {
                        graph.addEdge(from, to, 1);
                    }
                }
            }
            checkScc(graph);
        }
    }

    @Test
    void handlesSingleVertexAndLongChain() {
        Graph single = new Graph(1, true);
        checkTopologicalOrder(single);
        checkScc(single);
        Graph chain = new Graph(1000, true);
        for (int i = 1; i < 1000; i++) {
            chain.addEdge(i - 1, i, 1);
        }
        checkTopologicalOrder(chain);
        assertEquals(1000, Kosaraju.solve(chain, new OperationCounts()).size());
    }

    @Test
    void rejectsUndirectedInput() {
        Graph graph = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> Kosaraju.solve(graph, new OperationCounts()));
        assertThrows(IllegalArgumentException.class, () -> TopologicalSort.solve(graph, new OperationCounts()));
    }

    private void checkTopologicalOrder(Graph graph) {
        TopologicalResult result = TopologicalSort.solve(graph, new OperationCounts());
        assertTrue(result.acyclic);
        assertEquals(graph.vertexCount(), result.order.size());
        int[] position = new int[graph.vertexCount()];
        Arrays.fill(position, -1);
        for (int i = 0; i < result.order.size(); i++) {
            int vertex = result.order.get(i);
            assertEquals(-1, position[vertex]);
            position[vertex] = i;
        }
        for (Edge edge : graph.edges()) {
            assertTrue(position[edge.from] < position[edge.to]);
        }
    }

    private void checkScc(Graph graph) {
        int size = graph.vertexCount();
        boolean[][] reach = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            reach[i][i] = true;
        }
        for (Edge edge : graph.edges()) {
            reach[edge.from][edge.to] = true;
        }
        for (int via = 0; via < size; via++) {
            for (int from = 0; from < size; from++) {
                for (int to = 0; to < size; to++) {
                    reach[from][to] |= reach[from][via] && reach[via][to];
                }
            }
        }
        List<List<Integer>> components = Kosaraju.solve(graph, new OperationCounts());
        int[] owner = new int[size];
        Arrays.fill(owner, -1);
        for (int group = 0; group < components.size(); group++) {
            assertFalse(components.get(group).isEmpty());
            for (int vertex : components.get(group)) {
                assertEquals(-1, owner[vertex]);
                owner[vertex] = group;
            }
        }
        for (int first = 0; first < size; first++) {
            assertNotEquals(-1, owner[first]);
            for (int second = 0; second < size; second++) {
                assertEquals(reach[first][second] && reach[second][first], owner[first] == owner[second]);
            }
        }
    }
}
