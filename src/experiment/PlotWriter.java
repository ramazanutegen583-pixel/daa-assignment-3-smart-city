package experiment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class PlotWriter {
    private static final String[] COLORS = {"#2563eb", "#dc2626", "#15803d", "#9333ea", "#b45309", "#0891b2"};
    private static final int LEFT = 95;
    private static final int TOP = 70;
    private static final int WIDTH = 630;
    private static final int HEIGHT = 440;

    static void write(Path path, List<Experiment.Summary> rows, boolean byVertices) throws IOException {
        double minimumX = rows.stream().mapToDouble(row -> byVertices ? row.vertices : row.edges).min().orElseThrow();
        double maximumX = rows.stream().mapToDouble(row -> byVertices ? row.vertices : row.edges).max().orElseThrow();
        double minimumY = rows.stream().mapToDouble(row -> row.averageNs / 1000.0).min().orElseThrow();
        double maximumY = rows.stream().mapToDouble(row -> row.averageNs / 1000.0).max().orElseThrow();
        double lowY = Math.pow(10, Math.floor(Math.log10(minimumY)));
        double highY = Math.pow(10, Math.ceil(Math.log10(maximumY)));
        if (lowY == highY) {
            highY *= 10;
        }
        String axis = byVertices ? "Number of vertices (V)" : "Number of edges (E)";
        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1040\" height=\"610\" viewBox=\"0 0 1040 610\">\n");
        svg.append("<rect width=\"1040\" height=\"610\" fill=\"white\"/>\n");
        svg.append("<g font-family=\"Arial, sans-serif\" font-size=\"14\" fill=\"#1f2937\">\n");
        label(svg, 95, 32, "Execution time vs. " + (byVertices ? "vertices" : "edges"), "start", 23);
        label(svg, 95, 53, "Arithmetic mean of 5 trials; both axes use logarithmic scales", "start", 14);
        for (double tick = lowY; tick <= highY * 1.001; tick *= 10) {
            double y = TOP + HEIGHT - scale(tick, lowY, highY, HEIGHT);
            line(svg, LEFT, y, LEFT + WIDTH, y, "#e5e7eb", 1);
            label(svg, LEFT - 12, y + 5, String.format(Locale.US, "%.0f", tick), "end", 13);
        }
        Set<Integer> xTicks = new LinkedHashSet<>();
        for (Experiment.Summary row : rows) {
            xTicks.add(byVertices ? row.vertices : row.edges);
        }
        for (int tick : xTicks) {
            double x = LEFT + scale(tick, minimumX, maximumX, WIDTH);
            line(svg, x, TOP, x, TOP + HEIGHT, "#e5e7eb", 1);
            label(svg, x, TOP + HEIGHT + 25, Integer.toString(tick), "middle", 13);
        }
        line(svg, LEFT, TOP, LEFT, TOP + HEIGHT, "#374151", 1.5);
        line(svg, LEFT, TOP + HEIGHT, LEFT + WIDTH, TOP + HEIGHT, "#374151", 1.5);
        label(svg, LEFT + WIDTH / 2.0, 565, axis, "middle", 16);
        svg.append("<text transform=\"translate(25 290) rotate(-90)\" text-anchor=\"middle\" font-size=\"16\">Average execution time (microseconds)</text>\n");
        Set<String> algorithms = new LinkedHashSet<>();
        for (Experiment.Summary row : rows) {
            algorithms.add(row.algorithm);
        }
        int index = 0;
        for (String algorithm : algorithms) {
            String color = COLORS[index];
            StringBuilder points = new StringBuilder();
            for (Experiment.Summary row : rows) {
                if (row.algorithm.equals(algorithm)) {
                    double x = LEFT + scale(byVertices ? row.vertices : row.edges, minimumX, maximumX, WIDTH);
                    double y = TOP + HEIGHT - scale(row.averageNs / 1000.0, lowY, highY, HEIGHT);
                    points.append(String.format(Locale.US, "%.2f,%.2f ", x, y));
                    svg.append(String.format(Locale.US,
                            "<circle cx=\"%.2f\" cy=\"%.2f\" r=\"4\" fill=\"%s\"><title>%s: V=%d, E=%d, %.3f microseconds</title></circle>\n",
                            x, y, color, algorithm, row.vertices, row.edges, row.averageNs / 1000.0));
                }
            }
            svg.append("<polyline points=\"").append(points).append("\" fill=\"none\" stroke=\"")
                    .append(color).append("\" stroke-width=\"2\"/>\n");
            double legendY = 100 + index * 35;
            line(svg, 760, legendY, 790, legendY, color, 3);
            label(svg, 800, legendY + 5, algorithm, "start", 14);
            index++;
        }
        label(svg, 95, 595, "V and E grow together in these datasets; the plots do not isolate their separate effects.", "start", 13);
        svg.append("</g></svg>\n");
        Files.writeString(path, svg, StandardCharsets.UTF_8);
    }

    private static double scale(double value, double minimum, double maximum, double length) {
        return (Math.log10(value) - Math.log10(minimum)) / (Math.log10(maximum) - Math.log10(minimum)) * length;
    }

    private static void label(StringBuilder svg, double x, double y, String text, String anchor, int fontSize) {
        svg.append(String.format(Locale.US,
                "<text x=\"%.2f\" y=\"%.2f\" text-anchor=\"%s\" font-size=\"%d\">%s</text>\n",
                x, y, anchor, fontSize, text));
    }

    private static void line(StringBuilder svg, double x1, double y1, double x2, double y2, String color, double width) {
        svg.append(String.format(Locale.US,
                "<line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"%.1f\"/>\n",
                x1, y1, x2, y2, color, width));
    }
}
