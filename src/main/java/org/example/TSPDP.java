package org.example;
/*
*Tran, H.L. and Duong, M.P., 2024. Approach to Travelling Salesman Problem using Dynamic Programming and Branch-and-Bound technique. Research proposal. University of Technology Sydney. Available at: https://www.researchgate.net/publication/389855682
*[Accessed 21 September 2025].
*/
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of the Traveling Salesman Problem (TSP) solver using a
 * deterministic Dynamic Programming approach (Held-Karp).
 *
 * extends AbstractTSPSolver to reuse common functionality (NFC counting,
 * distance wrapper, progress consumer handling).
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 21 September 2025
 */
public class TSPDP extends AbstractTSPSolver {
    /** Stores the final, optimal tour length found by the solver. */
    private double bestTourLength = Double.POSITIVE_INFINITY;
    /** A list to store the sequence of cities in the optimal tour. */
    private final List<Integer> tour = new ArrayList<>();

    public TSPDP(TSPParser.TSPInstance instance) {
        super(instance);
    }

    @Override
    public void solve() {
        nfc = 0;
        tour.clear();
        int n = instance.size();
        if (n <= 1) { return; }

        int N = 1 << n; // 2^n subsets
        double[][] dp = new double[N][n];
        int[][] parent = new int[N][n];
        for (double[] row : dp) Arrays.fill(row, Double.POSITIVE_INFINITY);
        dp[1][0] = 0.0;

        // Main DP calculation loop.
        for (int mask = 1; mask < N; mask += 2) {
            for (int u = 0; u < n; u++) {
                if ((mask & (1 << u)) == 0) continue;
                for (int v = 0; v < n; v++) {
                    if ((mask & (1 << v)) != 0 || u == v) continue;
                    int nextMask = mask | (1 << v);
                    double newDist = dp[mask][u] + d(u, v); // uses inherited d()
                    if (newDist < dp[nextMask][v]) {
                        dp[nextMask][v] = newDist;
                        parent[nextMask][v] = u;
                    }
                }
            }

            // Graphing logic: after each mask size is processed estimate best possible full tour.
            if (progressConsumer != null && Integer.bitCount(mask) > 1) {
                double currentBestEstimate = Double.POSITIVE_INFINITY;
                for (int u = 1; u < n; u++) {
                    if ((mask & (1 << u)) != 0) {
                        double estimatedTour = dp[mask][u] + d(u, 0);
                        if (estimatedTour < currentBestEstimate) {
                            currentBestEstimate = estimatedTour;
                        }
                    }
                }
                if (currentBestEstimate != Double.POSITIVE_INFINITY) {
                    progressConsumer.accept(new RealTimeChart.DataPoint(nfc, currentBestEstimate));
                }
            }
        }

        // Finalize: find true best tour
        int finalMask = N - 1;
        int lastCity = -1;
        bestTourLength = Double.POSITIVE_INFINITY;
        for (int u = 1; u < n; u++) {
            double finalDist = dp[finalMask][u] + d(u, 0);
            if (finalDist < bestTourLength) {
                bestTourLength = finalDist;
                lastCity = u;
            }
        }

        // Ensure final value is reported to chart
        if (progressConsumer != null) {
            progressConsumer.accept(new RealTimeChart.DataPoint(nfc, bestTourLength));
        }

        // Reconstruct path
        if (lastCity != -1) {
            int currentMask = finalMask;
            int currentCity = lastCity;
            while (currentCity != 0) {
                tour.add(currentCity);
                int prevCity = parent[currentMask][currentCity];
                currentMask ^= (1 << currentCity);
                currentCity = prevCity;
            }
            tour.add(0);
            Collections.reverse(tour);
        }
    }

    @Override
    public void printSolution() {
        System.out.println("DP best tour length: " + bestTourLength);
        if (!tour.isEmpty()) {
            System.out.print("Path: ");
            for (int i = 0; i < tour.size(); i++) {
                System.out.print((tour.get(i) + 1) + (i == tour.size() - 1 ? "" : " -> "));
            }
            System.out.println(" -> " + (tour.get(0) + 1));
        } else {
            System.out.println("No path found.");
        }
    }

    /**
     * This packages the results into a PrintResults object for other classes to use.
     */
    @Override
    public PrintResults getSolution() {
        return new PrintResults("DP", bestTourLength, tour, instance);
    }

    public double getTourLength() { return bestTourLength; }
}
