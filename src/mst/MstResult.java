package mst;

import graph.Edge;
import java.util.List;

public final class MstResult {
    public final List<Edge> edges;
    public final long totalWeight;
    public final boolean connected;

    public MstResult(List<Edge> edges, long totalWeight, boolean connected) {
        this.edges = List.copyOf(edges);
        this.totalWeight = totalWeight;
        this.connected = connected;
    }
}
