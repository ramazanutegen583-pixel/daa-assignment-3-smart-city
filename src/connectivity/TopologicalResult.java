package connectivity;

import java.util.List;

public final class TopologicalResult {
    public final boolean acyclic;
    public final List<Integer> order;

    public TopologicalResult(boolean acyclic, List<Integer> order) {
        this.acyclic = acyclic;
        this.order = List.copyOf(order);
    }
}
