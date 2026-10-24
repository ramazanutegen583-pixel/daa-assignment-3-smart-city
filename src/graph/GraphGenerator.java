package graph;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    public static Graph connectedUndirected(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1) / 2;
        if (vertices < 1 || edges < vertices - 1 || edges > maximum) {
            throw new IllegalArgumentException("Invalid connected graph size.");
        }
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 1; vertex < vertices; vertex++) {
            int parent = random.nextInt(vertex);
            addUnique(graph, parent, vertex, random, used);
        }
        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            if (from != to) {
                addUnique(graph, Math.min(from, to), Math.max(from, to), random, used);
            }
        }
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1) / 2;
        if (vertices < 1 || edges < 0 || edges > maximum) {
            throw new IllegalArgumentException("Invalid DAG size.");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        while (graph.edgeCount() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second) {
                addUnique(graph, Math.min(first, second), Math.max(first, second), random, used);
            }
        }
        return graph;
    }

    public static Graph directedCyclic(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1);
        if (vertices < 2 || edges < vertices || edges > maximum) {
            throw new IllegalArgumentException("A cyclic graph needs at least two vertices and a spanning cycle.");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            addUnique(graph, vertex, (vertex + 1) % vertices, random, used);
        }
        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            if (from != to) {
                addUnique(graph, from, to, random, used);
            }
        }
        return graph;
    }

    private static void addUnique(Graph graph, int from, int to, Random random, Set<Long> used) {
        long key = (long) from * graph.vertexCount() + to;
        if (used.add(key)) {
            graph.addEdge(from, to, 1 + random.nextInt(100));
        }
    }
}
