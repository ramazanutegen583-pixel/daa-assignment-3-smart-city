package shortestpath;

import graph.Edge;
import graph.Graph;
import java.util.Arrays;
import java.util.PriorityQueue;
import metrics.OperationCounts;

public final class Dijkstra {
    private static final class Entry {
        final int vertex;
        final long distance;

        Entry(int vertex, long distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }

    public static ShortestPathResult solve(Graph graph, int source, OperationCounts counts) {
        graph.checkVertex(source);
        for (Edge edge : graph.edges()) {
            counts.edgeExaminations++;
            if (edge.weight < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights.");
            }
        }
        long[] distances = new long[graph.vertexCount()];
        int[] previous = new int[graph.vertexCount()];
        Arrays.fill(distances, Long.MAX_VALUE);
        Arrays.fill(previous, -1);
        distances[source] = 0;
        PriorityQueue<Entry> queue = new PriorityQueue<>((first, second) -> {
            counts.comparisons++;
            return Long.compare(first.distance, second.distance);
        });
        queue.add(new Entry(source, 0));
        while (!queue.isEmpty()) {
            Entry current = queue.poll();
            if (current.distance != distances[current.vertex]) {
                continue;
            }
            for (Edge edge : graph.neighbors(current.vertex)) {
                counts.edgeExaminations++;
                long candidate = current.distance + edge.weight;
                if (candidate < distances[edge.to]) {
                    distances[edge.to] = candidate;
                    previous[edge.to] = current.vertex;
                    queue.add(new Entry(edge.to, candidate));
                }
            }
        }
        return new ShortestPathResult(source, distances, previous);
    }
}
