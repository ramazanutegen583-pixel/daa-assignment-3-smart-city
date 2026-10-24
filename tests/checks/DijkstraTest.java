package checks;

import graph.Edge;
import graph.Graph;
import java.util.List;
import java.util.Random;
import metrics.OperationCounts;
import org.junit.jupiter.api.Test;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;
import static org.junit.jupiter.api.Assertions.*;

class DijkstraTest {
    @Test
    void returnsKnownDistancesAndPaths() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 8);
        graph.addEdge(0, 2, 2);
        graph.addEdge(2, 1, 1);
        graph.addEdge(1, 3, 3);
        graph.addEdge(2, 3, 10);
        ShortestPathResult result = Dijkstra.solve(graph, 0, new OperationCounts());
        assertEquals(3, result.distanceTo(1));
        assertEquals(6, result.distanceTo(3));
        assertEquals(List.of(0, 2, 1, 3), result.pathTo(3));
        assertFalse(result.isReachable(4));
        assertEquals(Long.MAX_VALUE, result.distanceTo(4));
        assertTrue(result.pathTo(4).isEmpty());
    }

    @Test
    void handlesZeroWeightsParallelEdgesAndDifferentSource() {
        Graph graph = new Graph(3, false);
        graph.addEdge(0, 1, 9);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 2, 2);
        graph.addEdge(2, 2, 0);
        ShortestPathResult result = Dijkstra.solve(graph, 2, new OperationCounts());
        assertEquals(2, result.distanceTo(0));
        assertEquals(List.of(2, 1, 0), result.pathTo(0));
        assertEquals(List.of(2), result.pathTo(2));
    }

    @Test
    void handlesSingleVertexAndLongDistances() {
        ShortestPathResult single = Dijkstra.solve(new Graph(1, false), 0, new OperationCounts());
        assertEquals(0, single.distanceTo(0));
        assertEquals(List.of(0), single.pathTo(0));
        Graph graph = new Graph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, Dijkstra.solve(graph, 0, new OperationCounts()).distanceTo(2));
    }

    @Test
    void rejectsNegativeWeightsEvenInUnreachableComponents() {
        Graph graph = new Graph(3, true);
        graph.addEdge(1, 2, -1);
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.solve(graph, 0, new OperationCounts()));
    }

    @Test
    void rejectsInvalidVerticesAndGraphSize() {
        Graph graph = new Graph(2, false);
        assertThrows(IllegalArgumentException.class, () -> new Graph(0, false));
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> Dijkstra.solve(graph, -1, new OperationCounts()));
        ShortestPathResult result = Dijkstra.solve(graph, 0, new OperationCounts());
        assertThrows(IllegalArgumentException.class, () -> result.pathTo(2));
    }

    @Test
    void matchesFloydWarshallOn100RandomGraphsFromEverySource() {
        Random random = new Random(3004);
        for (int trial = 0; trial < 100; trial++) {
            Graph graph = new Graph(7, trial % 2 == 0);
            for (int from = 0; from < 7; from++) {
                for (int to = 0; to < 7; to++) {
                    if (from != to && random.nextInt(5) == 0) {
                        graph.addEdge(from, to, random.nextInt(10));
                    }
                }
            }
            long[][] expected = floydWarshall(graph);
            for (int source = 0; source < 7; source++) {
                ShortestPathResult actual = Dijkstra.solve(graph, source, new OperationCounts());
                for (int destination = 0; destination < 7; destination++) {
                    assertEquals(expected[source][destination], actual.distanceTo(destination));
                    checkPath(graph, source, destination, actual);
                }
            }
        }
    }

    private long[][] floydWarshall(Graph graph) {
        int size = graph.vertexCount();
        long[][] distances = new long[size][size];
        for (int from = 0; from < size; from++) {
            for (int to = 0; to < size; to++) {
                distances[from][to] = from == to ? 0 : Long.MAX_VALUE;
            }
            for (Edge edge : graph.neighbors(from)) {
                distances[from][edge.to] = Math.min(distances[from][edge.to], edge.weight);
            }
        }
        for (int via = 0; via < size; via++) {
            for (int from = 0; from < size; from++) {
                for (int to = 0; to < size; to++) {
                    if (distances[from][via] != Long.MAX_VALUE && distances[via][to] != Long.MAX_VALUE) {
                        distances[from][to] = Math.min(distances[from][to], distances[from][via] + distances[via][to]);
                    }
                }
            }
        }
        return distances;
    }

    private void checkPath(Graph graph, int source, int destination, ShortestPathResult result) {
        List<Integer> path = result.pathTo(destination);
        if (!result.isReachable(destination)) {
            assertTrue(path.isEmpty());
            return;
        }
        assertEquals(source, path.get(0));
        assertEquals(destination, path.get(path.size() - 1));
        assertTrue(path.size() <= graph.vertexCount());
        long total = 0;
        for (int i = 1; i < path.size(); i++) {
            int bestWeight = Integer.MAX_VALUE;
            boolean found = false;
            for (Edge edge : graph.neighbors(path.get(i - 1))) {
                if (edge.to == path.get(i)) {
                    bestWeight = Math.min(bestWeight, edge.weight);
                    found = true;
                }
            }
            assertTrue(found);
            total += bestWeight;
        }
        assertEquals(result.distanceTo(destination), total);
    }
}
