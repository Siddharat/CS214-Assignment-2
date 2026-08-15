package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

/**
 * A class that encapsulates the complete solution of a TSP solver and is
 * responsible for presenting its results. This version has been enhanced to
 * print to the console and simultaneously save the results to a CSV file.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 22 September 2025
 */
public class PrintResults {

    private final String algorithmName;
    private final double tourLength;
    private final List<Integer> tourPath;
    private final TSPParser.TSPInstance problemInstance;

    public PrintResults(String algorithmName, double tourLength, List<Integer> tourPath, TSPParser.TSPInstance problemInstance) {
        this.algorithmName = algorithmName;
        this.tourLength = tourLength;
        this.tourPath = tourPath;
        this.problemInstance = problemInstance;
    }

    /**
     * Prints a comprehensive summary to the console and saves the same data
     * to a file named "Results.csv". This is the main public method to call.
     */
    public void print() {
        // --- 1. Print to Console (existing logic) ---
        printToConsole();

        // --- 2. Save to CSV File (new logic) ---
        saveToCsv("Results.csv");
    }

    /**
     * Handles printing the formatted output to the standard console.
     */
    private void printToConsole() {
        System.out.printf(Locale.US, "\n%s best tour length: %.2f%n", algorithmName, tourLength);

        printDistanceMatrix();

        if (tourPath == null || tourPath.isEmpty()) {
            System.out.println("\nNo path found.");
            return;
        }

        System.out.println("\nOptimal Path:");
        List<double[]> coordinates = problemInstance.getCoordinates();
        boolean hasCoords = coordinates != null && !coordinates.isEmpty();

        // Iterate through the tour and print each city.
        for (int cityId : tourPath) {
            System.out.print("  -> City " + (cityId + 1));
            // If coordinates are available, print them next to the city ID.
            if (hasCoords) {
                double[] coords = coordinates.get(cityId);
                System.out.printf(Locale.US, " (Lat: %.2f, Lon: %.2f)%n", coords[0], coords[1]);
            } else {
                System.out.println();
            }
        }

        // Print the return trip to the starting city to complete the loop.
        int startCityId = tourPath.get(0);
        System.out.print("  -> City " + (startCityId + 1));
        if (hasCoords) {
            double[] coords = coordinates.get(startCityId);
            System.out.printf(Locale.US, " (Lat: %.2f, Lon: %.2f)%n", coords[0], coords[1]);
        } else {
            System.out.println();
        }
    }

    /**
     * Saves the distance matrix and optimal path to a specified CSV file.
     * @param filename The name of the file to save the results to.
     */
    private void saveToCsv(String filename) {
        // Use try-with-resources to ensure the writer is closed automatically.
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // --- Write Matrix to CSV ---
            writer.println("--- Distance Matrix ---");
            double[][] matrix = problemInstance.getMatrix();
            if (matrix != null && matrix.length > 0) {
                // Create and write the header row (e.g., From/To,1,2,3...).
                StringBuilder header = new StringBuilder("From/To,");
                for (int i = 0; i < matrix.length; i++) {
                    header.append(i + 1).append(i == matrix.length - 1 ? "" : ",");
                }
                writer.println(header);

                // Write each row of the matrix data.
                for (int i = 0; i < matrix.length; i++) {
                    StringBuilder row = new StringBuilder((i + 1) + ",");
                    for (int j = 0; j < matrix[i].length; j++) {
                        double dist = matrix[i][j];
                        // Replace large numbers (infinity) with "inf" for clarity.
                        String val = (dist > 99999) ? "inf" : String.format(Locale.US, "%.0f", dist);
                        row.append(val).append(j == matrix[i].length - 1 ? "" : ",");
                    }
                    writer.println(row);
                }
            }

            // --- Write Path to CSV ---
            writer.println(); // Add a blank line for separation.
            writer.println("--- Optimal Path ---");
            writer.printf(Locale.US, "Algorithm,%s\n", algorithmName);
            writer.printf(Locale.US, "Best Tour Length,%.2f\n", tourLength);

            if (tourPath != null && !tourPath.isEmpty()) {
                List<double[]> coordinates = problemInstance.getCoordinates();
                boolean hasCoords = coordinates != null && !coordinates.isEmpty();

                // Write a different header row depending on whether coordinate data exists.
                if (hasCoords) {
                    writer.println("Step,City ID,Latitude,Longitude");
                } else {
                    writer.println("Step,City ID");
                }

                int step = 1;
                for (int cityId : tourPath) {
                    if (hasCoords) {
                        double[] coords = coordinates.get(cityId);
                        writer.printf(Locale.US, "%d,%d,%.2f,%.2f\n", step++, cityId + 1, coords[0], coords[1]);
                    } else {
                        writer.printf("%d,%d\n", step++, cityId + 1);
                    }
                }
                // Manually add the starting city again to show a complete loop in the path.
                int startCityId = tourPath.get(0);
                if (hasCoords) {
                    double[] coords = coordinates.get(startCityId);
                    writer.printf(Locale.US, "%d,%d,%.2f,%.2f\n", step, startCityId + 1, coords[0], coords[1]);
                } else {
                    writer.printf("%d,%d\n", step, startCityId + 1);
                }
            }
            System.out.println("\n✅ Results also saved to " + filename);

        } catch (IOException e) {
            System.err.println("❌ Error: Failed to write results to CSV file: " + e.getMessage());
        }
    }

    /**
     * A private helper method that formats and prints the full distance matrix
     * of the problem instance to the console.
     */
    private void printDistanceMatrix() {
        System.out.println("\n--- Distance Matrix ---");
        double[][] matrix = problemInstance.getMatrix();
        if (matrix == null || matrix.length == 0) {
            System.out.println("No distance matrix available to print.");
            return;
        }

        // Print header row with city numbers, properly formatted.
        System.out.printf("%-7s", "From/To");
        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%7d", i + 1);
        }
        System.out.println();
        System.out.println("-------" + "+-------".repeat(matrix.length));

        // Print each row of the matrix with formatted numbers.
        for (int i = 0; i < matrix.length; i++) {
            System.out.printf("%-7d|", i + 1); // Row label
            for (int j = 0; j < matrix[i].length; j++) {
                double dist = matrix[i][j];
                // Display a placeholder for infinity to improve readability.
                if (dist > 99999) {
                    System.out.printf("%7s", "inf");
                } else {
                    System.out.printf(Locale.US, "%7.0f", dist);
                }
            }
            System.out.println();
        }
        System.out.println("-----------------------");
    }
}