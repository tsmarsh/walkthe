package com.plancklattice;

import jdk.incubator.vector.FloatVector;

/**
 * Vectorized EM wave propagation through the lattice.
 * Implements discrete wave equation: each timestep, the field propagates to neighbors.
 * Speed is inherently 1 hop per timestep (defines c).
 */
public class EMPropagator {

    private final PlanckLattice lattice;

    public EMPropagator(PlanckLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Propagate EM waves through the lattice using discrete wave equation.
     *
     * Discrete wave equation (2D):
     *   ψ(x,y,t+1) = average(neighbors) - damping × ψ(x,y,t) + phase_term
     *
     * For complex field (real + imaginary), apply to both components.
     * This creates wave propagation at exactly 1 hop per timestep.
     */
    public void propagate(float dt) {
        // Double buffering: read from current, write to next
        // We use emFieldRealNext and emFieldImagNext as temporary buffers

        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);

                // Get neighbor indices (with toroidal wrap-around)
                int rightIdx = lattice.getNeighborIndex(x, y, 1, 0);
                int leftIdx = lattice.getNeighborIndex(x, y, -1, 0);
                int downIdx = lattice.getNeighborIndex(x, y, 0, 1);
                int upIdx = lattice.getNeighborIndex(x, y, 0, -1);

                // Average of neighbor values (Laplacian approximation)
                float avgReal = (lattice.emFieldReal[rightIdx] +
                                 lattice.emFieldReal[leftIdx] +
                                 lattice.emFieldReal[downIdx] +
                                 lattice.emFieldReal[upIdx]) * 0.25f;

                float avgImag = (lattice.emFieldImag[rightIdx] +
                                 lattice.emFieldImag[leftIdx] +
                                 lattice.emFieldImag[downIdx] +
                                 lattice.emFieldImag[upIdx]) * 0.25f;

                // Current field values
                float currentReal = lattice.emFieldReal[index];
                float currentImag = lattice.emFieldImag[index];

                // Discrete wave equation: new = 2 * avg - old - damping * current
                // Simplified: new = avg - damping * current (simpler, still wave-like)
                float newReal = avgReal - PlanckLattice.EM_DAMPING * currentReal;
                float newImag = avgImag - PlanckLattice.EM_DAMPING * currentImag;

                // Store in next buffer
                lattice.emFieldRealNext[index] = newReal;
                lattice.emFieldImagNext[index] = newImag;
            }
        }

        // Copy next buffer back to current (swap buffers)
        System.arraycopy(lattice.emFieldRealNext, 0, lattice.emFieldReal, 0, lattice.totalSpheres);
        System.arraycopy(lattice.emFieldImagNext, 0, lattice.emFieldImag, 0, lattice.totalSpheres);
    }

    /**
     * Vectorized version of EM propagation (more complex due to neighbor access).
     * This demonstrates how to vectorize when possible despite irregular access patterns.
     */
    public void propagateVectorized(float dt) {
        // Note: Full vectorization is challenging with neighbor access patterns
        // We can vectorize the final computation step though

        // First, compute neighbor averages (scalar, due to irregular access)
        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);

                int rightIdx = lattice.getNeighborIndex(x, y, 1, 0);
                int leftIdx = lattice.getNeighborIndex(x, y, -1, 0);
                int downIdx = lattice.getNeighborIndex(x, y, 0, 1);
                int upIdx = lattice.getNeighborIndex(x, y, 0, -1);

                float avgReal = (lattice.emFieldReal[rightIdx] +
                                 lattice.emFieldReal[leftIdx] +
                                 lattice.emFieldReal[downIdx] +
                                 lattice.emFieldReal[upIdx]) * 0.25f;

                float avgImag = (lattice.emFieldImag[rightIdx] +
                                 lattice.emFieldImag[leftIdx] +
                                 lattice.emFieldImag[downIdx] +
                                 lattice.emFieldImag[upIdx]) * 0.25f;

                // Store averages in next buffers temporarily
                lattice.emFieldRealNext[index] = avgReal;
                lattice.emFieldImagNext[index] = avgImag;
            }
        }

        // Now vectorize the wave equation update
        int upperBound = PlanckLattice.SPECIES.loopBound(lattice.totalSpheres);

        FloatVector vDamping = FloatVector.broadcast(PlanckLattice.SPECIES, PlanckLattice.EM_DAMPING);

        for (int i = 0; i < upperBound; i += PlanckLattice.SPECIES.length()) {
            // Load averaged neighbor values
            FloatVector vAvgReal = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.emFieldRealNext, i);
            FloatVector vAvgImag = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.emFieldImagNext, i);

            // Load current values
            FloatVector vCurrentReal = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.emFieldReal, i);
            FloatVector vCurrentImag = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.emFieldImag, i);

            // Wave equation: new = avg - damping * current
            FloatVector vNewReal = vAvgReal.sub(vCurrentReal.mul(vDamping));
            FloatVector vNewImag = vAvgImag.sub(vCurrentImag.mul(vDamping));

            // Store back to current arrays
            vNewReal.intoArray(lattice.emFieldReal, i);
            vNewImag.intoArray(lattice.emFieldImag, i);
        }

        // Handle tail
        for (int i = upperBound; i < lattice.totalSpheres; i++) {
            float avgReal = lattice.emFieldRealNext[i];
            float avgImag = lattice.emFieldImagNext[i];
            float currentReal = lattice.emFieldReal[i];
            float currentImag = lattice.emFieldImag[i];

            lattice.emFieldReal[i] = avgReal - PlanckLattice.EM_DAMPING * currentReal;
            lattice.emFieldImag[i] = avgImag - PlanckLattice.EM_DAMPING * currentImag;
        }
    }

    /**
     * Calculate total EM field energy in the lattice.
     */
    public float calculateEMEnergy() {
        float totalEnergy = 0.0f;

        for (int i = 0; i < lattice.totalSpheres; i++) {
            float real = lattice.emFieldReal[i];
            float imag = lattice.emFieldImag[i];
            float amplitudeSq = real * real + imag * imag;
            totalEnergy += amplitudeSq;
        }

        return totalEnergy;
    }

    /**
     * Find the center of mass of the EM wave (useful for tracking propagation).
     */
    public float[] getWaveCenter() {
        float totalAmplitude = 0.0f;
        float centerX = 0.0f;
        float centerY = 0.0f;

        for (int i = 0; i < lattice.totalSpheres; i++) {
            float amplitude = (float) Math.sqrt(
                lattice.emFieldReal[i] * lattice.emFieldReal[i] +
                lattice.emFieldImag[i] * lattice.emFieldImag[i]
            );

            if (amplitude > 0.001f) {
                centerX += lattice.posX[i] * amplitude;
                centerY += lattice.posY[i] * amplitude;
                totalAmplitude += amplitude;
            }
        }

        if (totalAmplitude > 0.0f) {
            centerX /= totalAmplitude;
            centerY /= totalAmplitude;
        }

        return new float[]{centerX, centerY};
    }

    /**
     * Apply boundary conditions (absorbing boundaries to prevent reflection).
     */
    public void applyAbsorbingBoundaries(int borderWidth) {
        float dampingFactor = 0.9f; // Damping near boundaries

        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                // Check if near boundary
                boolean nearBoundary = (x < borderWidth || x >= lattice.gridWidth - borderWidth ||
                                        y < borderWidth || y >= lattice.gridHeight - borderWidth);

                if (nearBoundary) {
                    int index = lattice.getIndex(x, y);
                    lattice.emFieldReal[index] *= dampingFactor;
                    lattice.emFieldImag[index] *= dampingFactor;
                }
            }
        }
    }
}
