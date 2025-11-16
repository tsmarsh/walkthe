package com.plancklattice;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Visualization for 3D discrete quantum lattice.
 * Shows Z-slices through the 3D volume.
 */
public class DiscreteVisualizer3D {

    private final DiscreteLattice3D lattice;

    public DiscreteVisualizer3D(DiscreteLattice3D lattice) {
        this.lattice = lattice;
    }

    /**
     * Generate PPM image of a single Z-slice.
     * Shows the XY plane at given Z depth.
     */
    public void generateEnergySlicePPM(String filename, int zSlice) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    int energy = lattice.getEnergy(x, y, zSlice);
                    int gray = (energy * 255) / 3;
                    writer.println(gray + " " + gray + " " + gray);
                }
            }
        }
    }

    /**
     * Generate PPM showing max projection along Z axis.
     * Each XY pixel shows the maximum energy across all Z slices.
     */
    public void generateMaxProjectionPPM(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    // Find max energy across all Z slices at this X,Y
                    int maxEnergy = 0;
                    for (int z = 0; z < lattice.gridDepth; z++) {
                        maxEnergy = Math.max(maxEnergy, lattice.getEnergy(x, y, z));
                    }

                    int gray = (maxEnergy * 255) / 3;
                    writer.println(gray + " " + gray + " " + gray);
                }
            }
        }
    }

    /**
     * Generate PPM showing sum projection along Z axis.
     * Each XY pixel shows the sum of energy across all Z slices.
     */
    public void generateSumProjectionPPM(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    // Sum energy across all Z slices at this X,Y
                    int sumEnergy = 0;
                    for (int z = 0; z < lattice.gridDepth; z++) {
                        sumEnergy += lattice.getEnergy(x, y, z);
                    }

                    // Normalize to 0-255 range
                    int gray = Math.min(255, (sumEnergy * 255) / (3 * lattice.gridDepth));
                    writer.println(gray + " " + gray + " " + gray);
                }
            }
        }
    }

    /**
     * Print ASCII visualization of center Z-slice.
     */
    public void printCenterSliceASCII() {
        int centerZ = lattice.gridDepth / 2;
        System.out.println("Center Z-slice (" + centerZ + "/" + lattice.gridDepth + "):");
        System.out.println("Energy Distribution (. = vacuum, ░ = 1q, ▒ = 2q, ▓ = 3q):");
        System.out.println("┌" + "─".repeat(lattice.gridWidth) + "┐");

        for (int y = 0; y < lattice.gridHeight; y++) {
            System.out.print("│");
            for (int x = 0; x < lattice.gridWidth; x++) {
                int energy = lattice.getEnergy(x, y, centerZ);
                char symbol = switch (energy) {
                    case 0 -> '·';
                    case 1 -> '░';
                    case 2 -> '▒';
                    case 3 -> '▓';
                    default -> '?';
                };
                System.out.print(symbol);
            }
            System.out.println("│");
        }

        System.out.println("└" + "─".repeat(lattice.gridWidth) + "┘");
    }

    /**
     * Print statistics.
     */
    public void printStats() {
        int total = lattice.getTotalEnergy();
        float avg = lattice.getAverageEnergy();

        int[] counts = new int[4];
        for (int i = 0; i < lattice.totalSites; i++) {
            counts[lattice.energyLevel[i]]++;
        }

        System.out.println("=== 3D Lattice Statistics ===");
        System.out.println("Grid: " + lattice.gridWidth + "×" + lattice.gridHeight + "×" + lattice.gridDepth +
                           " (" + lattice.totalSites + " sites)");
        System.out.println("Total energy: " + total + " quanta");
        System.out.println("Average energy: " + String.format("%.3f", avg) + " quanta/site");
        System.out.println("Distribution:");
        for (int level = 0; level <= 3; level++) {
            System.out.println("  Level " + level + ": " + counts[level] + " sites (" +
                               String.format("%.1f", 100.0 * counts[level] / lattice.totalSites) + "%)");
        }
    }
}
