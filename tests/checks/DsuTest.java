package checks;

import dsu.DisjointSetUnion;
import metrics.OperationCounts;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DsuTest {
    @Test
    void unionsJoinSetsAndRejectExistingConnections() {
        OperationCounts counts = new OperationCounts();
        DisjointSetUnion dsu = new DisjointSetUnion(5, counts);
        assertTrue(dsu.union(0, 1));
        assertTrue(dsu.union(1, 2));
        assertFalse(dsu.union(0, 2));
        assertEquals(dsu.find(0), dsu.find(2));
        assertNotEquals(dsu.find(0), dsu.find(3));
        assertEquals(3, counts.unionCalls);
    }

    @Test
    void compressionReducesParentVisits() {
        OperationCounts counts = new OperationCounts();
        DisjointSetUnion dsu = new DisjointSetUnion(8, counts);
        dsu.union(0, 1);
        dsu.union(2, 3);
        dsu.union(0, 2);
        dsu.union(4, 5);
        dsu.union(6, 7);
        dsu.union(4, 6);
        dsu.union(0, 4);
        long before = counts.findCalls;
        int root = dsu.find(7);
        long firstVisits = counts.findCalls - before;
        before = counts.findCalls;
        assertEquals(root, dsu.find(7));
        assertTrue(counts.findCalls - before < firstVisits);
    }

    @Test
    void rejectsInvalidSizeAndElements() {
        assertThrows(IllegalArgumentException.class, () -> new DisjointSetUnion(0, new OperationCounts()));
        DisjointSetUnion dsu = new DisjointSetUnion(1, new OperationCounts());
        assertEquals(0, dsu.find(0));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(-1));
        assertThrows(IllegalArgumentException.class, () -> dsu.find(1));
    }
}
