package checks;

import graph.Edge;
import graph.Graph;
import java.util.List;
import java.util.Random;
import metrics.OperationCounts;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MstTest {
    @Test
    void findsKnownMst() {
        Graph graph = new Graph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(0, 3, 10);
        graph.addEdge(0, 2, 5);
        checkBoth(graph, 6, true);
    }

    @Test
    void handlesSeveralMstsWithEqualWeights() {
        Graph graph = new Graph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 0, 1);
        graph.addEdge(0, 2, 2);
        checkBoth(graph, 3, true);
    }

    @Test
    void reportsDisconnectedForestIncludingIsolatedVertices() {
        Graph graph = new Graph(5, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(2, 3, 2);
        checkBoth(graph, 6, false);
        assertEquals(2, Kruskal.solve(graph, new OperationCounts()).edges.size());
        assertEquals(2, Prim.solve(graph, new OperationCounts()).edges.size());
    }

    @Test
    void handlesSingleVertexAndNoEdges() {
        checkBoth(new Graph(1, false), 0, true);
        checkBoth(new Graph(3, false), 0, false);
    }

    @Test
    void handlesParallelEdgesSelfLoopsAndNegativeCosts() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 1, -2);
        graph.addEdge(1, 2, 0);
        graph.addEdge(0, 2, 4);
        checkBoth(graph, -2, true);
    }

    @Test
    void matchesExhaustiveSpanningTreeSearchOn100RandomGraphs() {
        Random random = new Random(3001);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(5, false);
            for (int vertex = 1; vertex < 5; vertex++) {
                graph.addEdge(vertex - 1, vertex, random.nextInt(7));
            }
            for (int from = 0; from < 5; from++) {
                for (int to = from + 2; to < 5; to++) {
                    if (random.nextBoolean()) {
                        graph.addEdge(from, to, random.nextInt(7));
                    }
                }
            }
            checkBoth(graph, bruteForceWeight(graph), true);
        }
    }

    @Test
    void rejectsDirectedInput() {
        Graph graph = new Graph(2, true);
        graph.addEdge(0, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> Kruskal.solve(graph, new OperationCounts()));
        assertThrows(IllegalArgumentException.class, () -> Prim.solve(graph, new OperationCounts()));
    }

    private void checkBoth(Graph graph, long expected, boolean connected) {
        MstResult first = Kruskal.solve(graph, new OperationCounts());
        MstResult second = Prim.solve(graph, new OperationCounts());
        assertEquals(expected, first.totalWeight);
        assertEquals(expected, second.totalWeight);
        assertEquals(connected, first.connected);
        assertEquals(connected, second.connected);
        if (connected) {
            assertTree(graph.vertexCount(), first.edges);
            assertTree(graph.vertexCount(), second.edges);
        }
    }

    private void assertTree(int size, List<Edge> edges) {
        assertEquals(size - 1, edges.size());
        boolean[][] reach = new boolean[size][size];
        for (int i = 0; i < size; i++) {
            reach[i][i] = true;
        }
        for (Edge edge : edges) {
            reach[edge.from][edge.to] = true;
            reach[edge.to][edge.from] = true;
        }
        closeReachability(reach);
        for (int i = 0; i < size; i++) {
            assertTrue(reach[0][i]);
        }
    }

    private long bruteForceWeight(Graph graph) {
        long best = Long.MAX_VALUE;
        List<Edge> edges = graph.edges();
        int size = graph.vertexCount();
        for (int mask = 0; mask < (1 << edges.size()); mask++) {
            if (Integer.bitCount(mask) != size - 1) {
                continue;
            }
            boolean[][] reach = new boolean[size][size];
            long weight = 0;
            for (int i = 0; i < size; i++) {
                reach[i][i] = true;
            }
            for (int i = 0; i < edges.size(); i++) {
                if ((mask & (1 << i)) != 0) {
                    Edge edge = edges.get(i);
                    reach[edge.from][edge.to] = reach[edge.to][edge.from] = true;
                    weight += edge.weight;
                }
            }
            closeReachability(reach);
            boolean connected = true;
            for (int i = 0; i < size; i++) {
                connected &= reach[0][i];
            }
            if (connected) {
                best = Math.min(best, weight);
            }
        }
        return best;
    }

    private void closeReachability(boolean[][] reach) {
        for (int via = 0; via < reach.length; via++) {
            for (int from = 0; from < reach.length; from++) {
                for (int to = 0; to < reach.length; to++) {
                    reach[from][to] |= reach[from][via] && reach[via][to];
                }
            }
        }
    }
}
