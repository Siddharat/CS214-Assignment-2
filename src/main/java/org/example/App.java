package org.example;

import java.io.IOException;
import java.util.Scanner;

import javax.swing.SwingUtilities;

import org.example.TSPParser.TSPInstance;

/**
 * The main entry point for the Traveling Salesman Problem Solver application.
 * This class provides a command-line interface (CLI) for users to interact with
 * different TSP solving algorithms and analysis tools.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 20 September 2025
 */
public class App {
    private static String selectedResource = null;

    /**
     * Main method that runs the top-level application menu.
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            showTopLevelMenu(scanner);
        }
        System.out.println("\nApplication terminated.");
    }

    /**
     * Displays the main, top-level menu of the application.
     */
    private static void showTopLevelMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n==================================");
            System.out.println("      TSP SOLVER - MAIN MENU      ");
            System.out.println("==================================");
            System.out.println("1. Select a File for Single Tests (DP, GA, Empirical Testing and Chart.)");
            System.out.println("2. Run Full Benchmark Suite");
            System.out.println("3. Exit Program");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> handleSolverSubMenu(scanner);
                case "2" -> runFullBenchmark();
                case "3" -> {
                    return; // Exit the top-level menu loop and end the program
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Handles the workflow for the single-file testing sub-menu.
     * It first prompts for a file choice, then shows the sub-menu.
     */
    private static void handleSolverSubMenu(Scanner scanner) {
        // Step 1: User selects a resource first.
        chooseResource(scanner);

        // Step 2: Show the sub-menu for actions on that resource.
        showSolverActionsMenu(scanner);
    }

    /**
     * Displays the sub-menu with actions for a single, selected TSP file.
     */
    private static void showSolverActionsMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n----------------------------------");
            System.out.println("Actions for: " + selectedResource);
            System.out.println("----------------------------------");
            System.out.println("1. Solve with Dynamic Programming");
            System.out.println("2. Solve with Genetic Algorithm");
            System.out.println("3. Run Empirical Testing (DP vs GA)");
            System.out.println("4. Solve with Real-Time Chart");
            System.out.println("5. Change TSP File");
            System.out.println("6. Return to Main Menu"); // This goes back, doesn't exit program
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine();
            boolean shouldReturn = false;

            switch (choice) {
                case "1" -> runSolver(scanner, "DP");
                case "2" -> runSolver(scanner, "GA");
                case "3" -> runEmpiricalTesting();
                case "4" -> runWithChart(scanner);
                case "5" -> chooseResource(scanner); // Re-select a file
                case "6" -> shouldReturn = true; // Set flag to exit this sub-menu
                default -> System.out.println("Invalid choice. Please try again.");
            }

            if (shouldReturn) {
                break; // Exit the sub-menu loop and return to the top-level menu
            }
        }
    }

    private static void chooseResource(Scanner scanner) {
        String r1 = "burma14.tsp";
        String r2 = "testFile.atsp";

        while (true) {
            System.out.println("\nChoose a small TSP file for this session:");
            System.out.println("1. " + r1);
            System.out.println("2. " + r2);
            System.out.print("Choice (1 or 2): ");
            String c = scanner.nextLine().trim();
            if ("1".equals(c)) {
                selectedResource = r1;
                break;
            }
            if ("2".equals(c)) {
                selectedResource = r2;
                break;
            }
            System.out.println("Invalid choice. Please enter 1 or 2.");
        }
        System.out.println("✅ Selected resource: " + selectedResource);
    }

    private static void runSolver(Scanner scanner, String type) {
        String path = selectedResource;
        try {
            TSPInstance instance = TSPParser.readFromResource(path);
            TSPSolver solver;

            if (type.equals("DP")) {
                solver = new TSPDP(instance);
            } else {
                System.out.print("Enter population size: ");
                int popSize = Integer.parseInt(scanner.nextLine());
                System.out.print("Enter number of generations: ");
                int gens = Integer.parseInt(scanner.nextLine());

                // Instantiate GA with concrete strategy objects
                TSPGA.SelectionStrategy selection = new TSPGA.TournamentSelection(5);
                TSPGA.CrossoverStrategy crossover = new TSPGA.OrderedCrossover();
                TSPGA.MutationStrategy mutation = new TSPGA.SwapMutation();
                solver = new TSPGA(instance, popSize, gens, selection, crossover, mutation);
            }

            System.out.println("\nSolving " + selectedResource + " with " + type + "...");
            long startTime = System.currentTimeMillis();
            solver.solve();
            long endTime = System.currentTimeMillis();
            System.out.println("...Done in " + (endTime - startTime) + " ms.");
            solver.printSolution();
            // 1. Get the solution object from the solver.
            PrintResults solution = solver.getSolution();
            // 2. Tell the solution object to print itself.
            solution.print();

        } catch (IOException | NumberFormatException e) {
            System.out.println("❌ Error running solver: " + e.getMessage());
        }
    }

    /**
     * Initializes and runs the full benchmark suite on the specified list of files.
     */
    private static void runFullBenchmark() {
        BenchmarkRunner runner = new BenchmarkRunner();
        runner.run();
        runner.printSummary();
        runner.displayChart();
    }

    private static void runEmpiricalTesting() {
        try {
            System.out.println("\nStarting empirical test for: " + selectedResource);
            TSPParser.TSPInstance instance = TSPParser.readFromResource(selectedResource);

            EmpiricalTesting tester = new EmpiricalTesting(instance);
            tester.runComparison(30);
            tester.printResults();

        } catch (IOException e) {
            System.out.println("❌ Error during empirical testing: " + e.getMessage());
        }
    }

    private static void runWithChart(Scanner scanner) {
        System.out.println("\nChoose algorithm for chart visualization:");
        System.out.println("1. Dynamic Programming");
        System.out.println("2. Genetic Algorithm");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();

        try {
            TSPParser.TSPInstance instance = TSPParser.readFromResource(selectedResource);
            TSPSolver solver = null;
            String algName = "";

            if ("1".equals(choice)) {
                solver = new TSPDP(instance);
                algName = "Dynamic Programming";
            } else if ("2".equals(choice)) {
                System.out.print("Enter population size: ");
                int popSize = Integer.parseInt(scanner.nextLine());
                System.out.print("Enter number of generations: ");
                int gens = Integer.parseInt(scanner.nextLine());

                // Instantiate GA with concrete strategy objects
                TSPGA.SelectionStrategy selection = new TSPGA.TournamentSelection(5);
                TSPGA.CrossoverStrategy crossover = new TSPGA.OrderedCrossover();
                TSPGA.MutationStrategy mutation = new TSPGA.SwapMutation();
                solver = new TSPGA(instance, popSize, gens, selection, crossover, mutation);
                algName = "Genetic Algorithm";
            } else {
                System.out.println("Invalid choice.");
                return;
            }

            final TSPSolver finalSolver = solver;
            final String finalAlgName = algName;

            SwingUtilities.invokeLater(() -> {
                new RealTimeChart(finalAlgName, finalSolver).showAndRun();
            });

        } catch (IOException | NumberFormatException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}