package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShortestPathResult {
    private final int source;
    private final long[] distances;
    private final int[] previous;

    public ShortestPathResult(int source, long[] distances, int[] previous) {
        this.source = source;
        this.distances = distances.clone();
        this.previous = previous.clone();
    }

    public long distanceTo(int vertex) {
        checkVertex(vertex);
        return distances[vertex];
    }

    public boolean isReachable(int vertex) {
        return distanceTo(vertex) != Long.MAX_VALUE;
    }

    public List<Integer> pathTo(int vertex) {
        checkVertex(vertex);
        if (!isReachable(vertex)) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        int current = vertex;
        while (current != source) {
            path.add(current);
            current = previous[current];
        }
        path.add(source);
        Collections.reverse(path);
        return path;
    }

    private void checkVertex(int vertex) {
        if (vertex < 0 || vertex >= distances.length) {
            throw new IllegalArgumentException("Invalid destination vertex: " + vertex);
        }
    }
}
