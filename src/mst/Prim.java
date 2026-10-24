package mst;

import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import metrics.OperationCounts;

public final class Prim {
    public static MstResult solve(Graph graph, OperationCounts counts) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph.");
        }
        boolean[] visited = new boolean[graph.vertexCount()];
        List<Edge> chosen = new ArrayList<>();
        PriorityQueue<Edge> queue = new PriorityQueue<>((first, second) -> {
            counts.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });
        long totalWeight = 0;
        int components = 0;
        for (int root = 0; root < graph.vertexCount(); root++) {
            if (visited[root]) {
                continue;
            }
            components++;
            visited[root] = true;
            addCandidates(graph, root, visited, queue, counts);
            while (!queue.isEmpty()) {
                Edge edge = queue.poll();
                if (visited[edge.to]) {
                    continue;
                }
                visited[edge.to] = true;
                chosen.add(edge);
                totalWeight += edge.weight;
                addCandidates(graph, edge.to, visited, queue, counts);
            }
        }
        return new MstResult(chosen, totalWeight, components == 1);
    }

    private static void addCandidates(Graph graph, int vertex, boolean[] visited,
                                      PriorityQueue<Edge> queue, OperationCounts counts) {
        for (Edge edge : graph.neighbors(vertex)) {
            counts.edgeExaminations++;
            if (!visited[edge.to]) {
                queue.add(edge);
            }
        }
    }
}
