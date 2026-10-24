package connectivity;

import graph.Edge;
import graph.Graph;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import metrics.OperationCounts;

public final class Kosaraju {
    public static List<List<Integer>> solve(Graph graph, OperationCounts counts) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("SCC analysis requires a directed graph.");
        }
        int size = graph.vertexCount();
        boolean[] visited = new boolean[size];
        int[] nextNeighbor = new int[size];
        List<Integer> finishOrder = new ArrayList<>();
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        for (int start = 0; start < size; start++) {
            if (visited[start]) {
                continue;
            }
            stack.push(start);
            visited[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.peek();
                List<Edge> neighbors = graph.neighbors(vertex);
                if (nextNeighbor[vertex] < neighbors.size()) {
                    Edge edge = neighbors.get(nextNeighbor[vertex]++);
                    counts.edgeExaminations++;
                    if (!visited[edge.to]) {
                        visited[edge.to] = true;
                        stack.push(edge.to);
                    }
                } else {
                    finishOrder.add(stack.pop());
                }
            }
        }

        List<List<Integer>> reversed = new ArrayList<>();
        for (int vertex = 0; vertex < size; vertex++) {
            reversed.add(new ArrayList<>());
        }
        for (Edge edge : graph.edges()) {
            counts.edgeExaminations++;
            reversed.get(edge.to).add(edge.from);
        }

        boolean[] assigned = new boolean[size];
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (assigned[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            stack.push(start);
            assigned[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component.add(vertex);
                for (int neighbor : reversed.get(vertex)) {
                    counts.edgeExaminations++;
                    if (!assigned[neighbor]) {
                        assigned[neighbor] = true;
                        stack.push(neighbor);
                    }
                }
            }
            components.add(List.copyOf(component));
        }
        return List.copyOf(components);
    }
}
