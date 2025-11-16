package com.plancklattice;

import java.util.concurrent.ForkJoinPool;

/**
 * Benchmark comparing single-core vs multi-core performance.
 * Shows the power of combining SIMD + multi-threading.
 */
public class ParallelBenchmark {

    public static void main(String[] args) {
        System.out.println("=== Single-Core vs Multi-Core Benchmark ===\n");

        // System info
        int availableCores = Runtime.getRuntime().availableProcessors();
        int forkJoinParallelism = ForkJoinPool.commonPool().getParallelism();

        System.out.println("Hardware:");
        System.out.println("  Available processors: " + availableCores);
        System.out.println("  ForkJoinPool parallelism: " + forkJoinParallelism);
        System.out.println("  SIMD lanes: 32 (ByteVector)");
        System.out.println("  Theoretical max parallelism: " + forkJoinParallelism + " × 32 = " +
                          (forkJoinParallelism * 32));
        System.out.println();

        // Test different lattice sizes
        int[] sizes = {50, 100, 150};
        int iterations = 100;

        for (int size : sizes) {
            System.out.println("=== " + size + "³ Lattice (" + (size * size * size) + " sites) ===\n");

            // Create lattice with initial energy
            DiscreteLattice3D lattice = new DiscreteLattice3D(size, size, size);
            int c = size / 2;

            // Add spherical energy distribution
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if (dx*dx + dy*dy + dz*dz <= 9) {
                            lattice.addEnergyQuantum(c + dx, c + dy, c + dz, 3);
                        }
                    }
                }
            }

            int initialEnergy = lattice.getTotalEnergy();

            // Warmup
            for (int i = 0; i < 50; i++) {
                lattice.propagateEnergy();
            }
            lattice.initializeVacuum();
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if (dx*dx + dy*dy + dz*dz <= 9) {
                            lattice.addEnergyQuantum(c + dx, c + dy, c + dz, 3);
                        }
                    }
                }
            }

            // Benchmark single-core
            long startSingle = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                lattice.propagateEnergy();
            }
            long endSingle = System.nanoTime();
            double singleMs = (endSingle - startSingle) / 1_000_000.0;

            int finalEnergySingle = lattice.getTotalEnergy();

            // Reset
            lattice.initializeVacuum();
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if (dx*dx + dy*dy + dz*dz <= 9) {
                            lattice.addEnergyQuantum(c + dx, c + dy, c + dz, 3);
                        }
                    }
                }
            }

            // Warmup parallel
            for (int i = 0; i < 50; i++) {
                lattice.propagateEnergyParallel();
            }
            lattice.initializeVacuum();
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        if (dx*dx + dy*dy + dz*dz <= 9) {
                            lattice.addEnergyQuantum(c + dx, c + dy, c + dz, 3);
                        }
                    }
                }
            }

            // Benchmark multi-core
            long startParallel = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                lattice.propagateEnergyParallel();
            }
            long endParallel = System.nanoTime();
            double parallelMs = (endParallel - startParallel) / 1_000_000.0;

            int finalEnergyParallel = lattice.getTotalEnergy();

            // Results
            int totalSites = size * size * size;
            double singleThroughput = totalSites * iterations / (singleMs / 1000.0);
            double parallelThroughput = totalSites * iterations / (parallelMs / 1000.0);
            double speedup = singleMs / parallelMs;

            System.out.println("Single-Core (32-lane SIMD only):");
            System.out.println("  Time: " + String.format("%.2f", singleMs) + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", singleThroughput) + " sites/sec");
            System.out.println("  Final energy: " + finalEnergySingle + " quanta");
            System.out.println();

            System.out.println("Multi-Core (" + forkJoinParallelism + " cores × 32 lanes):");
            System.out.println("  Time: " + String.format("%.2f", parallelMs) + " ms");
            System.out.println("  Throughput: " + String.format("%.2e", parallelThroughput) + " sites/sec");
            System.out.println("  Final energy: " + finalEnergyParallel + " quanta");
            System.out.println();

            System.out.println("Speedup: " + String.format("%.2f", speedup) + "x");
            double efficiency = (speedup / forkJoinParallelism) * 100;
            System.out.println("Parallel efficiency: " + String.format("%.1f", efficiency) + "%");
            System.out.println();

            if (finalEnergySingle != initialEnergy || finalEnergyParallel != initialEnergy) {
                System.out.println("⚠ Warning: Energy not conserved!");
                System.out.println("  Initial: " + initialEnergy);
                System.out.println("  Single:  " + finalEnergySingle);
                System.out.println("  Parallel: " + finalEnergyParallel);
            } else {
                System.out.println("✓ Energy conserved: " + initialEnergy + " quanta");
            }
            System.out.println("\n" + "=".repeat(60) + "\n");
        }

        System.out.println("Summary:");
        System.out.println("  Single-core: 32-lane SIMD");
        System.out.println("  Multi-core:  " + forkJoinParallelism + " cores × 32 lanes = " +
                          (forkJoinParallelism * 32) + "x theoretical parallelism");
    }
}
