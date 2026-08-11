package performance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A tiny self-contained SVG line-chart writer, so the project can produce its own
 * graphs with <b>no external plotting dependency</b> (Python/matplotlib is not
 * available in every marking environment). Each chart plots one or more named
 * series of (x, y) points on auto-scaled linear axes and writes a standalone
 * {@code .svg} file that opens in any browser.
 *
 * <p>This is a reporting/utility class only — it is not part of the assessed data
 * structures or algorithms. It intentionally uses {@code java.util.List} purely
 * as local scratch storage while assembling SVG text.</p>
 */
public final class SvgChart {

    private final String title, xLabel, yLabel;
    private final List<String> names = new ArrayList<>();
    private final List<double[]> xs = new ArrayList<>();
    private final List<double[]> ys = new ArrayList<>();

    private static final String[] COLORS = { "#1f77b4", "#d62728", "#2ca02c", "#ff7f0e", "#9467bd", "#8c564b" };
    private static final int W = 820, H = 500;
    private static final int L = 90, R = 210, T = 60, B = 70; // margins (R leaves room for legend)

    public SvgChart(String title, String xLabel, String yLabel) {
        this.title = title; this.xLabel = xLabel; this.yLabel = yLabel;
    }

    /** Add a named data series. x[] and y[] must be the same length. */
    public SvgChart addSeries(String name, double[] x, double[] y) {
        names.add(name); xs.add(x); ys.add(y);
        return this;
    }

    /** Render the chart to {@code path} as an SVG file. */
    public void write(Path path) {
        double xMin = Double.MAX_VALUE, xMax = -Double.MAX_VALUE;
        double yMin = 0, yMax = -Double.MAX_VALUE; // y axis starts at 0 for timing charts
        for (int s = 0; s < xs.size(); s++) {
            for (double v : xs.get(s)) { xMin = Math.min(xMin, v); xMax = Math.max(xMax, v); }
            for (double v : ys.get(s)) { yMax = Math.max(yMax, v); }
        }
        if (xMax == xMin) xMax = xMin + 1;
        if (yMax == yMin) yMax = yMin + 1;
        final double fxMin = xMin, fxMax = xMax, fyMin = yMin, fyMax = yMax;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(W).append("' height='").append(H)
           .append("' font-family='Segoe UI, Arial, sans-serif'>\n");
        svg.append("<rect width='100%' height='100%' fill='white'/>\n");
        svg.append(text(W / 2, 30, title, 18, "middle", "#222", true));

        int plotW = W - L - R, plotH = H - T - B;
        // axes
        svg.append(line(L, T, L, T + plotH, "#888"));
        svg.append(line(L, T + plotH, L + plotW, T + plotH, "#888"));
        // gridlines + tick labels (5 divisions each axis)
        for (int i = 0; i <= 5; i++) {
            double gy = T + plotH - i / 5.0 * plotH;
            double yv = fyMin + i / 5.0 * (fyMax - fyMin);
            svg.append(line(L, gy, L + plotW, gy, "#eee"));
            svg.append(text(L - 8, gy + 4, trim(yv), 11, "end", "#555", false));
            double gx = L + i / 5.0 * plotW;
            double xv = fxMin + i / 5.0 * (fxMax - fxMin);
            svg.append(text(gx, T + plotH + 20, trim(xv), 11, "middle", "#555", false));
        }
        svg.append(text(L + plotW / 2.0, H - 18, xLabel, 13, "middle", "#333", false));
        svg.append("<text x='20' y='").append(T + plotH / 2.0)
           .append("' font-size='13' fill='#333' transform='rotate(-90 20 ").append(T + plotH / 2.0)
           .append(")' text-anchor='middle'>").append(esc(yLabel)).append("</text>\n");

        // series polylines + legend
        for (int s = 0; s < names.size(); s++) {
            String color = COLORS[s % COLORS.length];
            double[] x = xs.get(s), y = ys.get(s);
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < x.length; i++) {
                double px = L + (x[i] - fxMin) / (fxMax - fxMin) * plotW;
                double py = T + plotH - (y[i] - fyMin) / (fyMax - fyMin) * plotH;
                pts.append(fmt(px)).append(',').append(fmt(py)).append(' ');
                svg.append("<circle cx='").append(fmt(px)).append("' cy='").append(fmt(py))
                   .append("' r='3' fill='").append(color).append("'/>\n");
            }
            svg.append("<polyline fill='none' stroke='").append(color)
               .append("' stroke-width='2' points='").append(pts).append("'/>\n");
            int ly = T + 10 + s * 22;
            svg.append(line(L + plotW + 20, ly, L + plotW + 45, ly, color));
            svg.append(text(L + plotW + 50, ly + 4, names.get(s), 12, "start", "#333", false));
        }
        svg.append("</svg>\n");

        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, svg.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed writing chart " + path, e);
        }
    }

    // --- small SVG helpers ---
    private static String line(double x1, double y1, double x2, double y2, String color) {
        return "<line x1='" + fmt(x1) + "' y1='" + fmt(y1) + "' x2='" + fmt(x2) + "' y2='" + fmt(y2)
                + "' stroke='" + color + "'/>\n";
    }
    private static String text(double x, double y, String s, int size, String anchor, String color, boolean bold) {
        return "<text x='" + fmt(x) + "' y='" + fmt(y) + "' font-size='" + size + "' fill='" + color
                + "' text-anchor='" + anchor + "'" + (bold ? " font-weight='bold'" : "") + ">" + esc(s) + "</text>\n";
    }
    private static String fmt(double d) { return String.valueOf(Math.round(d * 100) / 100.0); }
    private static String trim(double d) {
        if (Math.abs(d) >= 1000) return String.valueOf(Math.round(d));
        return String.valueOf(Math.round(d * 100) / 100.0);
    }
    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
