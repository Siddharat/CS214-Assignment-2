package org.example;

/*
 *Li, H., 2025. Solving the TSP Problem Based on Improved Genetic Algorithm. Proceedings of the 5th International Conference on Signal Processing and Machine Learning. Harbin Engineering University. DOI: 10.54254/2755-2721/133/2025.20603.
 */
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A Genetic Algorithm (GA) implementation for solving the Traveling Salesman Problem (TSP).
 * This class has been refactored to use the Strategy Pattern, allowing its core
 * components (selection, crossover, mutation) to be injected as interchangeable
 * strategy objects. This enhances flexibility and adheres to advanced OOP principles.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 21 September 2025
 */
public class TSPGA extends AbstractTSPSolver {
    // The number of candidate tours (individuals) in the population.
    private final int populationSize;
    // The total number of iterations the algorithm will run.
    private final int generations;
    // A random number generator for various stochastic processes in the GA.
    private final Random rand = new Random();

    // The current population of tours.
    private List<int[]> population;
    // The best tour found so far across all generations.
    private int[] bestTour;
    // The distance of the best tour found so far.
    private double bestDistance;

    // The current probability of a mutation occurring in a new child tour.
    private double mutationProb = 0.05;
    // The initial or base mutation probability, used for resetting.
    private final double initialMutationProb = 0.05;

    // --- STRATEGY PATTERN: Fields to hold the injected strategies ---
    // The injected strategy for selecting parent tours for reproduction.
    private final SelectionStrategy selectionStrategy;
    // The injected strategy for combining two parent tours to create a child tour.
    private final CrossoverStrategy crossoverStrategy;
    // The injected strategy for randomly altering a child tour.
    private final MutationStrategy mutationStrategy;
    // Executor service to manage a pool of threads for parallel fitness calculation.
    private final ExecutorService fitnessExecutor;

    /**
     * Constructor that injects the GA's core algorithmic strategies.
     *
     * @param instance         The TSP problem data.
     * @param populationSize   The number of individuals in each generation.
     * @param generations      The number of generations to run.
     * @param selectionStrategy The algorithm for selecting parents.
     * @param crossoverStrategy The algorithm for creating children from parents.
     * @param mutationStrategy  The algorithm for mutating individuals.
     */
    public TSPGA(TSPParser.TSPInstance instance, int populationSize, int generations,
                 SelectionStrategy selectionStrategy, CrossoverStrategy crossoverStrategy, MutationStrategy mutationStrategy) {
        super(instance);
        this.populationSize = Math.max(2, populationSize); // Ensures population is at least 2.
        this.generations = Math.max(1, generations); // Ensures at least 1 generation runs.
        this.selectionStrategy = selectionStrategy;
        this.crossoverStrategy = crossoverStrategy;
        this.mutationStrategy = mutationStrategy;
        // Creates a thread pool with a size equal to the number of available CPU cores.
        this.fitnessExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void solve() {
        try {
            nfc = 0; // Reset the number of function calls counter.
            initPopulation(); // Create the initial random population of tours.

            // Initialize the best tour as the first tour in the initial population.
            bestTour = population.get(0).clone();
            bestDistance = calcDistance(bestTour, false);

            int noImprovement = 0; // Counter to track generations without a new best tour.

            // Main GA loop: iterates for a fixed number of generations.
            for (int gen = 0; gen < generations; gen++) {
                List<int[]> newPop = new ArrayList<>(); // To hold the next generation of tours.

                // Create a new population of the same size as the old one.
                while (newPop.size() < populationSize) {
                    // --- STRATEGY PATTERN: Delegate to strategy objects ---
                    int[] parent1 = selectionStrategy.select(population, this); // Select the first parent.
                    int[] parent2 = selectionStrategy.select(population, this); // Select the second parent.
                    int[] child = crossoverStrategy.crossover(parent1, parent2, rand); // Create a child via crossover.
                    mutationStrategy.mutate(child, mutationProb, rand); // Apply mutation to the child.
                    newPop.add(child); // Add the new child to the next generation.
                }

                // Use the executor service to calculate fitness for the new population in parallel.
                List<Future<Double>> futureDistances = new ArrayList<>();
                for (int[] child : newPop) {
                    // Create a task for each child's distance calculation.
                    Callable<Double> task = () -> calcDistance(child, true);
                    futureDistances.add(fitnessExecutor.submit(task)); // Submit the task to the thread pool.
                }

                // Retrieve results and check for a new best tour.
                for (int i = 0; i < newPop.size(); i++) {
                    try {
                        double dist = futureDistances.get(i).get(); // Get the calculated distance from the Future object.
                        // If a better tour is found, update the best distance and best tour.
                        if (dist < bestDistance) {
                            bestDistance = dist;
                            bestTour = newPop.get(i).clone();
                            noImprovement = 0; // Reset the no-improvement counter.
                            mutationProb = initialMutationProb; // Reset mutation probability to its base value.
                            progressConsumer.accept(new RealTimeChart.DataPoint(nfc, bestDistance)); // Notify observers of progress.
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // Handle exceptions from the parallel execution.
                        e.printStackTrace();
                    }
                }

                // Adaptive mutation: increase mutation rate if the solution stagnates.
                noImprovement++;
                if (noImprovement == 5) mutationProb = initialMutationProb * 2; // Double mutation rate.
                if (noImprovement == 20) mutationProb = initialMutationProb * 4; // Quadruple mutation rate.

                population = newPop; // Replace the old population with the new one.
            }
        } finally {
            fitnessExecutor.shutdown(); // Always shut down the executor service to release resources.
        }
    }

    // --- The private methods tournamentSelect, crossover, and mutate have been removed ---
    // --- Their logic now resides in the concrete strategy classes below. ---
    /**
     * Strategy interface for parent selection algorithms.
     * Defines the contract for any selection method.
     */
    public interface SelectionStrategy {
        int[] select(List<int[]> population, TSPGA ga);
    }

    /**
     * Strategy interface for crossover algorithms.
     * Defines the contract for any crossover (recombination) method.
     */
    public interface CrossoverStrategy {
        int[] crossover(int[] parent1, int[] parent2, Random rand);
    }

    /**
     * Strategy interface for mutation algorithms.
     * Defines the contract for any mutation method.
     */
    public interface MutationStrategy {
        void mutate(int[] tour, double mutationProbability, Random rand);
    }

    /**
     * A concrete implementation of tournament selection.
     * Selects a few individuals at random and chooses the best one from that group.
     */
    public static class TournamentSelection implements SelectionStrategy {
        private final int tournamentSize; // The number of individuals to compete in each selection.

        public TournamentSelection(int tournamentSize) {
            this.tournamentSize = tournamentSize;
        }

        @Override
        public int[] select(List<int[]> population, TSPGA ga) {
            // Pick an initial individual as the current best.
            int bestIdx = ga.rand.nextInt(population.size());
            double bestFit = ga.fitness(population.get(bestIdx));

            // Run the tournament.
            for (int i = 1; i < tournamentSize; i++) {
                int idx = ga.rand.nextInt(population.size());
                double fit = ga.fitness(population.get(idx));
                // If the new contender is better, it becomes the current best.
                if (fit > bestFit) {
                    bestFit = fit;
                    bestIdx = idx;
                }
            }
            return population.get(bestIdx); // Return the winner of the tournament.
        }
    }

    /**
     * A concrete implementation of ordered crossover (OX1).
     * Preserves the relative order of cities from the parents.
     */
    public static class OrderedCrossover implements CrossoverStrategy {
        @Override
        public int[] crossover(int[] p1, int[] p2, Random rand) {
            int n = p1.length;
            // Select a random sub-sequence from the first parent.
            int start = rand.nextInt(n);
            int end = rand.nextInt(n - start) + start;

            int[] child = new int[n];
            Arrays.fill(child, -1); // Initialize child with a placeholder value.

            // Copy the sub-sequence from parent 1 to the child.
            for (int i = start; i <= end; i++) {
                child[i] = p1[i];
            }

            // Fill the remaining empty spots in the child with cities from parent 2.
            int current = 0; // Pointer for the current position in the child array.
            for (int i = 0; i < n; i++) {
                int city = p2[i];
                // If the city from parent 2 is not already in the child's sub-sequence...
                if (!contains(child, city)) {
                    // Find the next available slot in the child and place the city there.
                    while (child[current] != -1) current++;
                    child[current] = city;
                }
            }
            return child;
        }

        // Helper method to check if an array contains a specific value.
        private boolean contains(int[] arr, int val) {
            for (int x : arr) if (x == val) return true;
            return false;
        }
    }

    /**
     * A concrete implementation of swap mutation.
     * Randomly swaps two cities in a tour.
     */
    public static class SwapMutation implements MutationStrategy {
        @Override
        public void mutate(int[] tour, double mutationProbability, Random rand) {
            // Proceed with mutation only if a random roll is within the probability.
            if (rand.nextDouble() < mutationProbability) {
                // Select two random indices to swap.
                int i = rand.nextInt(tour.length);
                int j = rand.nextInt(tour.length);
                // Perform the swap.
                int temp = tour[i];
                tour[i] = tour[j];
                tour[j] = temp;
            }
        }
    }

    // SECTION: UTILITY AND HELPER METHODS

    @Override
    public void printSolution() {
        System.out.print("GA best tour: ");
        for (int i = 0; i < bestTour.length; i++) {
            // Print city numbers (1-based) and arrows.
            System.out.print((bestTour[i] + 1) + (i == bestTour.length - 1 ? "" : " -> "));
        }
        System.out.println(" -> " + (bestTour[0] + 1)); // Print return to the starting city.
        System.out.println("Tour length: " + bestDistance);
    }

    /**
     * This packages the results into a PrintResults object.
     */
    @Override
    public PrintResults getSolution() {
        // Convert the int[] tour array to a List<Integer> for the DTO
        List<Integer> tourPath = new ArrayList<>();
        if (bestTour != null) {
            for (int cityId : bestTour) {
                tourPath.add(cityId);
            }
        }
        return new PrintResults("GA", bestDistance, tourPath, instance);
    }

    /**
     * Returns the distance of the best tour found.
     * @return The best tour distance.
     */
    public double getBestDistance() {
        return bestDistance;
    }

    /**
     * Calculates the total distance of a given tour.
     * @param tour The tour (an array of city indices).
     * @param isParallel A flag indicating if this call is part of a parallel execution.
     * @return The total distance of the tour.
     */
    public double calcDistance(int[] tour, boolean isParallel) {
        double total = 0;
        int localNfc = 0; // Local counter for function calls within this method.
        // Sum distances between consecutive cities.
        for (int i = 0; i < tour.length - 1; i++) {
            total += instance.getDistance(tour[i], tour[i+1]);
            localNfc++;
        }
        // Add the distance from the last city back to the first.
        total += instance.getDistance(tour[tour.length - 1], tour[0]);
        localNfc++;

        // Update the global function call counter in a thread-safe manner if called in parallel.
        if (isParallel) {
            synchronized (this) {
                this.nfc += localNfc;
            }
        } else {
            this.nfc += localNfc;
        }
        return total;
    }

    /**
     * Calculates the fitness of a tour. Fitness is inversely proportional to distance.
     * @param tour The tour to evaluate.
     * @return The fitness value (higher is better).
     */
    private double fitness(int[] tour) {
        double dist = calcDistance(tour, false);
        // Avoid division by zero and handle it as infinitely fit (though unlikely).
        return dist <= 0 ? Double.POSITIVE_INFINITY : 1.0 / dist;
    }

    /**
     * Initializes the population with random tours.
     */
    private void initPopulation() {
        population = new ArrayList<>();
        int n = instance.size();
        // Create a base tour [0, 1, 2, ..., n-1].
        int[] baseTour = new int[n];
        for (int i = 0; i < n; i++) baseTour[i] = i;

        // Create populationSize individuals by shuffling the base tour.
        for (int i = 0; i < populationSize; i++) {
            int[] tour = baseTour.clone();
            shuffle(tour);
            population.add(tour);
        }
    }

    /**
     * Shuffles an array using the Fisher-Yates algorithm.
     * @param arr The array to be shuffled.
     */
    private void shuffle(int[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            // Swap elements at indices i and j.
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }
}