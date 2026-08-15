package org.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

/**
 * Automates the benchmarking process for TSP solvers on a predefined set of problem files.
 * This class has been refactored to use a two-layered multithreading approach
 * to run benchmarks for multiple files in parallel, and to parallelize the
 * empirical GA tests within each benchmark. This significantly improves performance
 * and follows modern concurrent design patterns.
 * <p>
 * It intelligently handles different problem sizes, running the computationally
 * expensive DP algorithm only on smaller instances where it is feasible.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 25 September 2025
 */
public class BenchmarkRunner {
    /**
     * A public static inner class to hold the benchmark results for a single TSP file.
     * It is public so that external utility classes, like the chart generator, can
     * access the result data for visualization. This class acts as a Data Transfer Object (DTO).
     */
    public static class BenchmarkResult {
        public String problemName;
        public String dpCost = "N/A";
        public String dpNFC = "N/A";
        public double gaBestCost;
        public double gaMeanCost;
        public String gaSuccessRate = "N/A";
        public int gaAvgNFC;
    }

    /**
     * A self-contained, callable task that performs a full benchmark on a single file.
     * This class encapsulates the entire logic for benchmarking one problem instance,
     * following the Command design pattern. This makes the main runner class cleaner
     * and simplifies parallel execution.
     */
    private static class BenchmarkTask implements Callable<BenchmarkResult> {
        private final String filename;
        private static final int GA_RUNS = 30;
        /** A practical limit for DP feasibility; problems larger than this will be skipped. */
        private static final int DP_FEASIBILITY_LIMIT = 22;

        public BenchmarkTask(String filename) {
            this.filename = filename;
        }

        /**
         * The main execution method for the task, called by the ExecutorService.
         * @return A {@link BenchmarkResult} object containing the performance data.
         * @throws IOException if the TSP file cannot be read.
         */
        @Override
        public BenchmarkResult call() throws IOException {
            System.out.println("  -> Starting benchmark for: " + filename);
            TSPParser.TSPInstance instance = TSPParser.readFromResource(filename);
            BenchmarkResult result = new BenchmarkResult();
            result.problemName = filename;

            // Intelligently decide whether to run the DP solver based on problem size.
            if (instance.size() <= DP_FEASIBILITY_LIMIT) {
                runDpBenchmark(instance, result);
                runGaBenchmark(instance, result, true); // true = can calculate success rate
            } else {
                System.out.println("     - Skipping DP for " + filename + " (size " + instance.size() + " > " + DP_FEASIBILITY_LIMIT + ")");
                runGaBenchmark(instance, result, false); // false = cannot calculate SR
            }

            System.out.println("  <- Finished benchmark for: " + filename);
            return result;
        }

        /**
         * Runs the DP solver and populates the result object.
         */
        private void runDpBenchmark(TSPParser.TSPInstance instance, BenchmarkResult result) {
            TSPDP dpSolver = new TSPDP(instance);
            dpSolver.solve();
            result.dpCost = String.format(Locale.US, "%.2f", dpSolver.getTourLength());
            result.dpNFC = String.valueOf(dpSolver.getNFC());
        }

        /**
         * Runs the GA solver {@value #GA_RUNS} times in parallel to gather statistics.
         */
        private void runGaBenchmark(TSPParser.TSPInstance instance, BenchmarkResult result, boolean canCalcSR) {
            double bestCost = Double.MAX_VALUE;
            double totalCost = 0;
            long totalNFC = 0;
            int successCount = 0;

            // A temporary, local thread pool for the GA runs of this specific benchmark task.
            ExecutorService gaExecutor = Executors.newWorkStealingPool();
            try {
                List<Future<GA_Result>> futureGAResults = new ArrayList<>();
                for (int i = 0; i < GA_RUNS; i++) {
                    Callable<GA_Result> gaTask = () -> {
                        TSPGA.SelectionStrategy s = new TSPGA.TournamentSelection(5);
                        TSPGA.CrossoverStrategy c = new TSPGA.OrderedCrossover();
                        TSPGA.MutationStrategy m = new TSPGA.SwapMutation();
                        TSPGA ga = new TSPGA(instance, 50, 200, s, c, m);
                        ga.solve();
                        return new GA_Result(ga.getBestDistance(), ga.getNFC());
                    };
                    futureGAResults.add(gaExecutor.submit(gaTask));
                }

                // Collect results from all parallel GA runs.
                for (Future<GA_Result> future : futureGAResults) {
                    GA_Result res = future.get();
                    totalCost += res.cost;
                    totalNFC += res.nfc;
                    if (res.cost < bestCost) bestCost = res.cost;
                    if (canCalcSR) {
                        double optimalCost = Double.parseDouble(result.dpCost);
                        if (Math.abs(res.cost - optimalCost) < 1e-6) successCount++;
                    }
                }
            } catch (InterruptedException | ExecutionException | NumberFormatException e) {
                e.printStackTrace();
            } finally {
                // Ensure the local thread pool is always shut down.
                gaExecutor.shutdown();
            }

            // Aggregate and store the final GA statistics.
            result.gaBestCost = bestCost;
            result.gaMeanCost = totalCost / GA_RUNS;
            if (canCalcSR) {
                result.gaSuccessRate = String.format(Locale.US, "%.1f", (100.0 * successCount) / GA_RUNS);
            }
            result.gaAvgNFC = (int) (totalNFC / GA_RUNS);
        }

        /** A private record-like class to hold the result of a single GA run. */
        private static class GA_Result {
            final double cost;
            final int nfc;
            public GA_Result(double cost, int nfc) {
                this.cost = cost;
                this.nfc = nfc;
            }
        }
    }

    // --- Main BenchmarkRunner Class ---

    private final List<String> benchmarkFiles = List.of(
            "testFile.atsp", "br17.atsp", "ftv33.atsp", "ft53.atsp", "ft70.atsp"
    );
    private final List<BenchmarkResult> results;

    public BenchmarkRunner() {
        this.results = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Executes the entire benchmark suite by submitting a {@link BenchmarkTask} for
     * each file to a main thread pool. This parallelizes the entire process.
     */
    public void run() {
        System.out.println("\n===== Starting Full Benchmark Suite on " + benchmarkFiles.size() + " files =====");
        ExecutorService mainExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            List<Future<BenchmarkResult>> futureResults = new ArrayList<>();
            for (String filename : benchmarkFiles) {
                futureResults.add(mainExecutor.submit(new BenchmarkTask(filename)));
            }
            // Block and wait for all benchmark tasks to complete.
            for (Future<BenchmarkResult> future : futureResults) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            // Ensure the main thread pool is always shut down, even if errors occur.
            mainExecutor.shutdown();
        }
        System.out.println("\n===== Benchmark Suite Complete =====");
    }

    /**
     * Prints a formatted summary table of all benchmark results to the console.
     */
    public void printSummary() {
        results.sort((a, b) -> a.problemName.compareTo(b.problemName));
        System.out.println("\n------------------------------------------- BENCHMARK SUMMARY -------------------------------------------");
        System.out.printf("%-15s | %10s | %10s | %12s | %12s | %10s | %12s%n",
                "Problem", "DP Cost", "DP NFC", "GA Best Cost", "GA Mean Cost", "GA SR(%)", "GA Avg NFC");
        System.out.println("----------------|------------|------------|--------------|--------------|------------|----------------");
        for (BenchmarkResult res : results) {
            System.out.printf(Locale.US, "%-15s | %10s | %10s | %12.2f | %12.2f | %10s | %12d%n",
                    res.problemName, res.dpCost, res.dpNFC, res.gaBestCost, res.gaMeanCost, res.gaSuccessRate, res.gaAvgNFC);
        }
        System.out.println("---------------------------------------------------------------------------------------------------------");
    }

    /**
     * Triggers the creation and display of the final summary bar chart.
     * This method safely invokes the chart creation on the Swing Event Dispatch Thread.
     */
    public void displayChart() {
        if (results.isEmpty()) {
            System.out.println("No results to display in chart.");
            return;
        }
        SwingUtilities.invokeLater(() -> RealTimeChart.showBenchmarkSummaryChart(results));
    }

    /**
     * Provides external access to the benchmark results.
     * @return A thread-safe list of the benchmark results.
     */
    public List<BenchmarkResult> getResults() {
        return results;
    }
}

