package org.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * A utility class for creating and displaying charts related to the TSP solvers.
 * This class can generate two types of charts:
 * 1. A real-time line chart showing the progress of a single solver instance.
 * 2. A final summary bar chart comparing the results of a full benchmark suite.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 25 September 2025
 */
public class RealTimeChart {

    // (The DataPoint inner class and all existing code for the real-time chart remain the same)
    public static final class DataPoint {
        private final long nfc;
        private final double fitness;
        public DataPoint(long nfc, double fitness) { this.nfc = nfc; this.fitness = fitness; }
        public long nfc() { return nfc; }
        public double fitness() { return fitness; }
    }

    private final JFrame frame;
    private final TSPSolver solver;
    private final XYSeries series;

    public RealTimeChart(String algorithmName, TSPSolver solver) {
        this.solver = solver;
        series = new XYSeries(algorithmName);
        series.add(0, 0);
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                algorithmName + " Progress", "Number of Function Calls (NFC)", "Tour Distance (Fitness)",
                dataset, PlotOrientation.VERTICAL, true, true, false);
        frame = new JFrame("TSP Solver - Real-Time Performance");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new ChartPanel(chart), BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    public void showAndRun() {
        frame.setVisible(true);
        SwingWorker<Void, DataPoint> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                Consumer<DataPoint> progressConsumer = this::publish;
                solver.setProgressConsumer(progressConsumer);
                solver.solve();
                return null;
            }
            @Override
            protected void process(List<DataPoint> chunks) {
                for (DataPoint point : chunks) {
                    series.add(point.nfc(), point.fitness(), false);
                }
                series.fireSeriesChanged();
            }
            @Override
            protected void done() {
                try {
                    get();
                    if (solver instanceof TSPDP) {
                        frame.setTitle("Finished DP - Final Length: " + String.format("%.2f", ((TSPDP) solver).getTourLength()));
                    } else if (solver instanceof TSPGA) {
                        frame.setTitle("Finished GA - Final Length: " + String.format("%.2f", ((TSPGA) solver).getBestDistance()));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    /**
     * Creates and displays a summary bar chart from a list of benchmark results.
     * This is a static factory method that acts as a second entry point for this utility class.
     *
     * @param results The list of completed benchmark results to visualize.
     */
    public static void showBenchmarkSummaryChart(List<BenchmarkRunner.BenchmarkResult> results) {
        // Create a dataset for the bar chart
        DefaultCategoryDataset dataset = createBenchmarkDataset(results);

        // Create the chart object
        JFreeChart barChart = ChartFactory.createBarChart(
                "Benchmark Performance Summary", "TSP Problem Instance", "Tour Cost",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        // Create and set up the window (JFrame)
        JFrame frame = new JFrame("Benchmark Summary Chart");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(1024, 600));
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * A private helper method to populate the benchmark chart's dataset.
     * @return A DefaultCategoryDataset containing the performance data.
     */
    private static DefaultCategoryDataset createBenchmarkDataset(List<BenchmarkRunner.BenchmarkResult> results) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        final String dpSeries = "DP Optimal Cost";
        final String gaBestSeries = "GA Best Cost";
        final String gaMeanSeries = "GA Mean Cost";

        for (BenchmarkRunner.BenchmarkResult res : results) {
            if (!"N/A".equals(res.dpCost)) {
                try {
                    dataset.addValue(Double.parseDouble(res.dpCost), dpSeries, res.problemName);
                } catch (NumberFormatException ignored) {}
            }
            dataset.addValue(res.gaBestCost, gaBestSeries, res.problemName);
            dataset.addValue(res.gaMeanCost, gaMeanSeries, res.problemName);
        }
        return dataset;
    }
}