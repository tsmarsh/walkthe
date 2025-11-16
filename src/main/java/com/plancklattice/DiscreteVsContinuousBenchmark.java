package com.plancklattice;

/**
 * Comprehensive benchmark comparing discrete vs continuous implementations.
 *
 * Discrete (new):
 * - 2-bit quantum states (0-3)
 * - ByteVector, 32 lanes
 * - Cellular automaton rules
 * - Native discrete operations
 *
 * Continuous (original):
 * - 32-bit floats
 * - FloatVector, 8 lanes
 * - Differential equations
 * - Hooke's law, etc.
 */
public class DiscreteVsContinuousBenchmark {

    public static void main(String[] args) {
        System.out.println("=== Discrete vs Continuous Implementation Benchmark ===\n");

        System.out.println("DISCRETE (Quantum State) Approach:");
        System.out.println("  - Data: 2-bit quantum states (0-3 discrete levels)");
        System.out.println("  - SIMD: ByteVector, 32 lanes");
        System.out.println("  - Physics: Cellular automaton rules");
        System.out.println("  - Memory: 1 byte per state");
        System.out.println();

        System.out.println("CONTINUOUS (Traditional) Approach:");
        System.out.println("  - Data: 32-bit floats (continuous values)");
        System.out.println("  - SIMD: FloatVector, 8 lanes");
        System.out.println("  - Physics: Differential equations (Hooke's law, etc.)");
        System.out.println("  - Memory: 4 bytes per value");
        System.out.println();

        // Benchmark configurations
        int[] sizes = {100, 500, 1000};
        int iterations = 1000;

        System.out.println("=== Performance Comparison ===\n");

        for (int size : sizes) {
            System.out.println("--- " + size + "×" + size + " Lattice (" + (size * size) + " sites) ---\n");

            // Benchmark discrete
            long discreteTime = benchmarkDiscrete(size, iterations);

            // Benchmark continuous
            long continuousTime = benchmarkContinuous(size, iterations);

            // Analysis
            double speedup = continuousTime / (double) discreteTime;
            int totalSites = size * size;

            System.out.println("Discrete lattice:");
            System.out.println("  Time: " + discreteTime + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", totalSites * iterations / (discreteTime / 1000.0)) + " sites/sec");
            System.out.println();

            System.out.println("Continuous lattice:");
            System.out.println("  Time: " + continuousTime + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", totalSites * iterations / (continuousTime / 1000.0)) + " sites/sec");
            System.out.println();

            System.out.println("Speedup: " + String.format("%.2f", speedup) + "x");
            if (speedup > 1.5) {
                System.out.println("✓ Discrete is FASTER!");
            } else if (speedup < 0.67) {
                System.out.println("✗ Discrete is slower");
            } else {
                System.out.println("≈ Similar performance");
            }
            System.out.println();

            // Memory comparison
            int discreteMem = totalSites * 2; // 2 bytes per site (energy + EM)
            int continuousMem = totalSites * 8; // 8 bytes (2 floats: energy + forces)
            System.out.println("Memory usage:");
            System.out.println("  Discrete: " + (discreteMem / 1024) + " KB");
            System.out.println("  Continuous: " + (continuousMem / 1024) + " KB");
            System.out.println("  Savings: " + String.format("%.1f", 100.0 * (1 - discreteMem / (double) continuousMem)) + "%");
            System.out.println();
            System.out.println("----------------------------------------\n");
        }

        // Summary
        System.out.println("=== Summary ===\n");
        System.out.println("Discrete Quantum State Advantages:");
        System.out.println("  ✓ 4x more SIMD lanes (32 vs 8)");
        System.out.println("  ✓ 4x less memory (1 byte vs 4 bytes)");
        System.out.println("  ✓ Philosophically aligned with quantum physics");
        System.out.println("  ✓ No floating-point precision issues");
        System.out.println("  ✓ Simpler cellular automaton rules");
        System.out.println();

        System.out.println("Continuous Float Advantages:");
        System.out.println("  ✓ Continuous range of values");
        System.out.println("  ✓ Standard physics equations");
        System.out.println("  ✓ Well-understood mathematical framework");
        System.out.println();

        System.out.println("Recommendation:");
        System.out.println("  For Planck-scale quantum simulations:");
        System.out.println("  → Use DISCRETE approach (more physically accurate)");
        System.out.println("  For classical physics simulations:");
        System.out.println("  → Use CONTINUOUS approach (standard differential equations)");
    }

    private static long benchmarkDiscrete(int size, int iterations) {
        DiscreteLattice lattice = new DiscreteLattice(size, size);

        // Add initial energy
        int cx = size / 2;
        int cy = size / 2;
        lattice.addEnergyQuantum(cx, cy, 3);
        lattice.addEnergyQuantum(cx + 10, cy, 2);

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

        return (end - start) / 1_000_000; // Convert to ms
    }

    private static long benchmarkContinuous(int size, int iterations) {
        PlanckLattice lattice = new PlanckLattice(size, size);
        VectorForces forces = new VectorForces(lattice);

        // Add initial energy (comparable to discrete)
        int cx = size / 2;
        int cy = size / 2;
        lattice.energyDensity[lattice.getIndex(cx, cy)] = 3.0f;
        lattice.energyDensity[lattice.getIndex(cx + 10, cy)] = 2.0f;

        // Warmup
        for (int i = 0; i < 100; i++) {
            forces.calculateSpacingForces();
            forces.calculateGravityForces();
        }

        // Benchmark
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            forces.calculateSpacingForces();
            forces.calculateGravityForces();
        }
        long end = System.nanoTime();

        return (end - start) / 1_000_000;
    }
}
