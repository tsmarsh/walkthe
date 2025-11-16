package com.plancklattice;

import java.io.File;
import java.io.IOException;

/**
 * Generates animation sequences from discrete lattice simulations.
 * Creates a series of PPM images that can be converted to GIF using ffmpeg.
 *
 * Usage:
 *   1. Run this program to generate PPM frames
 *   2. Run: ./create_animation.sh "output/frame_%04d.ppm" simulation.gif 10
 */
public class AnimationGenerator {

    public static void main(String[] args) throws IOException {
        System.out.println("=== Discrete Lattice Animation Generator ===\n");

        // Create output directory
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
            System.out.println("Created output directory: output/");
        }

        // Animation parameters
        int latticeSize = 100;
        int totalFrames = 100;
        int stepsPerFrame = 2;

        System.out.println("Configuration:");
        System.out.println("  Lattice size: " + latticeSize + "×" + latticeSize);
        System.out.println("  Total frames: " + totalFrames);
        System.out.println("  Steps per frame: " + stepsPerFrame);
        System.out.println("  Output: output/frame_*.ppm");
        System.out.println();

        // Create lattice
        DiscreteLattice lattice = new DiscreteLattice(latticeSize, latticeSize);
        DiscreteVisualizer viz = new DiscreteVisualizer(lattice);

        // Create multiple energy pulses for interesting dynamics
        int cx = latticeSize / 2;
        int cy = latticeSize / 2;

        // Central burst
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dy) <= 2) {
                    lattice.addEnergyQuantum(cx + dx, cy + dy, 3);
                }
            }
        }

        // Four corner pulses
        lattice.addEnergyQuantum(20, 20, 3);
        lattice.addEnergyQuantum(80, 20, 3);
        lattice.addEnergyQuantum(20, 80, 3);
        lattice.addEnergyQuantum(80, 80, 3);

        System.out.println("Generating " + totalFrames + " frames...");

        // Generate frames
        for (int frame = 0; frame < totalFrames; frame++) {
            // Save current state
            String filename = String.format("output/frame_%04d.ppm", frame);
            viz.generateCombinedPPM(filename);

            // Progress indicator
            if (frame % 10 == 0) {
                System.out.println("  Frame " + frame + "/" + totalFrames + " - Total energy: " +
                                 lattice.getTotalEnergy() + " quanta");
            }

            // Simulate
            for (int step = 0; step < stepsPerFrame; step++) {
                lattice.propagateEnergy();
            }
        }

        System.out.println("\n✓ Generated " + totalFrames + " frames in output/\n");

        System.out.println("To create animated GIF, run:");
        System.out.println("  ./create_animation.sh \"output/frame_%04d.ppm\" energy_propagation.gif 10");
        System.out.println();

        System.out.println("For different frame rates:");
        System.out.println("  5 fps  (slow):  ./create_animation.sh \"output/frame_%04d.ppm\" slow.gif 5");
        System.out.println("  10 fps (medium): ./create_animation.sh \"output/frame_%04d.ppm\" medium.gif 10");
        System.out.println("  20 fps (fast):  ./create_animation.sh \"output/frame_%04d.ppm\" fast.gif 20");
    }

    /**
     * Generate EM wave propagation animation.
     */
    public static void generateEMAnimation() throws IOException {
        System.out.println("=== EM Wave Animation Generator ===\n");

        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        int latticeSize = 100;
        int totalFrames = 50;

        DiscreteLattice lattice = new DiscreteLattice(latticeSize, latticeSize);
        DiscreteVisualizer viz = new DiscreteVisualizer(lattice);

        // Create EM pulse at center
        int cx = latticeSize / 2;
        int cy = latticeSize / 2;
        lattice.createEMPulse(cx, cy, 3);

        System.out.println("Generating EM wave animation...");

        for (int frame = 0; frame < totalFrames; frame++) {
            String filename = String.format("output/em_%04d.ppm", frame);
            viz.generateEMPPM(filename);

            if (frame % 10 == 0) {
                System.out.println("  Frame " + frame + "/" + totalFrames);
            }

            lattice.propagateEM();
        }

        System.out.println("\n✓ Generated " + totalFrames + " EM frames\n");
        System.out.println("To create GIF:");
        System.out.println("  ./create_animation.sh \"output/em_%04d.ppm\" em_wave.gif 15");
    }

    /**
     * Generate multi-pulse interference animation.
     */
    public static void generateInterferenceAnimation() throws IOException {
        System.out.println("=== Quantum Interference Animation ===\n");

        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        int latticeSize = 150;
        int totalFrames = 150;

        DiscreteLattice lattice = new DiscreteLattice(latticeSize, latticeSize);
        DiscreteVisualizer viz = new DiscreteVisualizer(lattice);

        // Create multiple energy sources
        lattice.addEnergyQuantum(50, 75, 3);
        lattice.addEnergyQuantum(100, 75, 3);

        System.out.println("Generating interference pattern...");

        for (int frame = 0; frame < totalFrames; frame++) {
            String filename = String.format("output/interference_%04d.ppm", frame);
            viz.generateCombinedPPM(filename);

            if (frame % 20 == 0) {
                System.out.println("  Frame " + frame + "/" + totalFrames);
            }

            lattice.propagateEnergy();
            lattice.propagateEM();

            // Re-energize sources periodically
            if (frame % 10 == 0) {
                lattice.addEnergyQuantum(50, 75, 2);
                lattice.addEnergyQuantum(100, 75, 2);
            }
        }

        System.out.println("\n✓ Generated interference animation\n");
        System.out.println("To create GIF:");
        System.out.println("  ./create_animation.sh \"output/interference_%04d.ppm\" interference.gif 15");
    }
}
