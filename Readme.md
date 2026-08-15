# CS214 — Assignment II: Traveling Salesman Problem Solver

Programmers:Ronil Prasad (S11231541)
            Shivan Prasad(S11231502)
            Praheel Kumar (S11229535)  

Course: CS214 — Design & Analysis of Algorithms  
University: The University of the South Pacific  
Date: 26 September 2025

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Maven](https://img.shields.io/badge/Maven-3.8+-orange.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

                     Overview 

A comprehensive and extensible Java application for solving the Traveling Salesman Problem (TSP) using various algorithms. This project features a command-line interface (CLI) for user interaction, real-time performance visualization, and a robust benchmarking suite. It is designed with a strong emphasis on modern Object-Oriented principles, including the Strategy pattern, SOLID principles, and advanced concurrency.

                    Table of Content
- Features
- Core Concepts & Design
- Project Structure
- Sample Images
- Getting Started
  - Dependencies
- Usage
  - Main Menu
  - Single File Testing
  - Full Benchmark Suite
- File Formats Supported
- Credits & References


                        Features 

                Multiple Solver Algorithms:
Dynamic Programming (Held-Karp): An exact algorithm that guarantees the optimal solution. Best suited for smaller TSP instances (n < 22).

Genetic Algorithm (GA): A sophisticated heuristic solver for larger or more complex instances.

For optimal path and tour where GA = DP, the population size is: 50 and number of generation is: 200

                Extensible Algorithm Design:
The Genetic Algorithm uses the Strategy Pattern, allowing `Selection`, `Crossover`, and `Mutation` behaviors to be injected at runtime for maximum flexibility.
Includes concrete strategies like `TournamentSelection`, `OrderedCrossover`, and `SwapMutation`.

                Advanced Concurrency:
Multi-threaded benchmark runner to execute tests for multiple files in parallel.
Parallelized fitness evaluation within the Genetic Algorithm to leverage multi-core processors.
Concurrent execution of empirical test runs to gather statistical data efficiently.

                Powerful Benchmarking & Analysis:
Empirical Testing: Compare DP and GA on a single file, running each multiple times to gather statistics (Best, Mean, Worst, Success Rate, Avg. NFC).
Full Benchmark Suite: Run a comprehensive benchmark across a predefined set of TSP problems, intelligently skipping DP for infeasibly large instances.

                Rich Data Visualization:
Real-Time Progress Chart: See a solver's progress live, plotting tour distance against the number of function calls (NFC).
Benchmark Summary Chart: View a final bar chart comparing the performance of DP (optimal) vs. GA (best and mean) across all benchmarked problems.

                Versatile File Parsing:
Supports multiple TSP formats, including TSPLIB coordinate-based files (`NODE_COORD_SECTION`), TSPLIB matrix-based files (`EDGE_WEIGHT_SECTION`), and simple adjacency matrices.
Correctly calculates distances for both Euclidean (`EUC_2D`) and Geographical (`GEO`) coordinate systems.

                    Core Concepts

Interface-Based Design: The `TSPSolver` interface defines a clear contract for all solving algorithms, allowing them to be used interchangeably throughout the application (Liskov Substitution Principle).

Inheritance & Code Reuse: The `AbstractTSPSolver` class provides shared boilerplate logic, such as Number of Function Calls (NFC) tracking and progress reporting, to all concrete solver implementations.

Strategy Pattern: `TSPGA` is decoupled from its specific component algorithms. New selection, crossover, or mutation techniques can be added by simply creating a new class that implements the relevant strategy interface, without modifying the `TSPGA` class itself (Open/Closed Principle).

Concurrency & Parallelism: The application makes extensive use of Java's `ExecutorService` to manage thread pools for performance-critical tasks, such as running benchmarks and calculating fitness in the GA. This demonstrates a modern approach to leveraging multi-core hardware.

Observer Pattern (via Callbacks): The `RealTimeChart` "observes" the `TSPSolver` by passing a `Consumer` callback. The solver reports its progress by invoking this callback, decoupling the algorithm's logic from the GUI.

Encapsulation: Critical logic, like the NFC counter, is encapsulated within the `AbstractTSPSolver` and can only be incremented through a protected helper method, ensuring data integrity.

                The Project Structure

.idea
  ├──dictionaries
  |        ├──project.xml
  ├──.gitignore
  ├──compiler.xml
  ├──encodings.xml
  ├──jarRepositories.xml
  ├──misc.xml
  ├──vcs.xml
  ├──workspace.xml
.mvn
.vscode
  ├──settings.json
Developers
  ├──Assignment 2 - Mark Allocation
images
  ├──image-1.png   # Image of Sub Menu
  ├──image-2.png   # Genetic Algorithm's Graph
  ├──image-3.png   # Dynamic Pogramming Graph
  ├──image-4.png   # Benchmark Graph
  ├──image.png     # Image of the Main Menu Display

src
  ├── main
  │   ├── java/org/example
  │   │   ├── App.java               # Main entry point, CLI
  │   │   ├── TSPSolver.java         # Solver interface
  │   │   ├── AbstractTSPSolver.java # Base class for solvers
  │   │   ├── TSPDP.java             # Dynamic Programming solver
  │   │   ├── TSPGA.java             # Genetic Algorithm solver (with Strategy interfaces)
  │   │   ├── TSPParser.java         # TSP file parsing logic
  │   │   ├── BenchmarkRunner.java   # Runs the full benchmark suite
  │   │   ├── EmpiricalTesting.java  # Compares algorithms on one file
  │   │   ├── RealTimeChart.java     # Charting utility using JFreeChart
  │   │   └── PrintResults.java      # DTO for printing solutions
  │   └── resources
  │       ├── burma14.tsp
  │       └── testFile.atsp
  │       ├── br17.tsp
  │       └── ft53.atsp
  │       ├── ftv33.tsp
  │       └── ft70.atsp
  └── test/org/example
      ├── AppTest.java   # Unit tests
target
  ├──clasess
      ├──org/example
          ├──AbstractTSPSolver.class
          ├──App.class
          ├──BenchmarkRunner.class
          ├──BenchmarkRunner$BenchmarkResult.class    # DTO for aggregated benchmark metrics
          ├──BenchmarkRunner$BenchmarkTask.class
          ├──BenchmarkRunner$BenchmarkTask$GA_Results.class
          ├──EmpiricalTesting.class
          ├──EmpiricalTesting$GAResult.class
          ├──EmpiricalTesting$Stats.class
          ├──PrintResults.class
          ├──RealTimeChart.class
          ├──RealTimeChart$1.class
          ├──RealTimeChart$DataPoint.class    # DTO for Observer Pattern (Cost vs. NFC)
          ├──TSPDP.class
          ├──TSPGA.class
          ├──TSPGA$CrossoverStrategy.class
          ├──TSPGA$MutationStrategy.class
          ├──TSPGA$OrderedCrossover.class     # Concrete Crossover Strategy (Strategy Pattern)
          ├──TSPGA$SelectionStrategy.class
          ├──TSPGA$SwapMutation.class
          ├──TSPGA$TorunamentSelection.class
          ├──TSPParser.class
          ├──TSPParser$TSPInstance.class      # Data Transfer Object (DTO) for Problem Data
          ├──TSPSolver.class
      ├──br17.atsp
      ├──burma14.tsp
      ├──ft53.atsp
      ├──ft70.atsp
      ├──ftv33.atsp
      ├──testFile.atsp
  ├──test-classes/org/example
      ├──AppTest.class    
.gitignore
pom.xml
Readme.md
Results.csv        # Holds the printed out results in matrix form

                  Sample Images 

The application is controlled through a simple and intuitive CLI menu system.

![Image of the Main Menu Display](images/image.png)

![Image of Sub Menu](images/image-1.png)

            Real Time Chart Progression Example

Visualize the Genetic Algorithm's convergence towards a solution in real-time.

![Genetic Algorithm's Graph](images/image-2.png)

Visualize the Dynamic Programming flatuactuation towards a solution in real-time.

![Dynamic Programming Graph](images/image-3.png)

                Benchmark Summary Chart

Compare the final results of the benchmark suite with a summary bar chart.

![Benchmark Graph](images/image-4.png)



            Getting Started with the program

Open the project in an IDE preferably - VS Code/Intellij/ Netbeans.
Refresh the pom.xml file to download all dependencies.
Run the main class corresponding to the task you wish to execute (Refer to Structure section for detailed View).

                        Dependencies

Java 17+
Maven 3.8+
JFreeChart 1.5.4
JCommon 1.0.24
JUnit 4.13.2 (for testing)

                    Usage
                    Main Menu
Once the application is running, you will be presented with the main menu.


1. Select a File for Single Tests: This option lets you choose a TSP file (`burma14.tsp` or `testFile.atsp`) and then proceed to a sub-menu of actions for that specific file.
2. Run Full Benchmark Suite: This automatically runs a comprehensive performance test on a predefined list of TSP files and displays a summary table and chart.
3. Exit Program: Terminates the application.

                Single File Testing
After selecting a file, you can perform the following actions:

1. Solve with Dynamic Programming: Runs the exact Held-Karp algorithm.
2. Solve with Genetic Algorithm: Runs the heuristic GA. You will be asked for population size and number of generations.
3. Run Empirical Testing: Compares DP vs GA over 30 runs and prints the statistical results.
4. Solve with Real-Time Chart: Solves with either DP or GA and displays the live progress chart.

                Full Benchmark Suite

This option requires no further input. It will:
1.  Run benchmarks in parallel for `testFile.atsp`, `br17.atsp`,`ftv33.atsp`,`ft53.atsp`, and `ft70.atsp`.
2.  Print a detailed summary table to the console.
3.  Display a final bar chart comparing the results.

                File Formats Supported

The `TSPParser` can automatically detect and parse the following formats:

1. TSPLIB Coordinate Format: Files with a `NODE_COORD_SECTION` and `EDGE_WEIGHT_TYPE` of `EUC_2D` or `GEO`.
2. TSPLIB Full Matrix Format: Files with an `EDGE_WEIGHT_SECTION` containing an explicit distance matrix.
3. Simple Matrix Format: A custom format where the first line is the dimension `N`, followed by `N` lines of `N` space or comma-separated distance values.

                Credits & References
1. Dynamic Programming source - Tran, H.L. and Duong, M.P., 2024. Approach to Travelling Salesman Problem using Dynamic Programming and Branch-and-Bound technique. Research proposal. University of Technology Sydney. Available at: https://www.researchgate.net/publication/389855682 [Accessed 21 September 2025].
2. Genetic Algorithm source - Li, H., 2025. Solving the TSP Problem Based on Improved Genetic Algorithm. Proceedings of the 5th International Conference on Signal Processing and Machine Learning. Harbin Engineering University. DOI: 10.54254/2755-2721/133/2025.20603.
3. Gemini - AI model by Google. Used as coding partner.
4. GitHub Copilot - Plugin for VS Code. Used for generating methods.