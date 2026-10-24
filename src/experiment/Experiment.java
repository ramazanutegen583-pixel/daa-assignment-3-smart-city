package experiment;

import connectivity.Kosaraju;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.Graph;
import graph.GraphGenerator;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import metrics.OperationCounts;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

public final class Experiment {
    public static final long SEED = 20261024L;
    public static final int WARM_UP_RUNS = 3;
    public static final int TRIALS = 5;

    static final class Summary {
        final String algorithm;
        final String graphType;
        final int vertices;
        final int edges;
        final long seed;
        final double averageNs;
        final double averageEdges;
        final double averageComparisons;
        final double averageFinds;
        final double averageUnions;
        final long result;

        Summary(String algorithm, String graphType, Graph graph, long seed, List<Trial> trials) {
            this.algorithm = algorithm;
            this.graphType = graphType;
            vertices = graph.vertexCount();
            edges = graph.edgeCount();
            this.seed = seed;
            averageNs = trials.stream().mapToLong(trial -> trial.elapsedNs).average().orElseThrow();
            averageEdges = trials.stream().mapToLong(trial -> trial.counts.edgeExaminations).average().orElseThrow();
            averageComparisons = trials.stream().mapToLong(trial -> trial.counts.comparisons).average().orElseThrow();
            averageFinds = trials.stream().mapToLong(trial -> trial.counts.findCalls).average().orElseThrow();
            averageUnions = trials.stream().mapToLong(trial -> trial.counts.unionCalls).average().orElseThrow();
            result = trials.get(0).result;
        }
    }

    private static final class Trial {
        final long elapsedNs;
        final OperationCounts counts;
        final long result;

        Trial(long elapsedNs, OperationCounts counts, long result) {
            this.elapsedNs = elapsedNs;
            this.counts = counts;
            this.result = result;
        }
    }

    public static void run() throws IOException {
        Path tables = Path.of("results", "tables");
        Path plots = Path.of("results", "plots");
        Files.createDirectories(tables);
        Files.createDirectories(plots);
        List<Summary> summaries = new ArrayList<>();
        System.out.println("Graph experiments: 3 warm-up runs, 5 measured trials per case");
        try (PrintWriter raw = writer(tables.resolve("trials.csv"))) {
            raw.println("algorithm,graph_type,vertices,edges,seed,trial,elapsed_ns,edge_examinations,comparisons,find_calls,union_calls,result_signature");
            int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
            for (int[] size : sizes) {
                int vertices = size[0];
                int edges = size[1];
                long graphSeed = SEED + vertices;
                Graph undirected = GraphGenerator.connectedUndirected(vertices, edges, graphSeed);
                Graph dag = GraphGenerator.dag(vertices, edges, graphSeed + 1);
                Graph cyclic = GraphGenerator.directedCyclic(vertices, edges, graphSeed + 2);
                Summary kruskal = runCase("Kruskal", "undirected-connected", undirected, graphSeed, raw);
                Summary prim = runCase("Prim", "undirected-connected", undirected, graphSeed, raw);
                if (kruskal.result != prim.result) {
                    throw new IllegalStateException("MST weights disagree.");
                }
                summaries.add(kruskal);
                summaries.add(prim);
                summaries.add(runCase("SCC", "directed-cyclic", cyclic, graphSeed + 2, raw));
                summaries.add(runCase("Topological-DAG", "directed-dag", dag, graphSeed + 1, raw));
                summaries.add(runCase("Topological-Cyclic", "directed-cyclic", cyclic, graphSeed + 2, raw));
                summaries.add(runCase("Dijkstra", "undirected-connected", undirected, graphSeed, raw));
            }
        }
        writeSummaries(tables, summaries);
        writeEnvironment(tables.resolve("environment.txt"));
        PlotWriter.write(plots.resolve("time-vs-vertices.svg"), summaries, true);
        PlotWriter.write(plots.resolve("time-vs-edges.svg"), summaries, false);
        System.out.printf("%-21s %6s %7s %14s %12s %12s%n", "Algorithm", "V", "E", "Average ns", "Edge exams", "Comparisons");
        for (Summary summary : summaries) {
            System.out.printf(Locale.US, "%-21s %6d %7d %14.1f %12.0f %12.0f%n",
                    summary.algorithm, summary.vertices, summary.edges, summary.averageNs,
                    summary.averageEdges, summary.averageComparisons);
        }
        System.out.println("Saved CSV files, Markdown tables, environment details, and SVG plots in results/.");
    }

    private static Summary runCase(String algorithm, String graphType, Graph graph, long seed,
                                   PrintWriter raw) {
        for (int warmup = 0; warmup < WARM_UP_RUNS; warmup++) {
            execute(algorithm, graph);
        }
        List<Trial> trials = new ArrayList<>();
        for (int trialNumber = 1; trialNumber <= TRIALS; trialNumber++) {
            Trial trial = execute(algorithm, graph);
            if (!trials.isEmpty() && trial.result != trials.get(0).result) {
                throw new IllegalStateException("The algorithm returned inconsistent results.");
            }
            trials.add(trial);
            raw.printf(Locale.US, "%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d%n",
                    algorithm, graphType, graph.vertexCount(), graph.edgeCount(), seed, trialNumber,
                    trial.elapsedNs, trial.counts.edgeExaminations, trial.counts.comparisons,
                    trial.counts.findCalls, trial.counts.unionCalls, trial.result);
        }
        return new Summary(algorithm, graphType, graph, seed, trials);
    }

    private static Trial execute(String algorithm, Graph graph) {
        OperationCounts counts = new OperationCounts();
        long start;
        long elapsed;
        switch (algorithm) {
            case "Kruskal":
            case "Prim":
                start = System.nanoTime();
                MstResult mst = algorithm.equals("Kruskal") ? Kruskal.solve(graph, counts) : Prim.solve(graph, counts);
                elapsed = System.nanoTime() - start;
                if (!mst.connected || mst.edges.size() != graph.vertexCount() - 1) {
                    throw new IllegalStateException("The generated MST graph is not connected.");
                }
                return new Trial(elapsed, counts, mst.totalWeight);
            case "SCC":
                start = System.nanoTime();
                List<List<Integer>> components = Kosaraju.solve(graph, counts);
                elapsed = System.nanoTime() - start;
                if (components.size() != 1 || components.get(0).size() != graph.vertexCount()) {
                    throw new IllegalStateException("The spanning directed cycle must form one SCC.");
                }
                return new Trial(elapsed, counts, components.size());
            case "Topological-DAG":
            case "Topological-Cyclic":
                start = System.nanoTime();
                TopologicalResult ordering = TopologicalSort.solve(graph, counts);
                elapsed = System.nanoTime() - start;
                if (ordering.acyclic != algorithm.equals("Topological-DAG")) {
                    throw new IllegalStateException("Unexpected cycle detection result.");
                }
                if (ordering.acyclic) {
                    verifyOrder(graph, ordering.order);
                }
                return new Trial(elapsed, counts, ordering.acyclic ? ordering.order.size() : -1);
            case "Dijkstra":
                start = System.nanoTime();
                ShortestPathResult routes = Dijkstra.solve(graph, 0, counts);
                elapsed = System.nanoTime() - start;
                long signature = 0;
                for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                    if (!routes.isReachable(vertex)) {
                        throw new IllegalStateException("Connected graphs must have a route to every vertex.");
                    }
                    signature += (vertex + 1L) * routes.distanceTo(vertex);
                }
                return new Trial(elapsed, counts, signature);
            default:
                throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        }
    }

    private static void verifyOrder(Graph graph, List<Integer> order) {
        int[] position = new int[graph.vertexCount()];
        for (int i = 0; i < order.size(); i++) {
            position[order.get(i)] = i;
        }
        for (Edge edge : graph.edges()) {
            if (position[edge.from] >= position[edge.to]) {
                throw new IllegalStateException("Invalid topological ordering.");
            }
        }
    }

    private static void writeSummaries(Path tables, List<Summary> summaries) throws IOException {
        try (PrintWriter csv = writer(tables.resolve("summary.csv"));
             PrintWriter markdown = writer(tables.resolve("results.md"))) {
            csv.println("algorithm,graph_type,vertices,edges,seed,trials,average_ns,average_edge_examinations,average_comparisons,average_find_calls,average_union_calls,result_signature");
            markdown.println("| Algorithm | V | E | Average time (microseconds) | Edge examinations | Comparisons | Find calls | Union calls |");
            markdown.println("|---|---:|---:|---:|---:|---:|---:|---:|");
            for (Summary row : summaries) {
                csv.printf(Locale.US, "%s,%s,%d,%d,%d,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%d%n",
                        row.algorithm, row.graphType, row.vertices, row.edges, row.seed, TRIALS, row.averageNs,
                        row.averageEdges, row.averageComparisons, row.averageFinds, row.averageUnions, row.result);
                markdown.printf(Locale.US, "| %s | %d | %d | %.3f | %.0f | %.0f | %.0f | %.0f |%n",
                        row.algorithm, row.vertices, row.edges, row.averageNs / 1000.0,
                        row.averageEdges, row.averageComparisons, row.averageFinds, row.averageUnions);
            }
        }
    }

    private static void writeEnvironment(Path path) throws IOException {
        try (PrintWriter output = writer(path)) {
            output.println("Java: " + System.getProperty("java.version"));
            output.println("Java vendor: " + System.getProperty("java.vendor"));
            output.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            output.println("Architecture: " + System.getProperty("os.arch"));
            output.println("Available logical processors: " + Runtime.getRuntime().availableProcessors());
            output.println("Base seed: " + SEED);
            output.println("Graph sizes (V,E): (10,30), (100,500), (1000,5000)");
            output.println("Warm-up runs per case: " + WARM_UP_RUNS);
            output.println("Measured trials per case: " + TRIALS);
            output.println("Timing: System.nanoTime(), arithmetic mean, operation counting enabled");
            output.println("Graph generation and result verification are outside the timed interval.");
            output.println("Source for Dijkstra experiments: 0");
        }
    }

    private static PrintWriter writer(Path path) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }
}
