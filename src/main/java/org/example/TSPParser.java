package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses Traveling Salesman Problem (TSP) files in various formats.
 * This class is a crucial utility that abstracts the complexity of file I/O
 * and data interpretation. It can handle three types of files:
 * 1. Simple adjacency matrices (dimension on the first line).
 * 2. TSPLIB format with coordinate data (NODE_COORD_SECTION).
 * 3. TSPLIB format with an explicit full matrix (EDGE_WEIGHT_SECTION).
 * <p>
 * This version reads files as classpath resources, making the application
 * portable across different IDEs (like NetBeans and VS Code) and executable JARs.
 *
 * @author Ronil (S11231541)
 * @author Shivan (S11231502)
 * @author Praheel (S11229535)
 * @version 1.1, 20 September 2025
 */
public class TSPParser {

    /**
     * A public static inner class that encapsulates all the data for a single
     * TSP problem instance. It acts as a Data Transfer Object (DTO), holding the
     * final distance matrix and the original city coordinates if available.
     */
    public static class TSPInstance {
        private final double[][] distanceMatrix;
        private final List<double[]> coordinates;

        /**
         * Constructs a TSPInstance.
         * @param matrix The calculated distance matrix.
         * @param coordinates The list of city coordinates (can be null for matrix-based files).
         */
        public TSPInstance(double[][] matrix, List<double[]> coordinates) {
            this.distanceMatrix = matrix;
            this.coordinates = coordinates;
        }

        public int size() { return distanceMatrix.length; }
        public double getDistance(int i, int j) { return distanceMatrix[i][j]; }
        public double[][] getMatrix() { return distanceMatrix; }
        public List<double[]> getCoordinates() { return coordinates; }
    }

    /**
     * Loads a TSP instance from a file located in the project's resource directory.
     * This is the primary entry point for loading problem data.
     *
     * @param resourceName The name of the file (e.g., "burma14.tsp").
     * @return A new TSPInstance object.
     * @throws IOException If the resource cannot be found or read.
     */
    public static TSPInstance readFromResource(String resourceName) throws IOException {
        try (InputStream is = TSPParser.class.getResourceAsStream("/" + resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found in classpath: " + resourceName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                List<String> lines = reader.lines().collect(Collectors.toList());
                return loadFromLines(lines);
            }
        }
    }

    /**
     * A private helper that detects the format from a list of strings and routes
     * it to the correct specialized parser.
     *
     * @param lines The lines of text from the TSP file.
     * @return A new TSPInstance object.
     * @throws IOException If the file format is invalid or data is incomplete.
     */
    private static TSPInstance loadFromLines(List<String> lines) throws IOException {
        if (lines.isEmpty()) {
            throw new IOException("Input data is empty.");
        }

        boolean isTsplib = lines.stream().anyMatch(l -> l.contains("NAME:") || l.contains("TYPE:") || l.contains("DIMENSION:"));

        if (isTsplib) {
            if (lines.stream().anyMatch(l -> l.contains("NODE_COORD_SECTION"))) {
                return parseTsplibCoordFormat(lines);
            } else if (lines.stream().anyMatch(l -> l.contains("EDGE_WEIGHT_SECTION"))) {
                return new TSPInstance(parseTsplibMatrixFormat(lines), null);
            }
        }

        return new TSPInstance(parseSimpleMatrixFormat(lines, "input stream"), null);
    }

    /**
     * Parses TSPLIB files that define city locations by coordinates.
     * @param lines The content of the file.
     * @return A TSPInstance containing both the calculated distance matrix and the coordinates.
     */
    private static TSPInstance parseTsplibCoordFormat(List<String> lines) {
        List<double[]> coords = new ArrayList<>();
        String edgeWeightType = "EUC_2D";
        boolean coordSection = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("EDGE_WEIGHT_TYPE")) edgeWeightType = line.split(":")[1].trim();
            if (line.startsWith("NODE_COORD_SECTION")) { coordSection = true; continue; }
            if (line.startsWith("EOF")) break;
            if (coordSection && !line.isEmpty()) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    double rawLat = Double.parseDouble(parts[1]);
                    double rawLon = Double.parseDouble(parts[2]);
                    coords.add(new double[]{convertTsplibCoordToDecimal(rawLat), convertTsplibCoordToDecimal(rawLon)});
                }
            }
        }
        int n = coords.size();
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double[] p1 = coords.get(i);
                double[] p2 = coords.get(j);
                if ("GEO".equalsIgnoreCase(edgeWeightType)) {
                    dist[i][j] = calculateGeoDistance(p1[0], p1[1], p2[0], p2[1]);
                } else {
                    dist[i][j] = Math.sqrt(Math.pow(p1[0] - p2[0], 2) + Math.pow(p1[1] - p2[1], 2));
                }
            }
        }
        System.out.println("✅ Parsed TSPLIB coordinate format with " + n + " cities.");
        return new TSPInstance(dist, coords);
    }

    /**
     * Parses TSPLIB files that define distances with an explicit full matrix.
     * @param lines The content of the file.
     * @return The calculated distance matrix.
     * @throws IOException If the dimension is not found or the matrix is incomplete.
     */
    private static double[][] parseTsplibMatrixFormat(List<String> lines) throws IOException {
        int dimension = -1;
        List<Double> allNumbers = new ArrayList<>();
        boolean inMatrixSection = false;
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("DIMENSION")) dimension = Integer.parseInt(line.split(":")[1].trim());
            if (line.startsWith("EDGE_WEIGHT_SECTION")) { inMatrixSection = true; continue; }
            if (line.startsWith("EOF")) break;
            if (inMatrixSection) {
                String[] parts = line.split("\\s+");
                for (String part : parts) if (!part.isEmpty()) allNumbers.add(Double.parseDouble(part));
            }
        }
        if (dimension == -1) throw new IOException("Dimension not found in TSPLIB file.");
        if (allNumbers.size() < dimension * dimension) throw new IOException("Matrix data is incomplete.");
        double[][] dist = new double[dimension][dimension];
        int k = 0;
        for (int i = 0; i < dimension; i++) for (int j = 0; j < dimension; j++) dist[i][j] = allNumbers.get(k++);
        System.out.println("✅ Parsed TSPLIB matrix format with " + dimension + " cities.");
        return dist;
    }

    /**
     * Parses simple matrix files where the first line is the dimension.
     * @param lines The content of the file.
     * @param source A string indicating the source of the data (e.g., a filename).
     * @return The distance matrix.
     * @throws IOException If data rows are missing or incorrectly formatted.
     */
    private static double[][] parseSimpleMatrixFormat(List<String> lines, String source) throws IOException {
        int dimension = Integer.parseInt(lines.get(0).trim());
        double[][] dist = new double[dimension][dimension];
        int lineIndex = 1;
        for (int i = 0; i < dimension; i++) {
            while (lineIndex < lines.size() && lines.get(lineIndex).trim().isEmpty()) lineIndex++;
            if (lineIndex >= lines.size()) throw new IOException("Not enough data rows in " + source);
            String[] parts = lines.get(lineIndex).trim().split("[,\\s]+");
            if (parts.length < dimension) throw new IOException("Row " + (i + 1) + " has wrong number of values.");
            for (int j = 0; j < dimension; j++) dist[i][j] = Double.parseDouble(parts[j]);
            lineIndex++;
        }
        System.out.println("✅ Parsed simple matrix format with " + dimension + " cities.");
        return dist;
    }

    /**
     * Converts a TSPLIB coordinate from DDD.MM format to decimal degrees.
     */
    private static double convertTsplibCoordToDecimal(double tsplibCoord) {
        int degrees = (int) tsplibCoord;
        double minutes = tsplibCoord - degrees;
        return degrees + (minutes * 100.0) / 60.0;
    }

    /**
     * Calculates GEO distance using the official TSPLIB formula.
     */
    private static double calculateGeoDistance(double lat1_dd, double lon1_dd, double lat2_dd, double lon2_dd) {
        final double RRR = 6378.388;
        double lat1_rad = Math.toRadians(lat1_dd);
        double lon1_rad = Math.toRadians(lon1_dd);
        double lat2_rad = Math.toRadians(lat2_dd);
        double lon2_rad = Math.toRadians(lon2_dd);
        double angle = Math.acos(Math.sin(lat1_rad) * Math.sin(lat2_rad) +
                Math.cos(lat1_rad) * Math.cos(lat2_rad) * Math.cos(lon1_rad - lon2_rad));
        return Math.round(RRR * angle);
    }
}

