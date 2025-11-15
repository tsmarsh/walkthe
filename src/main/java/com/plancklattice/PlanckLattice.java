package com.plancklattice;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Core data structure representing the Planck-scale lattice using Structure of Arrays (SoA)
 * for optimal SIMD vectorization with Java's Vector API.
 */
public class PlanckLattice {

    // Vector API species - uses optimal SIMD width for the platform
    public static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    // Grid dimensions
    public final int gridWidth;
    public final int gridHeight;
    public final int totalSpheres;

    // Structure of Arrays - separate arrays for each property enable efficient vectorization
    // Position arrays (can drift from grid positions)
    public final float[] posX;
    public final float[] posY;

    // Velocity arrays for lattice dynamics
    public final float[] velX;
    public final float[] velY;

    // Force accumulation arrays (cleared each timestep)
    public final float[] forceX;
    public final float[] forceY;

    // Energy density at each lattice site (causes gravitational compression)
    public final float[] energyDensity;

    // Complex EM field for wave propagation (real and imaginary components)
    public final float[] emFieldReal;
    public final float[] emFieldImag;

    // Temporary arrays for EM field updates (double buffering)
    public final float[] emFieldRealNext;
    public final float[] emFieldImagNext;

    // Physics parameters
    public static final float SPRING_K = 1.0f;              // Spring constant for spacing
    public static final float EQUILIBRIUM_DISTANCE = 1.0f;   // Target spacing (Planck length)
    public static final float GRAVITY_G = 0.01f;             // Gravitational constant
    public static final float EM_DAMPING = 0.01f;            // EM wave damping
    public static final float EM_SPEED = 1.0f;               // Inherent: 1 hop per timestep
    public static final float SPHERE_MASS = 1.0f;            // Mass of each sphere

    /**
     * Create a new Planck lattice with the given dimensions.
     * Initializes all spheres in a regular grid with 1.0 spacing.
     */
    public PlanckLattice(int width, int height) {
        this.gridWidth = width;
        this.gridHeight = height;
        this.totalSpheres = width * height;

        // Allocate all arrays
        this.posX = new float[totalSpheres];
        this.posY = new float[totalSpheres];
        this.velX = new float[totalSpheres];
        this.velY = new float[totalSpheres];
        this.forceX = new float[totalSpheres];
        this.forceY = new float[totalSpheres];
        this.energyDensity = new float[totalSpheres];
        this.emFieldReal = new float[totalSpheres];
        this.emFieldImag = new float[totalSpheres];
        this.emFieldRealNext = new float[totalSpheres];
        this.emFieldImagNext = new float[totalSpheres];

        // Initialize positions in regular grid
        initializeGrid();
    }

    /**
     * Initialize all spheres in a regular grid with equilibrium spacing.
     */
    private void initializeGrid() {
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int index = y * gridWidth + x;
                posX[index] = x * EQUILIBRIUM_DISTANCE;
                posY[index] = y * EQUILIBRIUM_DISTANCE;
                // All other arrays default to 0.0f
            }
        }
    }

    /**
     * Get the 1D array index for a given 2D grid position.
     */
    public int getIndex(int x, int y) {
        return y * gridWidth + x;
    }

    /**
     * Get the grid X coordinate from a 1D index.
     */
    public int getGridX(int index) {
        return index % gridWidth;
    }

    /**
     * Get the grid Y coordinate from a 1D index.
     */
    public int getGridY(int index) {
        return index / gridWidth;
    }

    /**
     * Check if grid coordinates are within bounds.
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
    }

    /**
     * Get neighbor index with boundary handling (toroidal wrap-around).
     */
    public int getNeighborIndex(int x, int y, int dx, int dy) {
        int nx = (x + dx + gridWidth) % gridWidth;
        int ny = (y + dy + gridHeight) % gridHeight;
        return getIndex(nx, ny);
    }

    /**
     * Clear all force accumulators (call at start of each timestep).
     */
    public void clearForces() {
        int upperBound = SPECIES.loopBound(totalSpheres);

        // Vectorized clearing
        FloatVector zero = FloatVector.zero(SPECIES);
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            zero.intoArray(forceX, i);
            zero.intoArray(forceY, i);
        }

        // Handle tail elements
        for (int i = upperBound; i < totalSpheres; i++) {
            forceX[i] = 0.0f;
            forceY[i] = 0.0f;
        }
    }

    /**
     * Add a Gaussian EM wave pulse centered at the given position.
     */
    public void addEMPulse(float centerX, float centerY, float amplitude, float sigma, float wavelength) {
        for (int i = 0; i < totalSpheres; i++) {
            float dx = posX[i] - centerX;
            float dy = posY[i] - centerY;
            float distSq = dx * dx + dy * dy;
            float dist = (float) Math.sqrt(distSq);

            // Gaussian envelope
            float envelope = amplitude * (float) Math.exp(-distSq / (sigma * sigma));

            // Wave component
            float k = 2.0f * (float) Math.PI / wavelength;
            float phase = k * dist;

            emFieldReal[i] = envelope * (float) Math.cos(phase);
            emFieldImag[i] = envelope * (float) Math.sin(phase);
        }
    }

    /**
     * Add a concentrated mass (energy density) at the given position.
     */
    public void addMassConcentration(float centerX, float centerY, float mass, float radius) {
        for (int i = 0; i < totalSpheres; i++) {
            float dx = posX[i] - centerX;
            float dy = posY[i] - centerY;
            float distSq = dx * dx + dy * dy;

            // Gaussian distribution of mass
            float energy = mass * (float) Math.exp(-distSq / (radius * radius));
            energyDensity[i] += energy;
        }
    }

    /**
     * Calculate average lattice spacing (useful for monitoring compression).
     */
    public float getAverageSpacing() {
        float totalSpacing = 0.0f;
        int count = 0;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                int index = getIndex(x, y);

                // Check right neighbor
                if (x < gridWidth - 1) {
                    int rightIndex = getIndex(x + 1, y);
                    float dx = posX[rightIndex] - posX[index];
                    float dy = posY[rightIndex] - posY[index];
                    totalSpacing += (float) Math.sqrt(dx * dx + dy * dy);
                    count++;
                }

                // Check down neighbor
                if (y < gridHeight - 1) {
                    int downIndex = getIndex(x, y + 1);
                    float dx = posX[downIndex] - posX[index];
                    float dy = posY[downIndex] - posY[index];
                    totalSpacing += (float) Math.sqrt(dx * dx + dy * dy);
                    count++;
                }
            }
        }

        return count > 0 ? totalSpacing / count : 0.0f;
    }

    /**
     * Get total energy in the system.
     */
    public float getTotalEnergy() {
        float total = 0.0f;
        for (int i = 0; i < totalSpheres; i++) {
            total += energyDensity[i];
        }
        return total;
    }

    /**
     * Get maximum EM field amplitude.
     */
    public float getMaxEMAmplitude() {
        float max = 0.0f;
        for (int i = 0; i < totalSpheres; i++) {
            float amplitude = (float) Math.sqrt(
                emFieldReal[i] * emFieldReal[i] +
                emFieldImag[i] * emFieldImag[i]
            );
            if (amplitude > max) {
                max = amplitude;
            }
        }
        return max;
    }
}
