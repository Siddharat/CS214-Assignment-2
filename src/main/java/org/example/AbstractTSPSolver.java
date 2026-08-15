package org.example;

import java.util.function.Consumer;

/**
 * An abstract base class for TSP solvers, designed to enforce a common structure
 * and share boilerplate code among different solver implementations. This class
 * is a core component of the project's Object-Oriented design, utilizing
 * inheritance to promote code reuse and a consistent interface.
 * <p>
 * It implements the {@link TSPSolver} interface and provides default handling for
 * progress reporting and a centralized, encapsulated method for counting
 * function calls (NFC). Concrete solver classes like {@link TSPDP} and
 * {@link TSPGA} should extend this class.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 21 September 2025
 */
public abstract class AbstractTSPSolver implements TSPSolver {

    /** The TSP problem instance, containing the distance matrix. Marked as protected
     * so it is accessible to subclasses. */
    protected final TSPParser.TSPInstance instance;

    /** An encapsulated counter for the number of function calls (NFC) made during
     * the solving process. */
    protected int nfc = 0;

    /** A functional interface consumer for reporting real-time progress to a chart.
     * Initialized to a no-op lambda to prevent NullPointerExceptions. */
    protected Consumer<RealTimeChart.DataPoint> progressConsumer = (p) -> {};

    /**
     * Constructs the abstract solver. This constructor must be called by all
     * subclasses using {@code super(instance)}.
     *
     * @param instance The TSP problem data to be solved.
     */
    public AbstractTSPSolver(TSPParser.TSPInstance instance) {
        this.instance = instance;
    }

    /**
     * Sets the consumer that will receive real-time progress updates.
     * This is part of the Observer pattern, allowing the chart to "listen" to the solver.
     *
     * @param consumer The consumer to which progress data points will be sent.
     */
    @Override
    public void setProgressConsumer(Consumer<RealTimeChart.DataPoint> consumer) {
        this.progressConsumer = consumer != null ? consumer : (p) -> {};
    }

    /**
     * Accessor for the total number of function calls made by the solver instance.
     * This method correctly implements the contract from the TSPSolver interface.
     *
     * @return The total NFC count.
     */
    @Override
    public int getNFC() {
        return nfc;
    }

    /**
     * A protected helper method that wraps the distance matrix lookup.
     * This is the single, centralized point where the NFC is incremented, ensuring
     * strong encapsulation of the counting logic.
     *
     * @param i The index of the starting city.
     * @param j The index of the destination city.
     * @return The distance between city i and city j.
     */
    protected double d(int i, int j) {
        nfc++;
        return instance.getDistance(i, j);
    }
}

