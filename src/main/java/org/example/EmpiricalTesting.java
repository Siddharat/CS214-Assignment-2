package org.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Handles the empirical testing and statistical comparison of different TSP solvers.
 * This class runs each algorithm multiple times to gather performance data
 * and then prints a summary of the results, including metrics like success rate,
 * best, mean, and worst performance, and the average number of function calls.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 25 September 2025
 */
public class EmpiricalTesting {
    private final TSPParser.TSPInstance instance;
    // A map to store statistical results for each algorithm, preserving insertion order.
    private final Map<String, Stats> results = new LinkedHashMap<>();

    /**
     * A simple data class to hold the results from a single GA run,
     * necessary for returning multiple values from a Callable.
     */
    private static class GAResult {
        final double cost;
        final int nfc; // Number of Function Calls

        public GAResult(double cost, int nfc) {
            this.cost = cost;
            this.nfc = nfc;
        }
    }

    public EmpiricalTesting(TSPParser.TSPInstance instance) {
        this.instance = instance;
    }

    /**
     * Executes a series of runs for different TSP algorithms to compare them.
     * @param runs The number of times to run each stochastic algorithm.
     */
    public void runComparison(int runs) {
        results.clear();
        double optimum; // To be determined by the exact solver (DP).

        // --- 1. Dynamic Programming (Exact Solver) ---
        System.out.println("🚀 Running Dynamic Programming " + runs + " times...");
        Stats dpStats = new Stats();
        // First, run DP once to find the true optimal solution for later comparison.
        TSPDP initialDpRun = new TSPDP(instance);
        initialDpRun.solve();
        optimum = initialDpRun.getTourLength();
        System.out.printf("✅ Optimal solution found by DP: %.2f%n", optimum);

        // Run DP multiple times to get a baseline for execution time/NFC.
        for (int i = 0; i < runs; i++) {
            TSPDP dpRun = new TSPDP(instance);
            dpRun.solve();
            dpStats.add(dpRun.getTourLength(), dpRun.getNFC());
            dpStats.successCount++; // DP is deterministic, so it always "succeeds".
            System.out.printf("  -> Finished DP Run %d/%d: cost=%.2f%n", i + 1, runs, dpRun.getTourLength());
        }
        results.put("Dynamic Programming", dpStats);
        System.out.println("✅ DP runs complete.");

        // --- 2. Genetic Algorithm (Heuristic Solver) ---
        Stats gaStats = new Stats();
        int coreCount = Runtime.getRuntime().availableProcessors();
        // Use a fixed-size thread pool to run GA instances concurrently.
        ExecutorService executor = Executors.newFixedThreadPool(coreCount);
        System.out.println("\n🚀 Running " + runs + " GA tasks in parallel on " + coreCount + " CPU threads...");

        List<Future<GAResult>> futureResults = new ArrayList<>();

        // Create and submit a task to the executor for each GA run.
        for (int i = 0; i < runs; i++) {
            Callable<GAResult> task = () -> {
                // Instantiate GA with concrete strategy objects for each run.
                TSPGA.SelectionStrategy selection = new TSPGA.TournamentSelection(5);
                TSPGA.CrossoverStrategy crossover = new TSPGA.OrderedCrossover();
                TSPGA.MutationStrategy mutation = new TSPGA.SwapMutation();
                TSPGA ga = new TSPGA(instance, 50, 200, selection, crossover, mutation);

                ga.solve();
                return new GAResult(ga.getBestDistance(), ga.getNFC());
            };
            futureResults.add(executor.submit(task));
        }

        try {
            // Retrieve the results as they become available.
            for (int i = 0; i < runs; i++) {
                // .get() blocks until the future is complete.
                GAResult result = futureResults.get(i).get();
                gaStats.add(result.cost, result.nfc);
                // A run is a "success" if its result is very close to the known optimum.
                if (Math.abs(result.cost - optimum) < 1e-6) {
                    gaStats.successCount++;
                }
                System.out.printf("  -> Finished GA Run %d/%d: cost=%.2f%n", i + 1, runs, result.cost);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ An error occurred during parallel execution of GA runs.");
            e.printStackTrace();
            executor.shutdownNow(); // Attempt to stop all actively executing tasks.
            return;
        }

        executor.shutdown(); // Gracefully shut down the executor service.
        results.put("Genetic Algorithm", gaStats);
        System.out.println("✅ All GA runs complete.");
    }

    /**
     * Formats and prints a summary table of the performance of all tested algorithms.
     */
    public void printResults() {
        System.out.println("\n================ Empirical Testing Results ================");
        for (Map.Entry<String, Stats> entry : results.entrySet()) {
            Stats s = entry.getValue();
            // Calculate success rate (SR) as a percentage.
            double sr = (s.count == 0) ? 0.0 : (100.0 * s.successCount / s.count);
            System.out.printf("%-22s -> Best: %.2f, Mean: %.2f, Worst: %.2f, SR: %.2f%%, Avg NFC: %d (runs: %d)%n",
                    entry.getKey(), s.best, s.mean(), s.worst, sr, s.avgNFC(), s.count);
        }
        System.out.println("==========================================================");
    }

    /**
     * A helper class to aggregate statistics from multiple runs of an algorithm.
     */
    private static class Stats {
        double best = Double.MAX_VALUE;
        double worst = Double.MIN_VALUE;
        double total = 0;
        int count = 0;
        long totalNFC = 0;
        int successCount = 0;

        /** Adds a new result to the statistical summary. */
        void add(double val, int nfc) {
            if (val < best) best = val;
            if (val > worst) worst = val;
            total += val;
            totalNFC += nfc;
            count++;
        }

        double mean() { return (count == 0) ? 0 : total / count; }
        int avgNFC() { return (count == 0) ? 0 : (int) (totalNFC / count); }
    }
}