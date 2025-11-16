package com.plancklattice;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Visualization for discrete quantum lattice.
 * Renders quantum states as grayscale images and ASCII art.
 */
public class DiscreteVisualizer {

    private final DiscreteLattice lattice;

    public DiscreteVisualizer(DiscreteLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Generate PPM image of energy levels.
     * 0 = black, 1 = dark gray, 2 = light gray, 3 = white
     */
    public void generateEnergyPPM(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // PPM header
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            // Pixel data
            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    int energy = lattice.getEnergy(x, y);

                    // Map 0-3 to 0-255
                    int gray = (energy * 255) / 3;

                    // RGB (grayscale)
                    writer.println(gray + " " + gray + " " + gray);
                }
            }
        }
    }

    /**
     * Generate PPM image of EM field.
     * Use blue color gradient for EM field.
     */
    public void generateEMPPM(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    int em = lattice.getEM(x, y);

                    // Map to blue gradient
                    int blue = (em * 255) / 3;

                    writer.println("0 0 " + blue);
                }
            }
        }
    }

    /**
     * Generate combined visualization (energy = red, EM = blue).
     */
    public void generateCombinedPPM(String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(lattice.gridWidth + " " + lattice.gridHeight);
            writer.println("255");

            for (int y = 0; y < lattice.gridHeight; y++) {
                for (int x = 0; x < lattice.gridWidth; x++) {
                    int energy = lattice.getEnergy(x, y);
                    int em = lattice.getEM(x, y);

                    // Energy = red, EM = blue
                    int red = (energy * 255) / 3;
                    int blue = (em * 255) / 3;

                    writer.println(red + " 0 " + blue);
                }
            }
        }
    }

    /**
     * Print ASCII visualization to console.
     * Uses characters to represent quantum levels: . ░ ▒ ▓
     */
    public void printEnergyASCII() {
        System.out.println("Energy Distribution (. = vacuum, ░ = 1q, ▒ = 2q, ▓ = 3q):");
        System.out.println("┌" + "─".repeat(lattice.gridWidth) + "┐");

        for (int y = 0; y < lattice.gridHeight; y++) {
            System.out.print("│");
            for (int x = 0; x < lattice.gridWidth; x++) {
                int energy = lattice.getEnergy(x, y);
                char symbol = switch (energy) {
                    case 0 -> '·';  // Vacuum
                    case 1 -> '░';  // 1 quantum
                    case 2 -> '▒';  // 2 quanta
                    case 3 -> '▓';  // 3 quanta
                    default -> '?';
                };
                System.out.print(symbol);
            }
            System.out.println("│");
        }

        System.out.println("└" + "─".repeat(lattice.gridWidth) + "┘");
        System.out.println("Total energy: " + lattice.getTotalEnergy() + " quanta");
        System.out.println("Average: " + String.format("%.2f", lattice.getAverageEnergy()) + " quanta/site");
    }

    /**
     * Print EM field as ASCII.
     */
    public void printEMASCII() {
        System.out.println("EM Field (. = none, ∼ = weak, ≈ = medium, ≋ = strong):");
        System.out.println("┌" + "─".repeat(lattice.gridWidth) + "┐");

        for (int y = 0; y < lattice.gridHeight; y++) {
            System.out.print("│");
            for (int x = 0; x < lattice.gridWidth; x++) {
                int em = lattice.getEM(x, y);
                char symbol = switch (em) {
                    case 0 -> '·';  // No field
                    case 1 -> '∼';  // Weak
                    case 2 -> '≈';  // Medium
                    case 3 -> '≋';  // Strong
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

        // Count sites at each level
        int[] counts = new int[4];
        for (int i = 0; i < lattice.totalSpheres; i++) {
            counts[lattice.energyLevel[i]]++;
        }

        System.out.println("=== Lattice Statistics ===");
        System.out.println("Grid: " + lattice.gridWidth + "×" + lattice.gridHeight +
                           " (" + lattice.totalSpheres + " sites)");
        System.out.println("Total energy: " + total + " quanta");
        System.out.println("Average energy: " + String.format("%.3f", avg) + " quanta/site");
        System.out.println("Distribution:");
        System.out.println("  Level 0 (vacuum): " + counts[0] + " sites (" +
                           String.format("%.1f", 100.0 * counts[0] / lattice.totalSpheres) + "%)");
        System.out.println("  Level 1: " + counts[1] + " sites (" +
                           String.format("%.1f", 100.0 * counts[1] / lattice.totalSpheres) + "%)");
        System.out.println("  Level 2: " + counts[2] + " sites (" +
                           String.format("%.1f", 100.0 * counts[2] / lattice.totalSpheres) + "%)");
        System.out.println("  Level 3: " + counts[3] + " sites (" +
                           String.format("%.1f", 100.0 * counts[3] / lattice.totalSpheres) + "%)");
    }
}
