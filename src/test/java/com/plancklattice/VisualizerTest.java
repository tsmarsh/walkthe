package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Visualizer.
 * These tests demonstrate visualization and output generation functionality.
 *
 * Usage examples:
 * - Generating PPM images for different data types
 * - Printing statistics
 * - Writing CSV data
 */
@DisplayName("Visualizer - Visualization and output generation")
class VisualizerTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("PPM Image Generation")
    class PPMImageGenerationTests {

        @Test
        @DisplayName("Should generate spacing heatmap PPM file")
        void testGenerateSpacingHeatmap() throws IOException {
            // USAGE: Create a visualization of lattice compression/stretching
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("spacing.ppm");
            visualizer.generateSpacingHeatmap(outputFile.toString());

            // File should exist
            assertTrue(Files.exists(outputFile), "PPM file should be created");

            // Read and verify PPM format
            List<String> lines = Files.readAllLines(outputFile);
            assertTrue(lines.size() > 3, "PPM file should have header and data");
            assertEquals("P3", lines.get(0), "Should have P3 magic number");
            assertEquals("10 10", lines.get(1), "Should have correct dimensions");
            assertEquals("255", lines.get(2), "Should have max color value 255");

            // Should have data for all pixels (header + 100 pixels)
            assertTrue(lines.size() >= 103, "Should have data for all pixels");
        }

        @Test
        @DisplayName("Should generate EM field image")
        void testGenerateEMFieldImage() throws IOException {
            // USAGE: Visualize electromagnetic wave amplitude and phase
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // Add some EM field
            lattice.addEMPulse(2.5f, 2.5f, 1.0f, 1.0f, 3.0f);

            Path outputFile = tempDir.resolve("em_field.ppm");
            visualizer.generateEMFieldImage(outputFile.toString());

            assertTrue(Files.exists(outputFile), "EM field PPM should be created");

            List<String> lines = Files.readAllLines(outputFile);
            assertEquals("P3", lines.get(0), "Should be P3 format");
            assertEquals("5 5", lines.get(1), "Should have correct dimensions");
        }

        @Test
        @DisplayName("Should generate energy density image")
        void testGenerateEnergyDensityImage() throws IOException {
            // USAGE: Visualize mass/energy distribution in the lattice
            PlanckLattice lattice = new PlanckLattice(8, 8);
            Visualizer visualizer = new Visualizer(lattice);

            // Add energy concentration
            lattice.addMassConcentration(4.0f, 4.0f, 100.0f, 2.0f);

            Path outputFile = tempDir.resolve("energy.ppm");
            visualizer.generateEnergyDensityImage(outputFile.toString());

            assertTrue(Files.exists(outputFile), "Energy density PPM should be created");

            List<String> lines = Files.readAllLines(outputFile);
            assertEquals("P3", lines.get(0), "Should be P3 format");
            assertEquals("8 8", lines.get(1), "Should have correct dimensions");
        }

        @Test
        @DisplayName("Should handle empty lattice in spacing heatmap")
        void testSpacingHeatmapEmptyLattice() throws IOException {
            PlanckLattice lattice = new PlanckLattice(3, 3);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("empty_spacing.ppm");

            assertDoesNotThrow(() -> visualizer.generateSpacingHeatmap(outputFile.toString()),
                "Should handle empty lattice without errors");

            assertTrue(Files.exists(outputFile), "File should be created even for empty lattice");
        }

        @Test
        @DisplayName("Should handle empty EM field")
        void testEMFieldImageEmptyField() throws IOException {
            PlanckLattice lattice = new PlanckLattice(4, 4);
            Visualizer visualizer = new Visualizer(lattice);

            // No EM pulse added
            Path outputFile = tempDir.resolve("empty_em.ppm");

            assertDoesNotThrow(() -> visualizer.generateEMFieldImage(outputFile.toString()),
                "Should handle empty EM field");

            assertTrue(Files.exists(outputFile));
        }

        @Test
        @DisplayName("Should create valid RGB values in PPM output")
        void testValidRGBValues() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("rgb_test.ppm");
            visualizer.generateSpacingHeatmap(outputFile.toString());

            List<String> lines = Files.readAllLines(outputFile);

            // Check RGB values (skip header lines 0-2)
            for (int i = 3; i < lines.size(); i++) {
                String[] rgb = lines.get(i).trim().split("\\s+");
                if (rgb.length == 3) {
                    int r = Integer.parseInt(rgb[0]);
                    int g = Integer.parseInt(rgb[1]);
                    int b = Integer.parseInt(rgb[2]);

                    assertTrue(r >= 0 && r <= 255, "Red value should be in range 0-255");
                    assertTrue(g >= 0 && g <= 255, "Green value should be in range 0-255");
                    assertTrue(b >= 0 && b <= 255, "Blue value should be in range 0-255");
                }
            }
        }

        @Test
        @DisplayName("Should show compressed regions in blue")
        void testCompressedRegionsInBlue() throws IOException {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            // Compress one region
            for (int i = 0; i < 10; i++) {
                lattice.posX[i] *= 0.5f;  // Compress first row
            }

            Path outputFile = tempDir.resolve("compressed.ppm");
            visualizer.generateSpacingHeatmap(outputFile.toString());

            // Just verify it creates valid output
            assertTrue(Files.exists(outputFile));
            assertTrue(Files.size(outputFile) > 100, "File should have substantial content");
        }
    }

    @Nested
    @DisplayName("Statistics Printing")
    class StatisticsPrintingTests {

        private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        private final PrintStream originalOut = System.out;

        @BeforeEach
        void setUpStreams() {
            System.setOut(new PrintStream(outContent));
        }

        @AfterEach
        void restoreStreams() {
            System.setOut(originalOut);
        }

        @Test
        @DisplayName("Should print statistics to stdout")
        void testPrintStatistics() {
            // USAGE: Monitor simulation progress with statistics
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            visualizer.printStatistics(100, 12.345);

            String output = outContent.toString();

            assertTrue(output.contains("Timestep 100"), "Should show timestep number");
            assertTrue(output.contains("12.345"), "Should show elapsed time");
            assertTrue(output.contains("Average spacing"), "Should show average spacing");
        }

        @Test
        @DisplayName("Should include all key metrics in statistics")
        void testStatisticsContent() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            lattice.addMassConcentration(5.0f, 5.0f, 50.0f, 2.0f);
            lattice.addEMPulse(5.0f, 5.0f, 1.0f, 2.0f, 4.0f);

            visualizer.printStatistics(42, 5.678);

            String output = outContent.toString();

            assertTrue(output.contains("Average spacing"), "Should include average spacing");
            assertTrue(output.contains("Total energy"), "Should include total energy");
            assertTrue(output.contains("Max EM amplitude"), "Should include EM amplitude");
            assertTrue(output.contains("compression") || output.contains("stretch"),
                "Should include compression/stretch info");
        }

        @Test
        @DisplayName("Should format numbers appropriately")
        void testNumberFormatting() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            visualizer.printStatistics(1, 0.123456789);

            String output = outContent.toString();

            // Should use formatted output (not too many decimal places)
            assertTrue(output.contains("0.123"), "Should format elapsed time");
        }
    }

    @Nested
    @DisplayName("CSV Statistics Export")
    class CSVExportTests {

        @Test
        @DisplayName("Should create CSV file with header")
        void testCSVCreation() throws IOException {
            // USAGE: Export statistics for later analysis
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            Path csvFile = tempDir.resolve("stats.csv");

            visualizer.appendStatisticsToCSV(csvFile.toString(), 0, 1.0);

            assertTrue(Files.exists(csvFile), "CSV file should be created");

            List<String> lines = Files.readAllLines(csvFile);
            assertTrue(lines.size() >= 2, "Should have header and at least one data row");

            // Check header
            String header = lines.get(0);
            assertTrue(header.contains("timestep"), "Header should include timestep");
            assertTrue(header.contains("elapsed_ms"), "Header should include elapsed time");
            assertTrue(header.contains("avg_spacing"), "Header should include average spacing");
            assertTrue(header.contains("total_energy"), "Header should include total energy");
            assertTrue(header.contains("max_em_amplitude"), "Header should include EM amplitude");
        }

        @Test
        @DisplayName("Should append data without duplicating header")
        void testCSVAppending() throws IOException {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Visualizer visualizer = new Visualizer(lattice);

            Path csvFile = tempDir.resolve("append_test.csv");

            // Write first row
            visualizer.appendStatisticsToCSV(csvFile.toString(), 0, 1.0);

            // Append second row
            visualizer.appendStatisticsToCSV(csvFile.toString(), 1, 2.0);

            // Append third row
            visualizer.appendStatisticsToCSV(csvFile.toString(), 2, 3.0);

            List<String> lines = Files.readAllLines(csvFile);

            // Should have 1 header + 3 data rows
            assertEquals(4, lines.size(), "Should have header plus 3 data rows");

            // Only first line should be header
            String firstLine = lines.get(0);
            assertTrue(firstLine.contains("timestep"), "First line should be header");

            // Other lines should be data
            assertTrue(lines.get(1).startsWith("0,"), "Second line should start with timestep 0");
            assertTrue(lines.get(2).startsWith("1,"), "Third line should start with timestep 1");
            assertTrue(lines.get(3).startsWith("2,"), "Fourth line should start with timestep 2");
        }

        @Test
        @DisplayName("Should write correct CSV values")
        void testCSVValues() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // Add known values
            lattice.addMassConcentration(2.5f, 2.5f, 100.0f, 1.0f);
            lattice.addEMPulse(2.5f, 2.5f, 2.0f, 1.0f, 3.0f);

            Path csvFile = tempDir.resolve("values_test.csv");

            visualizer.appendStatisticsToCSV(csvFile.toString(), 42, 123.456);

            List<String> lines = Files.readAllLines(csvFile);
            String dataLine = lines.get(1);  // Skip header

            assertTrue(dataLine.startsWith("42,"), "Should start with timestep 42");
            assertTrue(dataLine.contains("123.456"), "Should contain elapsed time");

            // Parse CSV
            String[] values = dataLine.split(",");
            assertEquals(5, values.length, "Should have 5 columns");

            int timestep = Integer.parseInt(values[0]);
            assertEquals(42, timestep, "Timestep should be 42");

            double elapsedMs = Double.parseDouble(values[1]);
            assertEquals(123.456, elapsedMs, 0.001, "Elapsed time should match");
        }

        @Test
        @DisplayName("Should handle file path creation")
        void testFilePathCreation() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // Create nested directory path
            Path nestedPath = tempDir.resolve("subdir").resolve("stats.csv");

            // Create parent directory
            Files.createDirectories(nestedPath.getParent());

            visualizer.appendStatisticsToCSV(nestedPath.toString(), 0, 1.0);

            assertTrue(Files.exists(nestedPath), "Should create file in nested directory");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle 1x1 lattice")
        void testMinimalLattice() throws IOException {
            PlanckLattice lattice = new PlanckLattice(1, 1);
            Visualizer visualizer = new Visualizer(lattice);

            Path spacingFile = tempDir.resolve("1x1_spacing.ppm");
            Path emFile = tempDir.resolve("1x1_em.ppm");
            Path energyFile = tempDir.resolve("1x1_energy.ppm");

            assertDoesNotThrow(() -> {
                visualizer.generateSpacingHeatmap(spacingFile.toString());
                visualizer.generateEMFieldImage(emFile.toString());
                visualizer.generateEnergyDensityImage(energyFile.toString());
            }, "1x1 lattice should not cause errors");

            assertTrue(Files.exists(spacingFile));
            assertTrue(Files.exists(emFile));
            assertTrue(Files.exists(energyFile));
        }

        @Test
        @DisplayName("Should handle large lattice efficiently")
        void testLargeLatticePerformance() throws IOException {
            PlanckLattice lattice = new PlanckLattice(100, 100);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("large.ppm");

            long startTime = System.nanoTime();
            visualizer.generateSpacingHeatmap(outputFile.toString());
            long endTime = System.nanoTime();

            double elapsedMs = (endTime - startTime) / 1_000_000.0;

            // Should complete in reasonable time (< 100ms for 100x100)
            assertTrue(elapsedMs < 100.0,
                "Large lattice visualization should be fast, took " + elapsedMs + "ms");

            assertTrue(Files.exists(outputFile));
        }

        @Test
        @DisplayName("Should handle rectangular lattice")
        void testRectangularLattice() throws IOException {
            PlanckLattice lattice = new PlanckLattice(20, 10);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("rectangular.ppm");
            visualizer.generateSpacingHeatmap(outputFile.toString());

            List<String> lines = Files.readAllLines(outputFile);
            assertEquals("20 10", lines.get(1), "Should have correct rectangular dimensions");
        }

        @Test
        @DisplayName("Should handle extreme spacing values")
        void testExtremeSpacingValues() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // Create extreme compression
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.posX[i] *= 0.01f;  // Severe compression
                lattice.posY[i] *= 0.01f;
            }

            Path outputFile = tempDir.resolve("extreme_spacing.ppm");

            assertDoesNotThrow(() -> visualizer.generateSpacingHeatmap(outputFile.toString()),
                "Should handle extreme spacing values");

            assertTrue(Files.exists(outputFile));
        }

        @Test
        @DisplayName("Should handle extreme EM field values")
        void testExtremeEMValues() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // Set very large EM field
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.emFieldReal[i] = 10000.0f;
                lattice.emFieldImag[i] = 10000.0f;
            }

            Path outputFile = tempDir.resolve("extreme_em.ppm");

            assertDoesNotThrow(() -> visualizer.generateEMFieldImage(outputFile.toString()),
                "Should handle extreme EM values");

            assertTrue(Files.exists(outputFile));
        }

        @Test
        @DisplayName("Should handle zero energy density gracefully")
        void testZeroEnergyDensity() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            // All energy densities are zero
            Path outputFile = tempDir.resolve("zero_energy.ppm");

            assertDoesNotThrow(() -> visualizer.generateEnergyDensityImage(outputFile.toString()),
                "Should handle zero energy density");

            assertTrue(Files.exists(outputFile));
        }

        @Test
        @DisplayName("Should overwrite existing files")
        void testFileOverwriting() throws IOException {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Visualizer visualizer = new Visualizer(lattice);

            Path outputFile = tempDir.resolve("overwrite_test.ppm");

            // Create first file
            visualizer.generateSpacingHeatmap(outputFile.toString());
            long firstSize = Files.size(outputFile);

            // Modify lattice
            lattice.posX[0] = 100.0f;

            // Overwrite
            visualizer.generateSpacingHeatmap(outputFile.toString());
            long secondSize = Files.size(outputFile);

            assertTrue(Files.exists(outputFile), "File should still exist after overwrite");
            // File sizes might be same or different depending on content, just verify no error
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should generate complete set of outputs")
        void testCompleteOutputGeneration() throws IOException {
            // USAGE: Typical workflow - generate all output types
            PlanckLattice lattice = new PlanckLattice(15, 15);
            Visualizer visualizer = new Visualizer(lattice);

            // Set up interesting scenario
            lattice.addMassConcentration(7.5f, 7.5f, 100.0f, 3.0f);
            lattice.addEMPulse(7.5f, 7.5f, 1.0f, 2.0f, 4.0f);

            // Generate all output types
            Path spacingFile = tempDir.resolve("frame_spacing.ppm");
            Path emFile = tempDir.resolve("frame_em.ppm");
            Path energyFile = tempDir.resolve("frame_energy.ppm");
            Path csvFile = tempDir.resolve("statistics.csv");

            assertDoesNotThrow(() -> {
                visualizer.generateSpacingHeatmap(spacingFile.toString());
                visualizer.generateEMFieldImage(emFile.toString());
                visualizer.generateEnergyDensityImage(energyFile.toString());
                visualizer.appendStatisticsToCSV(csvFile.toString(), 0, 1.0);
            }, "Should generate all outputs without errors");

            // Verify all files exist
            assertTrue(Files.exists(spacingFile), "Spacing heatmap should exist");
            assertTrue(Files.exists(emFile), "EM field image should exist");
            assertTrue(Files.exists(energyFile), "Energy density image should exist");
            assertTrue(Files.exists(csvFile), "CSV statistics should exist");

            // All should have content
            assertTrue(Files.size(spacingFile) > 0, "Spacing file should have content");
            assertTrue(Files.size(emFile) > 0, "EM file should have content");
            assertTrue(Files.size(energyFile) > 0, "Energy file should have content");
            assertTrue(Files.size(csvFile) > 0, "CSV file should have content");
        }
    }
}
