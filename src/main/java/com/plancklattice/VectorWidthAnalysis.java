package com.plancklattice;

import jdk.incubator.vector.*;

/**
 * Analysis of different vector widths and precision trade-offs.
 * Explores whether using smaller data types improves parallelism.
 */
public class VectorWidthAnalysis {

    public static void main(String[] args) {
        System.out.println("=== Vector Width vs Precision Trade-offs ===\n");

        // Float (current implementation)
        VectorSpecies<Float> floatSpecies = FloatVector.SPECIES_PREFERRED;
        System.out.println("FloatVector (32-bit, current):");
        System.out.println("  Lanes: " + floatSpecies.length());
        System.out.println("  Bits per lane: 32");
        System.out.println("  Total width: " + floatSpecies.vectorBitSize() + " bits");
        System.out.println("  Precision: ~7 decimal digits");
        System.out.println("  Range: ±3.4e38");
        System.out.println();

        // Short (16-bit integer - potential for fixed-point)
        VectorSpecies<Short> shortSpecies = ShortVector.SPECIES_PREFERRED;
        System.out.println("ShortVector (16-bit integer, fixed-point option):");
        System.out.println("  Lanes: " + shortSpecies.length());
        System.out.println("  Bits per lane: 16");
        System.out.println("  Total width: " + shortSpecies.vectorBitSize() + " bits");
        System.out.println("  Parallelism gain: " + (shortSpecies.length() / (float)floatSpecies.length()) + "x");
        System.out.println("  Precision: ~4-5 decimal digits (fixed-point)");
        System.out.println("  Range: Limited by scaling factor");
        System.out.println();

        // Byte (8-bit integer - extreme parallelism)
        VectorSpecies<Byte> byteSpecies = ByteVector.SPECIES_PREFERRED;
        System.out.println("ByteVector (8-bit integer, extreme parallelism):");
        System.out.println("  Lanes: " + byteSpecies.length());
        System.out.println("  Bits per lane: 8");
        System.out.println("  Total width: " + byteSpecies.vectorBitSize() + " bits");
        System.out.println("  Parallelism gain: " + (byteSpecies.length() / (float)floatSpecies.length()) + "x");
        System.out.println("  Precision: ~2 decimal digits (fixed-point)");
        System.out.println("  Range: Very limited");
        System.out.println();

        // Double (64-bit, higher precision but less parallelism)
        VectorSpecies<Double> doubleSpecies = DoubleVector.SPECIES_PREFERRED;
        System.out.println("DoubleVector (64-bit, higher precision):");
        System.out.println("  Lanes: " + doubleSpecies.length());
        System.out.println("  Bits per lane: 64");
        System.out.println("  Total width: " + doubleSpecies.vectorBitSize() + " bits");
        System.out.println("  Parallelism change: " + (doubleSpecies.length() / (float)floatSpecies.length()) + "x");
        System.out.println("  Precision: ~15 decimal digits");
        System.out.println("  Range: ±1.7e308");
        System.out.println();

        // Analysis
        System.out.println("=== Analysis ===\n");

        System.out.println("OPTION 1: Use ShortVector (16-bit fixed-point)");
        System.out.println("  ✓ Pros: " + shortSpecies.length() + " lanes (" +
                          (shortSpecies.length() / floatSpecies.length()) + "x more parallelism)");
        System.out.println("  ✗ Cons: Requires fixed-point arithmetic (complexity)");
        System.out.println("         Reduced precision (may affect physics accuracy)");
        System.out.println("         Conversion overhead (float ↔ short)");
        System.out.println();

        System.out.println("OPTION 2: Stay with FloatVector (current)");
        System.out.println("  ✓ Pros: Native floating-point, good precision");
        System.out.println("         Simple code, no conversions needed");
        System.out.println("  ✗ Cons: Only " + floatSpecies.length() + " lanes");
        System.out.println();

        System.out.println("OPTION 3: Use DoubleVector (higher precision)");
        System.out.println("  ✓ Pros: Maximum precision");
        System.out.println("  ✗ Cons: Only " + doubleSpecies.length() + " lanes (worse parallelism)");
        System.out.println("         2x memory usage");
        System.out.println();

        System.out.println("=== Recommendation ===\n");
        System.out.println("For Planck Lattice simulation:");
        System.out.println("  - Physics calculations need good precision");
        System.out.println("  - Fixed-point adds complexity and potential bugs");
        System.out.println("  - Conversion overhead may negate parallelism gains");
        System.out.println("  - FloatVector (32-bit) is the sweet spot");
        System.out.println();
        System.out.println("HOWEVER: We could benchmark to verify!");
    }
}
