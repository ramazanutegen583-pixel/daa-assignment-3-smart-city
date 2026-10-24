package app;

import connectivity.Kosaraju;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import experiment.Experiment;
import graph.Graph;
import metrics.OperationCounts;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

public final class Main {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "demo" : args[0];
        if (mode.equals("experiments")) {
            Experiment.run();
        } else if (mode.equals("demo")) {
            int source = args.length > 1 ? Integer.parseInt(args[1]) : 0;
            runDemo(source);
        } else {
            throw new IllegalArgumentException("Usage: demo [source vertex 0-6] | experiments");
        }
    }

    private static void runDemo(int source) {
        System.out.println("Smart City Infrastructure and Route Planning");
        Graph city = new Graph(6, false);
        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 2);
        city.addEdge(1, 2, 1);
        city.addEdge(1, 3, 5);
        city.addEdge(2, 3, 8);
        city.addEdge(2, 4, 10);
        city.addEdge(3, 4, 2);
        city.addEdge(3, 5, 6);
        city.addEdge(4, 5, 3);
        printMst("Kruskal", Kruskal.solve(city, new OperationCounts()));
        printMst("Prim", Prim.solve(city, new OperationCounts()));

        Graph services = new Graph(6, true);
        services.addEdge(0, 2, 1);
        services.addEdge(1, 2, 1);
        services.addEdge(2, 3, 1);
        services.addEdge(2, 4, 1);
        services.addEdge(4, 5, 1);
        TopologicalResult dagOrder = TopologicalSort.solve(services, new OperationCounts());
        System.out.println("\nService dependency order (DAG): " + dagOrder.order);
        services.addEdge(5, 2, 1);
        TopologicalResult cyclicOrder = TopologicalSort.solve(services, new OperationCounts());
        System.out.println("Topological order after adding a cycle: "
                + (cyclicOrder.acyclic ? cyclicOrder.order : "impossible: cycle detected"));
        System.out.println("Strongly connected components: " + Kosaraju.solve(services, new OperationCounts()));

        Graph roads = new Graph(7, true);
        roads.addEdge(0, 1, 4);
        roads.addEdge(0, 2, 2);
        roads.addEdge(2, 1, 1);
        roads.addEdge(1, 3, 5);
        roads.addEdge(2, 3, 8);
        roads.addEdge(3, 4, 2);
        roads.addEdge(4, 5, 3);
        ShortestPathResult routes = Dijkstra.solve(roads, source, new OperationCounts());
        System.out.println("\nShortest routes from district " + source + ":");
        for (int district = 0; district < roads.vertexCount(); district++) {
            if (routes.isReachable(district)) {
                System.out.println("District " + district + ": distance=" + routes.distanceTo(district)
                        + ", path=" + routes.pathTo(district));
            } else {
                System.out.println("District " + district + ": unreachable");
            }
        }
    }

    private static void printMst(String name, MstResult result) {
        System.out.println("\n" + name + ": connected=" + result.connected + ", total weight=" + result.totalWeight);
        result.edges.forEach(System.out::println);
    }
}
