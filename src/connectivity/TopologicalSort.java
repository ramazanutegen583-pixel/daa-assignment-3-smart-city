package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import metrics.OperationCounts;

public final class TopologicalSort {
    public static TopologicalResult solve(Graph graph, OperationCounts counts) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological ordering requires a directed graph.");
        }
        int[] indegree = new int[graph.vertexCount()];
        for (Edge edge : graph.edges()) {
            counts.edgeExaminations++;
            indegree[edge.to]++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                counts.edgeExaminations++;
                indegree[edge.to]--;
                if (indegree[edge.to] == 0) {
                    queue.add(edge.to);
                }
            }
        }
        boolean acyclic = order.size() == graph.vertexCount();
        return new TopologicalResult(acyclic, acyclic ? order : List.of());
    }
}
