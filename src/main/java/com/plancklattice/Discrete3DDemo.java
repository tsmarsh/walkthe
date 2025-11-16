package com.plancklattice;

import java.io.IOException;

/**
 * Demo of 3D discrete quantum lattice.
 * Shows emergent wave behavior in 3D space.
 */
public class Discrete3DDemo {

    public static void main(String[] args) throws IOException {
        System.out.println("=== 3D Discrete Quantum Lattice Demo ===\n");
        DiscreteLattice3D.printSIMDInfo();
        System.out.println();

        // Create modest 3D lattice (20×20×20 = 8000 sites)
        System.out.println("Creating 20×20×20 lattice...\n");
        DiscreteLattice3D lattice = new DiscreteLattice3D(20, 20, 20);
        DiscreteVisualizer3D viz = new DiscreteVisualizer3D(lattice);

        // Add energy pulse at center
        int cx = lattice.gridWidth / 2;
        int cy = lattice.gridHeight / 2;
        int cz = lattice.gridDepth / 2;

        System.out.println("Adding energy quantum at center (" + cx + "," + cy + "," + cz + ")");
        lattice.addEnergyQuantum(cx, cy, cz, 3);
        System.out.println();

        // Show initial state
        System.out.println("Initial state:");
        viz.printCenterSliceASCII();
        viz.printStats();
        System.out.println();

        // Propagate and show evolution
        for (int step = 1; step <= 10; step++) {
            lattice.propagateEnergy();

            if (step % 2 == 0) {
                System.out.println("After " + step + " steps:");
                viz.printCenterSliceASCII();
                System.out.println("Total energy: " + lattice.getTotalEnergy() + " quanta\n");
            }
        }

        viz.printStats();

        // Performance test
        System.out.println("\n=== Performance Test ===\n");
        testPerformance();
    }

    private static void testPerformance() {
        int[] sizes = {50, 100};
        int iterations = 100;

        for (int size : sizes) {
            DiscreteLattice3D lattice = new DiscreteLattice3D(size, size, size);

            // Add initial energy
            int c = size / 2;
            lattice.addEnergyQuantum(c, c, c, 3);
            lattice.addEnergyQuantum(c - 10, c, c, 3);
            lattice.addEnergyQuantum(c + 10, c, c, 3);

            // Warmup
            for (int i = 0; i < 50; i++) {
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
            int totalSites = size * size * size;
            double sitesPerSec = totalSites * iterations / (timeMs / 1000.0);

            System.out.println(size + "³ lattice (" + totalSites + " sites):");
            System.out.println("  Total time: " + String.format("%.2f", timeMs) + " ms for " + iterations + " iterations");
            System.out.println("  Per iteration: " + String.format("%.3f", avgMs) + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", sitesPerSec) + " sites/sec");
            System.out.println();
        }

        System.out.println("Note: 3D with 6 neighbors vs 2D with 4 neighbors");
        System.out.println("      32-lane ByteVector on single core");
    }
}
