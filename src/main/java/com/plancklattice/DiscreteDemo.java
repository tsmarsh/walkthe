package com.plancklattice;

import java.io.IOException;

/**
 * Demonstration of discrete quantum lattice simulation.
 * Shows energy propagation and EM field behavior using cellular automaton rules.
 */
public class DiscreteDemo {

    public static void main(String[] args) throws IOException {
        // Print SIMD info
        System.out.println("=== Discrete Quantum Lattice Demo ===\n");
        DiscreteLattice.printSIMDInfo();
        System.out.println();

        // Create small lattice for visualization
        System.out.println("Creating 20×20 discrete lattice...\n");
        DiscreteLattice lattice = new DiscreteLattice(20, 20);
        DiscreteVisualizer viz = new DiscreteVisualizer(lattice);

        // Demo 1: Energy propagation
        System.out.println("=== Demo 1: Energy Propagation ===\n");
        demonstrateEnergyPropagation(lattice, viz);

        // Reset
        lattice.initializeVacuum();
        System.out.println();

        // Demo 2: EM wave
        System.out.println("=== Demo 2: EM Wave Propagation ===\n");
        demonstrateEMWave(lattice, viz);

        // Demo 3: Large lattice performance
        System.out.println("\n=== Demo 3: Performance Test ===\n");
        demonstratePerformance();
    }

    private static void demonstrateEnergyPropagation(DiscreteLattice lattice, DiscreteVisualizer viz) {
        // Add energy pulse at center
        int cx = lattice.gridWidth / 2;
        int cy = lattice.gridHeight / 2;

        System.out.println("Adding 3 energy quanta at center (" + cx + ", " + cy + ")...\n");
        lattice.addEnergyQuantum(cx, cy, 3);

        System.out.println("Initial state:");
        viz.printEnergyASCII();
        System.out.println();

        // Propagate for several steps
        for (int step = 1; step <= 5; step++) {
            lattice.propagateEnergy();
            System.out.println("After " + step + " propagation step(s):");
            viz.printEnergyASCII();
            System.out.println();
        }

        viz.printStats();
    }

    private static void demonstrateEMWave(DiscreteLattice lattice, DiscreteVisualizer viz) {
        // Create EM pulse at center
        int cx = lattice.gridWidth / 2;
        int cy = lattice.gridHeight / 2;

        System.out.println("Creating EM pulse at center (" + cx + ", " + cy + ")...\n");
        lattice.createEMPulse(cx, cy, 3);

        System.out.println("Initial EM field:");
        viz.printEMASCII();
        System.out.println();

        // Propagate EM wave
        for (int step = 1; step <= 5; step++) {
            lattice.propagateEM();
            System.out.println("After " + step + " EM propagation step(s):");
            viz.printEMASCII();
            System.out.println();
        }
    }

    private static void demonstratePerformance() {
        int[] sizes = {100, 500, 1000};
        int iterations = 1000;

        System.out.println("Testing performance of discrete lattice (ByteVector, 32 lanes):\n");

        for (int size : sizes) {
            DiscreteLattice lattice = new DiscreteLattice(size, size);

            // Add some initial energy
            for (int i = 0; i < 10; i++) {
                int x = (int) (Math.random() * size);
                int y = (int) (Math.random() * size);
                lattice.addEnergyQuantum(x, y, 3);
            }

            // Warmup
            for (int i = 0; i < 100; i++) {
                lattice.propagateEnergy();
            }

            // Benchmark
            long start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                lattice.propagateEnergy();
            }
            long end = System.nanoTime();

            double timeMs = (end - start) / 1_000_000.0;
            double avgMs = timeMs / iterations;
            int totalSites = size * size;
            double sitesPerSec = totalSites * iterations / (timeMs / 1000.0);

            System.out.println(size + "×" + size + " lattice (" + totalSites + " sites):");
            System.out.println("  Total time: " + String.format("%.2f", timeMs) + " ms for " + iterations + " iterations");
            System.out.println("  Per iteration: " + String.format("%.3f", avgMs) + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", sitesPerSec) + " sites/sec");
            System.out.println();
        }

        System.out.println("Note: This uses 32-lane ByteVector operations (4x wider than FloatVector)");
    }
}
