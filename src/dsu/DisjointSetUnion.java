package dsu;

import metrics.OperationCounts;

public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] rank;
    private final OperationCounts counts;

    public DisjointSetUnion(int size, OperationCounts counts) {
        if (size < 1) {
            throw new IllegalArgumentException("DSU size must be positive.");
        }
        parent = new int[size];
        rank = new int[size];
        this.counts = counts;
        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    public int find(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IllegalArgumentException("Invalid DSU element: " + vertex);
        }
        counts.findCalls++;
        if (parent[vertex] != vertex) {
            parent[vertex] = find(parent[vertex]);
        }
        return parent[vertex];
    }

    public boolean union(int first, int second) {
        counts.unionCalls++;
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (rank[firstRoot] < rank[secondRoot]) {
            parent[firstRoot] = secondRoot;
        } else if (rank[firstRoot] > rank[secondRoot]) {
            parent[secondRoot] = firstRoot;
        } else {
            parent[secondRoot] = firstRoot;
            rank[firstRoot]++;
        }
        return true;
    }
}
