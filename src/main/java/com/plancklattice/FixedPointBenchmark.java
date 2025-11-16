package com.plancklattice;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Benchmark comparing 32-bit float (8 lanes) vs 16-bit fixed-point (16 lanes).
 * Tests whether 2x parallelism gain outweighs conversion overhead.
 */
public class FixedPointBenchmark {

    private static final VectorSpecies<Float> FLOAT_SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Short> SHORT_SPECIES = ShortVector.SPECIES_PREFERRED;

    // Fixed-point scaling: 1.0f = 256 (8 bits of fractional precision)
    private static final float SCALE = 256.0f;
    private static final float INV_SCALE = 1.0f / 256.0f;

    public static void main(String[] args) {
        System.out.println("=== Fixed-Point vs Float Benchmark ===\n");
        System.out.println("Testing: Simple vector addition (spheres += forces)");
        System.out.println();

        int arraySize = 10000; // 100x100 lattice
        int iterations = 10000; // Increased for better measurement

        // Warmup
        System.out.println("Warming up JIT compiler...");
        for (int i = 0; i < 500; i++) {
            benchmarkFloat(arraySize, 10);
            benchmarkFixedPoint(arraySize, 10);
        }
        System.out.println("Warmup complete.\n");

        // Benchmark float version (current implementation)
        long floatTime = benchmarkFloat(arraySize, iterations);
        System.out.println("FloatVector (32-bit, 8 lanes):");
        System.out.println("  Time: " + floatTime + " ms");
        System.out.println("  Throughput: " + (arraySize * iterations / (floatTime / 1000.0)) + " ops/sec");
        System.out.println();

        // Benchmark fixed-point version
        long fixedTime = benchmarkFixedPoint(arraySize, iterations);
        System.out.println("ShortVector (16-bit fixed-point, 16 lanes):");
        System.out.println("  Time: " + fixedTime + " ms");
        System.out.println("  Throughput: " + (arraySize * iterations / (fixedTime / 1000.0)) + " ops/sec");
        System.out.println();

        // Analysis
        System.out.println("=== Results ===");
        double speedup = floatTime / (double) fixedTime;
        if (speedup > 1.1) {
            System.out.println("✓ Fixed-point is FASTER: " + String.format("%.2f", speedup) + "x speedup");
            System.out.println("  Recommendation: Consider converting critical code paths");
        } else if (speedup < 0.9) {
            System.out.println("✗ Fixed-point is SLOWER: " + String.format("%.2f", 1.0/speedup) + "x slower");
            System.out.println("  Recommendation: Stick with FloatVector (conversion overhead too high)");
        } else {
            System.out.println("≈ Similar performance (within 10%)");
            System.out.println("  Recommendation: Stick with FloatVector (simpler code, same speed)");
        }
        System.out.println();

        // Accuracy test
        System.out.println("=== Accuracy Test ===");
        testAccuracy();
    }

    private static long benchmarkFloat(int size, int iterations) {
        float[] positions = new float[size];
        float[] forces = new float[size];

        // Initialize
        for (int i = 0; i < size; i++) {
            positions[i] = i * 1.0f;
            forces[i] = 0.01f;
        }

        long start = System.nanoTime();

        for (int iter = 0; iter < iterations; iter++) {
            int upperBound = FLOAT_SPECIES.loopBound(size);

            // Vectorized loop
            for (int i = 0; i < upperBound; i += FLOAT_SPECIES.length()) {
                FloatVector vPos = FloatVector.fromArray(FLOAT_SPECIES, positions, i);
                FloatVector vForce = FloatVector.fromArray(FLOAT_SPECIES, forces, i);
                FloatVector vResult = vPos.add(vForce);
                vResult.intoArray(positions, i);
            }

            // Tail loop
            for (int i = upperBound; i < size; i++) {
                positions[i] += forces[i];
            }
        }

        long end = System.nanoTime();

        // Prevent JIT from optimizing away computation
        if (positions[0] > 1e10) System.out.println("Checksum: " + positions[0]);

        return (end - start) / 1_000_000; // Convert to milliseconds
    }

    private static long benchmarkFixedPoint(int size, int iterations) {
        float[] positions = new float[size];
        float[] forces = new float[size];

        // Initialize
        for (int i = 0; i < size; i++) {
            positions[i] = i * 1.0f;
            forces[i] = 0.01f;
        }

        long start = System.nanoTime();

        for (int iter = 0; iter < iterations; iter++) {
            int upperBound = SHORT_SPECIES.loopBound(size);

            // Vectorized loop with fixed-point
            for (int i = 0; i < upperBound; i += SHORT_SPECIES.length()) {
                // Convert float → short (fixed-point encode)
                short[] posShort = new short[SHORT_SPECIES.length()];
                short[] forceShort = new short[SHORT_SPECIES.length()];

                for (int j = 0; j < SHORT_SPECIES.length(); j++) {
                    posShort[j] = (short)(positions[i + j] * SCALE);
                    forceShort[j] = (short)(forces[i + j] * SCALE);
                }

                // Vectorized addition
                ShortVector vPos = ShortVector.fromArray(SHORT_SPECIES, posShort, 0);
                ShortVector vForce = ShortVector.fromArray(SHORT_SPECIES, forceShort, 0);
                ShortVector vResult = vPos.add(vForce);
                vResult.intoArray(posShort, 0);

                // Convert short → float (fixed-point decode)
                for (int j = 0; j < SHORT_SPECIES.length(); j++) {
                    positions[i + j] = posShort[j] * INV_SCALE;
                }
            }

            // Tail loop
            for (int i = upperBound; i < size; i++) {
                positions[i] += forces[i];
            }
        }

        long end = System.nanoTime();

        // Prevent JIT from optimizing away computation
        if (positions[0] > 1e10) System.out.println("Checksum: " + positions[0]);

        return (end - start) / 1_000_000;
    }

    private static void testAccuracy() {
        float[] testValues = {0.0f, 1.0f, 10.5f, 100.123f, -50.7f};

        System.out.println("Testing fixed-point precision:");
        for (float val : testValues) {
            short encoded = (short)(val * SCALE);
            float decoded = encoded * INV_SCALE;
            float error = Math.abs(val - decoded);
            System.out.println(String.format("  %.3f → %d → %.3f (error: %.6f)",
                    val, encoded, decoded, error));
        }
    }
}
