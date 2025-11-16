package com.plancklattice;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Utility to probe SIMD capabilities on the current machine.
 * Reports Vector API species information to understand hardware acceleration.
 */
public class VectorProbe {

    public static void main(String[] args) {
        System.out.println("=== Vector API SIMD Capabilities ===\n");

        // Check preferred species (what the JVM thinks is best for this hardware)
        VectorSpecies<Float> preferred = FloatVector.SPECIES_PREFERRED;
        System.out.println("PREFERRED Species:");
        System.out.println("  Name: " + preferred);
        System.out.println("  Lane count: " + preferred.length() + " floats");
        System.out.println("  Vector size: " + preferred.vectorBitSize() + " bits");
        System.out.println("  Bytes per vector: " + (preferred.vectorBitSize() / 8));
        System.out.println();

        // Check all available species
        System.out.println("All available FloatVector species:");

        VectorSpecies<Float> s64 = FloatVector.SPECIES_64;
        System.out.println("  SPECIES_64:  " + s64.length() + " lanes, " + s64.vectorBitSize() + " bits");

        VectorSpecies<Float> s128 = FloatVector.SPECIES_128;
        System.out.println("  SPECIES_128: " + s128.length() + " lanes, " + s128.vectorBitSize() + " bits");

        VectorSpecies<Float> s256 = FloatVector.SPECIES_256;
        System.out.println("  SPECIES_256: " + s256.length() + " lanes, " + s256.vectorBitSize() + " bits");

        VectorSpecies<Float> s512 = FloatVector.SPECIES_512;
        System.out.println("  SPECIES_512: " + s512.length() + " lanes, " + s512.vectorBitSize() + " bits");

        VectorSpecies<Float> sMax = FloatVector.SPECIES_MAX;
        System.out.println("  SPECIES_MAX: " + sMax.length() + " lanes, " + sMax.vectorBitSize() + " bits");

        System.out.println();

        // Practical interpretation
        System.out.println("=== Interpretation ===");
        int laneCount = preferred.length();
        System.out.println("This machine can process " + laneCount + " floats in parallel per operation.");

        if (laneCount >= 16) {
            System.out.println("Hardware: Likely AVX-512 support (512-bit SIMD)");
        } else if (laneCount >= 8) {
            System.out.println("Hardware: Likely AVX/AVX2 support (256-bit SIMD)");
        } else if (laneCount >= 4) {
            System.out.println("Hardware: Likely SSE support (128-bit SIMD)");
        } else {
            System.out.println("Hardware: Limited SIMD support");
        }

        System.out.println();
        System.out.println("For a " + PlanckLattice.EQUILIBRIUM_DISTANCE + " equilibrium lattice with " +
                           (100 * 100) + " spheres:");
        System.out.println("  Vectorized operations will process " + laneCount + " spheres at once");
        System.out.println("  Non-vectorizable tail: ~" + laneCount + " spheres (worst case)");
        System.out.println("  Theoretical speedup: up to " + laneCount + "x for vectorized code");
    }
}
