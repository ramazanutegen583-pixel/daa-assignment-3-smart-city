package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.Graph;
import java.util.ArrayList;
import java.util.List;
import metrics.OperationCounts;

public final class Kruskal {
    public static MstResult solve(Graph graph, OperationCounts counts) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("MST requires an undirected graph.");
        }
        List<Edge> sortedEdges = new ArrayList<>(graph.edges());
        sortedEdges.sort((first, second) -> {
            counts.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });
        DisjointSetUnion dsu = new DisjointSetUnion(graph.vertexCount(), counts);
        List<Edge> chosen = new ArrayList<>();
        long totalWeight = 0;
        for (Edge edge : sortedEdges) {
            if (chosen.size() == graph.vertexCount() - 1) {
                break;
            }
            counts.edgeExaminations++;
            if (dsu.union(edge.from, edge.to)) {
                chosen.add(edge);
                totalWeight += edge.weight;
            }
        }
        return new MstResult(chosen, totalWeight, chosen.size() == graph.vertexCount() - 1);
    }
}
