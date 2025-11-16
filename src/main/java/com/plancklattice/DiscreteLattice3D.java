package com.plancklattice;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

/**
 * 3D discrete quantum lattice using native 2-bit quantum states.
 *
 * Extension to 3D space - same fundamental rules, 6 neighbors instead of 4.
 * Quantum states (0-3) with gradient-flow rules where waves emerge naturally.
 */
public class DiscreteLattice3D {

    // Quantum state levels (2 bits = 4 levels)
    public static final byte LEVEL_0 = 0;  // Vacuum / No field
    public static final byte LEVEL_1 = 1;  // 1 quantum
    public static final byte LEVEL_2 = 2;  // 2 quanta
    public static final byte LEVEL_3 = 3;  // 3 quanta (maximum)

    // Vector species for SIMD operations
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    // Lattice dimensions
    public final int gridWidth;   // X dimension
    public final int gridHeight;  // Y dimension
    public final int gridDepth;   // Z dimension
    public final int totalSites;

    // Discrete quantum states (0-3 for each)
    public final byte[] energyLevel;    // Energy quanta at each site
    public final byte[] emField;        // EM field strength
    public final byte[] nextEnergy;     // Double buffer for updates
    public final byte[] nextEM;         // Double buffer for EM

    // Configuration
    private final boolean toroidal;
    private int stepCount = 0;

    public DiscreteLattice3D(int width, int height, int depth) {
        this(width, height, depth, true);
    }

    public DiscreteLattice3D(int width, int height, int depth, boolean toroidal) {
        this.gridWidth = width;
        this.gridHeight = height;
        this.gridDepth = depth;
        this.totalSites = width * height * depth;
        this.toroidal = toroidal;

        // Allocate state arrays
        this.energyLevel = new byte[totalSites];
        this.emField = new byte[totalSites];
        this.nextEnergy = new byte[totalSites];
        this.nextEM = new byte[totalSites];

        // Initialize to vacuum
        initializeVacuum();
    }

    /**
     * Initialize lattice to vacuum state (all zeros).
     * Vectorized initialization.
     */
    public void initializeVacuum() {
        int upperBound = SPECIES.loopBound(totalSites);
        ByteVector vZero = ByteVector.zero(SPECIES);

        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            vZero.intoArray(energyLevel, i);
            vZero.intoArray(emField, i);
        }

        for (int i = upperBound; i < totalSites; i++) {
            energyLevel[i] = LEVEL_0;
            emField[i] = LEVEL_0;
        }
    }

    /**
     * Add energy quantum at a specific 3D location.
     */
    public void addEnergyQuantum(int x, int y, int z, int quanta) {
        int index = getIndex(x, y, z);
        int newLevel = energyLevel[index] + quanta;
        energyLevel[index] = (byte) Math.min(newLevel, LEVEL_3);
    }

    /**
     * Create EM pulse at a 3D location.
     */
    public void createEMPulse(int x, int y, int z, int strength) {
        int index = getIndex(x, y, z);
        emField[index] = (byte) Math.min(strength, LEVEL_3);
    }

    /**
     * FUNDAMENTAL QUANTUM RULES in 3D (not wave equations!)
     *
     * Rule: Energy quanta flow from high→low energy sites
     * - 6 neighbors instead of 4 (±X, ±Y, ±Z)
     * - Local nearest-neighbor interactions
     * - Perfect energy conservation
     * - Waves emerge naturally from gradient flow
     */
    public void propagateEnergy() {
        System.arraycopy(energyLevel, 0, nextEnergy, 0, totalSites);

        for (int z = 0; z < gridDepth; z++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    int idx = getIndex(x, y, z);
                    int energy = energyLevel[idx];

                    if (energy == 0) continue;

                    // Get 6 neighbors (±X, ±Y, ±Z)
                    int[] neighbors = new int[6];
                    neighbors[0] = getNeighborIndex(x + 1, y, z);  // +X
                    neighbors[1] = getNeighborIndex(x - 1, y, z);  // -X
                    neighbors[2] = getNeighborIndex(x, y + 1, z);  // +Y
                    neighbors[3] = getNeighborIndex(x, y - 1, z);  // -Y
                    neighbors[4] = getNeighborIndex(x, y, z + 1);  // +Z
                    neighbors[5] = getNeighborIndex(x, y, z - 1);  // -Z

                    // Count neighbors with lower energy
                    int lowerCount = 0;
                    for (int n : neighbors) {
                        if (n != -1 && energyLevel[n] < energy) {
                            lowerCount++;
                        }
                    }

                    // Transfer 1 quantum to a lower neighbor
                    if (lowerCount > 0 && energy > 0) {
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
        }

        stepCount++;
        System.arraycopy(nextEnergy, 0, energyLevel, 0, totalSites);
    }

    /**
     * Propagate EM field in 3D.
     */
    public void propagateEM() {
        int upperBound = SPECIES.loopBound(totalSites);

        ByteVector vOne = ByteVector.broadcast(SPECIES, (byte) 1);
        ByteVector vZero = ByteVector.zero(SPECIES);

        // Decay
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            ByteVector vField = ByteVector.fromArray(SPECIES, emField, i);
            ByteVector vDecayed = vField.sub(vOne).max(vZero);
            vDecayed.intoArray(nextEM, i);
        }

        for (int i = upperBound; i < totalSites; i++) {
            nextEM[i] = (byte) Math.max(0, emField[i] - 1);
        }

        // Propagate to 6 neighbors
        for (int z = 0; z < gridDepth; z++) {
            for (int y = 0; y < gridHeight; y++) {
                for (int x = 0; x < gridWidth; x++) {
                    int idx = getIndex(x, y, z);
                    int field = emField[idx];

                    if (field <= LEVEL_1) continue;

                    int[] neighbors = new int[6];
                    neighbors[0] = getNeighborIndex(x + 1, y, z);
                    neighbors[1] = getNeighborIndex(x - 1, y, z);
                    neighbors[2] = getNeighborIndex(x, y + 1, z);
                    neighbors[3] = getNeighborIndex(x, y - 1, z);
                    neighbors[4] = getNeighborIndex(x, y, z + 1);
                    neighbors[5] = getNeighborIndex(x, y, z - 1);

                    int spreadField = field - 1;
                    for (int n : neighbors) {
                        if (n != -1) {
                            nextEM[n] = (byte) Math.max(nextEM[n], spreadField);
                        }
                    }
                }
            }
        }

        System.arraycopy(nextEM, 0, emField, 0, totalSites);
    }

    /**
     * Get total energy (vectorized).
     */
    public int getTotalEnergy() {
        int upperBound = SPECIES.loopBound(totalSites);
        int totalSum = 0;

        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            ByteVector vEnergy = ByteVector.fromArray(SPECIES, energyLevel, i);
            int chunkSum = vEnergy.reduceLanes(VectorOperators.ADD);
            totalSum += chunkSum;
        }

        for (int i = upperBound; i < totalSites; i++) {
            totalSum += energyLevel[i];
        }

        return totalSum;
    }

    /**
     * Get average energy.
     */
    public float getAverageEnergy() {
        return getTotalEnergy() / (float) totalSites;
    }

    /**
     * Get neighbor index with toroidal wrapping in 3D.
     */
    private int getNeighborIndex(int x, int y, int z) {
        if (toroidal) {
            x = (x + gridWidth) % gridWidth;
            y = (y + gridHeight) % gridHeight;
            z = (z + gridDepth) % gridDepth;
            return getIndex(x, y, z);
        } else {
            if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight || z < 0 || z >= gridDepth) {
                return -1;
            }
            return getIndex(x, y, z);
        }
    }

    /**
     * Get linear index from 3D coordinates.
     * Layout: z * (width * height) + y * width + x
     */
    public int getIndex(int x, int y, int z) {
        return z * gridWidth * gridHeight + y * gridWidth + x;
    }

    /**
     * Get energy level at 3D position.
     */
    public int getEnergy(int x, int y, int z) {
        return energyLevel[getIndex(x, y, z)];
    }

    /**
     * Get EM field at 3D position.
     */
    public int getEM(int x, int y, int z) {
        return emField[getIndex(x, y, z)];
    }

    /**
     * Print SIMD info.
     */
    public static void printSIMDInfo() {
        System.out.println("DiscreteLattice3D SIMD Configuration:");
        System.out.println("  Species: " + SPECIES);
        System.out.println("  Lane count: " + SPECIES.length() + " bytes per operation");
        System.out.println("  Vector width: " + SPECIES.vectorBitSize() + " bits");
        System.out.println("  3D: 6 neighbors per site (±X, ±Y, ±Z)");
    }
}
