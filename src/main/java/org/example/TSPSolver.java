package org.example;

import java.util.function.Consumer;

/**
 * Defines the contract for any class that implements a solution to the
 * Traveling Salesman Problem (TSP).
 * This interface is a core component of the project's Object-Oriented design,
 * ensuring that different solving strategies (like Dynamic Programming or
 * Genetic Algorithms) can be used interchangeably by the main application.
 * It mandates the essential behaviors that every solver must provide.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 20 September 2025
 * @see TSPDP
 * @see TSPGA
 * @see AbstractTSPSolver
 */
public interface TSPSolver {

    /**
     * Executes the main logic of the solving algorithm.
     * Implementations of this method will contain the core computational work,
     * whether it's the deterministic process of DP or the evolutionary
     * process of a GA.
     */
    void solve();

    /**
     * Prints the final, best solution found by the solver to the standard output.
     * This should include, at a minimum, the total distance of the best tour and,
     * ideally, the sequence of cities in that tour.
     */
    void printSolution();

    /**
     * Returns the final solution as a PrintResults object.
     * @return A {@link PrintResults} object containing the results and print logic.
     */
    PrintResults getSolution();

    /**
     * Accessor for the total number of function calls (NFC) made by the solver.
     * The NFC is a key metric for measuring and comparing the computational
     * efficiency of different algorithms.
     *
     * @return The total number of distance lookups performed.
     */
    int getNFC();

    /**
     * Registers a callback function (a Consumer) to receive real-time progress
     * updates from the solver.
     * This is a default method, making it optional for implementing classes. It is
     * designed to decouple the solver's logic from the GUI, allowing components
     * like {@link RealTimeChart} to listen for data points as they are generated.
     *
     * @param consumer A {@link Consumer} that accepts a {@link RealTimeChart.DataPoint}
     * object, representing a snapshot of the solver's progress.
     */
    default void setProgressConsumer(Consumer<RealTimeChart.DataPoint> consumer) {
        // This default implementation does nothing, making it optional for solvers
        // that do not support real-time progress reporting.
    }
}