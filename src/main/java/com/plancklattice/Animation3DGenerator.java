package com.plancklattice;

import java.io.File;
import java.io.IOException;

/**
 * Generates animations from 3D discrete lattice simulations.
 * Uses max-projection to visualize 3D spherical wave expansion.
 */
public class Animation3DGenerator {

    public static void main(String[] args) throws IOException {
        System.out.println("=== 3D Discrete Lattice Animation Generator ===\n");

        // Create output directory
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        // Animation parameters
        int latticeSize = 80;  // 80×80×80 = 512K sites
        int totalFrames = 100;
        int stepsPerFrame = 1;

        System.out.println("Configuration:");
        System.out.println("  Lattice size: " + latticeSize + "×" + latticeSize + "×" + latticeSize);
        System.out.println("  Total sites: " + (latticeSize * latticeSize * latticeSize));
        System.out.println("  Total frames: " + totalFrames);
        System.out.println("  Steps per frame: " + stepsPerFrame);
        System.out.println("  Projection: Max intensity along Z-axis");
        System.out.println("  Output: output/frame3d_*.ppm");
        System.out.println();

        // Create 3D lattice
        DiscreteLattice3D lattice = new DiscreteLattice3D(latticeSize, latticeSize, latticeSize);
        DiscreteVisualizer3D viz = new DiscreteVisualizer3D(lattice);

        // Create spherical energy pulse at center
        int cx = latticeSize / 2;
        int cy = latticeSize / 2;
        int cz = latticeSize / 2;

        // Central sphere of energy
        for (int dz = -2; dz <= 2; dz++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    // Spherical distribution
                    if (dx*dx + dy*dy + dz*dz <= 4) {
                        lattice.addEnergyQuantum(cx + dx, cy + dy, cz + dz, 3);
                    }
                }
            }
        }

        // Add corner pulses for interesting dynamics
        int offset = 15;
        lattice.addEnergyQuantum(cx - offset, cy, cz, 3);
        lattice.addEnergyQuantum(cx + offset, cy, cz, 3);
        lattice.addEnergyQuantum(cx, cy - offset, cz, 3);
        lattice.addEnergyQuantum(cx, cy + offset, cz, 3);
        lattice.addEnergyQuantum(cx, cy, cz - offset, 3);
        lattice.addEnergyQuantum(cx, cy, cz + offset, 3);

        System.out.println("Initial energy: " + lattice.getTotalEnergy() + " quanta");
        System.out.println("Generating " + totalFrames + " frames...\n");

        // Generate frames
        for (int frame = 0; frame < totalFrames; frame++) {
            // Save max projection (shows full 3D structure)
            String filename = String.format("output/frame3d_%04d.ppm", frame);
            viz.generateMaxProjectionPPM(filename);

            // Progress
            if (frame % 10 == 0) {
                System.out.println("  Frame " + frame + "/" + totalFrames +
                                 " - Energy: " + lattice.getTotalEnergy() + " quanta");
            }

            // Simulate
            for (int step = 0; step < stepsPerFrame; step++) {
                lattice.propagateEnergy();
            }
        }

        System.out.println("\n✓ Generated " + totalFrames + " frames\n");
        System.out.println("Final energy: " + lattice.getTotalEnergy() + " quanta (conservation check)");
        System.out.println();

        System.out.println("To create animated GIF, run:");
        System.out.println("  ./create_animation.sh \"output/frame3d_%04d.ppm\" quantum3d.gif 15");
        System.out.println();
        System.out.println("The animation shows MAX PROJECTION:");
        System.out.println("  - Each pixel = maximum energy across all Z slices");
        System.out.println("  - Reveals 3D spherical wave expansion");
        System.out.println("  - Brighter = higher energy depth");
    }
}
