package com.plancklattice;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

/**
 * Discrete quantum lattice using native 2-bit quantum states.
 *
 * Philosophy: The universe at Planck scale is fundamentally discrete.
 * Instead of approximating discrete physics with continuous floats,
 * we use native discrete states and cellular automaton rules.
 *
 * Advantages:
 * - 4x more parallelism (32 lanes vs 8 with FloatVector)
 * - No floating-point precision errors
 * - Computationally simpler
 * - More physically accurate for Planck-scale physics
 *
 * Quantum States (2 bits each, 0-3):
 * - Energy Level: Number of energy quanta at this site
 * - EM Field: Electromagnetic field strength
 *
 * Rules: Cellular automaton instead of differential equations
 */
public class DiscreteLattice {

    // Quantum state levels (2 bits = 4 levels)
    public static final byte LEVEL_0 = 0;  // Vacuum / No field
    public static final byte LEVEL_1 = 1;  // 1 quantum
    public static final byte LEVEL_2 = 2;  // 2 quanta
    public static final byte LEVEL_3 = 3;  // 3 quanta (maximum)

    // Vector species for SIMD operations
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    // Lattice dimensions
    public final int gridWidth;
    public final int gridHeight;
    public final int totalSpheres;

    // Discrete quantum states (0-3 for each)
    public final byte[] energyLevel;    // Energy quanta at each site
    public final byte[] emField;        // EM field strength
    public final byte[] nextEnergy;     // Double buffer for updates
    public final byte[] nextEM;         // Double buffer for EM

    // Configuration
    private final boolean toroidal;

    public DiscreteLattice(int width, int height) {
        this(width, height, true);
    }

    public DiscreteLattice(int width, int height, boolean toroidal) {
        this.gridWidth = width;
        this.gridHeight = height;
        this.totalSpheres = width * height;
        this.toroidal = toroidal;

        // Allocate state arrays
        this.energyLevel = new byte[totalSpheres];
        this.emField = new byte[totalSpheres];
        this.nextEnergy = new byte[totalSpheres];
        this.nextEM = new byte[totalSpheres];

        // Initialize to vacuum
        initializeVacuum();
    }

    /**
     * Initialize lattice to vacuum state (all zeros).
     * Vectorized initialization.
     */
    public void initializeVacuum() {
        int upperBound = SPECIES.loopBound(totalSpheres);

        ByteVector vZero = ByteVector.zero(SPECIES);

        // Vectorized zero initialization (32 spheres at a time!)
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            vZero.intoArray(energyLevel, i);
            vZero.intoArray(emField, i);
        }

        // Tail elements
        for (int i = upperBound; i < totalSpheres; i++) {
            energyLevel[i] = LEVEL_0;
            emField[i] = LEVEL_0;
        }
    }

    /**
     * Add energy quantum at a specific location.
     * Saturates at LEVEL_3 (3 quanta maximum).
     */
    public void addEnergyQuantum(int x, int y, int quanta) {
        int index = getIndex(x, y);
        int newLevel = energyLevel[index] + quanta;
        energyLevel[index] = (byte) Math.min(newLevel, LEVEL_3);
    }

    /**
     * Create EM pulse at a location.
     */
    public void createEMPulse(int x, int y, int strength) {
        int index = getIndex(x, y);
        emField[index] = (byte) Math.min(strength, LEVEL_3);
    }

    /**
     * FUNDAMENTAL QUANTUM RULES (not wave equations!)
     *
     * Rule: Energy quanta flow from high-potential to low-potential sites
     * - If neighbor has lower energy, transfer 1 quantum
     * - Process all 4 directions each step
     * - Waves/diffusion should EMERGE from this, not be modeled directly
     *
     * Like molecular dynamics: simple local interactions → emergent waves
     */
    public void propagateEnergy() {
        // Copy to buffer
        System.arraycopy(energyLevel, 0, nextEnergy, 0, totalSpheres);

        // Process each site
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int idx = getIndex(x, y);
                int energy = energyLevel[idx];

                if (energy == 0) continue; // No quanta to transfer

                // Get neighbors
                int[] neighbors = new int[4];
                neighbors[0] = getNeighborIndex(x + 1, y, gridWidth, gridHeight);
                neighbors[1] = getNeighborIndex(x - 1, y, gridWidth, gridHeight);
                neighbors[2] = getNeighborIndex(x, y + 1, gridWidth, gridHeight);
                neighbors[3] = getNeighborIndex(x, y - 1, gridWidth, gridHeight);

                // Count neighbors with LOWER energy (gradient)
                int lowerCount = 0;
                for (int n : neighbors) {
                    if (n != -1 && energyLevel[n] < energy) {
                        lowerCount++;
                    }
                }

                // If we have lower neighbors, transfer energy
                // Allow even level-1 to propagate (otherwise system freezes at equilibrium)
                if (lowerCount > 0 && energy > 0) {
                    // Transfer 1 quantum to a lower neighbor
                    // Choose based on position (deterministic, not random)
                    int choice = (idx + stepCount) % lowerCount;
                    int transferred = 0;

                    for (int n : neighbors) {
                        if (n != -1 && energyLevel[n] < energy) {
                            if (transferred == choice && nextEnergy[n] < LEVEL_3) {
                                nextEnergy[idx]--;
                                nextEnergy[n]++;
                                break;
                            }
                            transferred++;
                        }
                    }
                }
            }
        }

        stepCount++;
        System.arraycopy(nextEnergy, 0, energyLevel, 0, totalSpheres);
    }

    private int stepCount = 0; // For deterministic selection

    /**
     * Propagate EM field using wave equation (discrete).
     *
     * Rule: EM field propagates outward from sources
     * - Each site spreads field to neighbors at reduced strength
     * - Field decays over time
     */
    public void propagateEM() {
        // Vectorized decay first (all fields lose 1 level)
        int upperBound = SPECIES.loopBound(totalSpheres);

        ByteVector vOne = ByteVector.broadcast(SPECIES, (byte) 1);
        ByteVector vZero = ByteVector.zero(SPECIES);

        // Decay: field = max(0, field - 1)
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            ByteVector vField = ByteVector.fromArray(SPECIES, emField, i);
            ByteVector vDecayed = vField.sub(vOne).max(vZero);
            vDecayed.intoArray(nextEM, i);
        }

        // Tail elements
        for (int i = upperBound; i < totalSpheres; i++) {
            nextEM[i] = (byte) Math.max(0, emField[i] - 1);
        }

        // Propagate to neighbors
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int idx = getIndex(x, y);
                int field = emField[idx];

                if (field <= LEVEL_1) continue; // Too weak to propagate

                // Get neighbors
                int right = getNeighborIndex(x + 1, y, gridWidth, gridHeight);
                int left = getNeighborIndex(x - 1, y, gridWidth, gridHeight);
                int down = getNeighborIndex(x, y + 1, gridWidth, gridHeight);
                int up = getNeighborIndex(x, y - 1, gridWidth, gridHeight);

                // Spread reduced field to neighbors
                int spreadField = field - 1; // Reduce by 1 when spreading

                if (right != -1) nextEM[right] = (byte) Math.max(nextEM[right], spreadField);
                if (left != -1) nextEM[left] = (byte) Math.max(nextEM[left], spreadField);
                if (down != -1) nextEM[down] = (byte) Math.max(nextEM[down], spreadField);
                if (up != -1) nextEM[up] = (byte) Math.max(nextEM[up], spreadField);
            }
        }

        // Swap buffers
        System.arraycopy(nextEM, 0, emField, 0, totalSpheres);
    }

    /**
     * Get total energy in the lattice (sum of all quanta).
     * Vectorized reduction with proper overflow handling.
     */
    public int getTotalEnergy() {
        int upperBound = SPECIES.loopBound(totalSpheres);

        int totalSum = 0;

        // Vectorized sum - reduce each vector chunk to avoid byte overflow
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            ByteVector vEnergy = ByteVector.fromArray(SPECIES, energyLevel, i);
            // Reduce this chunk to int to avoid overflow
            int chunkSum = vEnergy.reduceLanes(VectorOperators.ADD);
            totalSum += chunkSum;
        }

        // Add tail
        for (int i = upperBound; i < totalSpheres; i++) {
            totalSum += energyLevel[i];
        }

        return totalSum;
    }

    /**
     * Get average energy level across lattice.
     */
    public float getAverageEnergy() {
        return getTotalEnergy() / (float) totalSpheres;
    }

    /**
     * Get neighbor index with toroidal wrapping.
     */
    private int getNeighborIndex(int x, int y, int width, int height) {
        if (toroidal) {
            // Wrap around (toroidal topology)
            x = (x + width) % width;
            y = (y + height) % height;
            return getIndex(x, y);
        } else {
            // Non-toroidal: return -1 for out of bounds
            if (x < 0 || x >= width || y < 0 || y >= height) {
                return -1;
            }
            return getIndex(x, y);
        }
    }

    /**
     * Get linear index from 2D coordinates.
     */
    public int getIndex(int x, int y) {
        return y * gridWidth + x;
    }

    /**
     * Get energy level at position.
     */
    public int getEnergy(int x, int y) {
        return energyLevel[getIndex(x, y)];
    }

    /**
     * Get EM field at position.
     */
    public int getEM(int x, int y) {
        return emField[getIndex(x, y)];
    }

    /**
     * Print SIMD performance info.
     */
    public static void printSIMDInfo() {
        System.out.println("DiscreteLattice SIMD Configuration:");
        System.out.println("  Species: " + SPECIES);
        System.out.println("  Lane count: " + SPECIES.length() + " bytes per operation");
        System.out.println("  Vector width: " + SPECIES.vectorBitSize() + " bits");
        System.out.println("  Parallelism: " + SPECIES.length() + " quantum states per operation");
        System.out.println("  Speedup vs FloatVector: " + (SPECIES.length() / 8.0f) + "x more lanes");
    }
}
