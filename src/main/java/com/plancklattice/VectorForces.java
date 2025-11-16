package com.plancklattice;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;

/**
 * Vectorized force calculations for the Planck lattice.
 * Handles both spacing forces (maintaining equilibrium distance) and gravitational compression.
 */
public class VectorForces {

    private final PlanckLattice lattice;

    public VectorForces(PlanckLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Calculate spacing forces to maintain equilibrium distance between neighbors.
     * Uses Hooke's law: F = k(d - d₀) where d₀ = 1.0
     * This is the primary force keeping the lattice structured.
     */
    public void calculateSpacingForces() {
        // Process each sphere's interaction with its 4 neighbors (up, down, left, right)
        // Note: Due to neighbor dependencies, this is harder to fully vectorize,
        // but we can still vectorize some operations within the loop

        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);

                float px = lattice.posX[index];
                float py = lattice.posY[index];

                // Check all 4 neighbors
                // Right neighbor
                if (x < lattice.gridWidth - 1) {
                    addSpacingForce(index, lattice.getIndex(x + 1, y), px, py);
                } else {
                    // Wrap-around (toroidal boundary)
                    addSpacingForce(index, lattice.getIndex(0, y), px, py);
                }

                // Left neighbor
                if (x > 0) {
                    addSpacingForce(index, lattice.getIndex(x - 1, y), px, py);
                } else {
                    // Wrap-around
                    addSpacingForce(index, lattice.getIndex(lattice.gridWidth - 1, y), px, py);
                }

                // Down neighbor
                if (y < lattice.gridHeight - 1) {
                    addSpacingForce(index, lattice.getIndex(x, y + 1), px, py);
                } else {
                    // Wrap-around
                    addSpacingForce(index, lattice.getIndex(x, 0), px, py);
                }

                // Up neighbor
                if (y > 0) {
                    addSpacingForce(index, lattice.getIndex(x, y - 1), px, py);
                } else {
                    // Wrap-around
                    addSpacingForce(index, lattice.getIndex(x, lattice.gridHeight - 1), px, py);
                }
            }
        }
    }

    /**
     * Add spacing force between two spheres (Hooke's law).
     */
    private void addSpacingForce(int index1, int index2, float px1, float py1) {
        float px2 = lattice.posX[index2];
        float py2 = lattice.posY[index2];

        // Vector from sphere1 to sphere2
        float dx = px2 - px1;
        float dy = py2 - py1;

        // Current distance
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.0001f) { // Avoid division by zero
            // Hooke's law: F = k(d - d₀)
            // Positive force means attraction, negative means repulsion
            float forceMag = PlanckLattice.SPRING_K * (dist - PlanckLattice.EQUILIBRIUM_DISTANCE);

            // Normalize direction
            float nx = dx / dist;
            float ny = dy / dist;

            // Apply force (equal and opposite)
            lattice.forceX[index1] += forceMag * nx;
            lattice.forceY[index1] += forceMag * ny;
        }
    }

    /**
     * Calculate gravitational compression forces.
     * High energy density attracts neighbors, compressing local spacetime geometry.
     * F_gravity = -G × energyDensity × distance_to_neighbor
     */
    public void calculateGravityForces() {
        // This can be partially vectorized
        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);

                float energy = lattice.energyDensity[index];
                if (energy < 0.0001f) continue; // Skip if negligible energy

                float px = lattice.posX[index];
                float py = lattice.posY[index];

                // Apply gravitational attraction to all 4 neighbors
                // Right neighbor
                if (x < lattice.gridWidth - 1) {
                    addGravityForce(index, lattice.getIndex(x + 1, y), px, py, energy);
                } else {
                    addGravityForce(index, lattice.getIndex(0, y), px, py, energy);
                }

                // Left neighbor
                if (x > 0) {
                    addGravityForce(index, lattice.getIndex(x - 1, y), px, py, energy);
                } else {
                    addGravityForce(index, lattice.getIndex(lattice.gridWidth - 1, y), px, py, energy);
                }

                // Down neighbor
                if (y < lattice.gridHeight - 1) {
                    addGravityForce(index, lattice.getIndex(x, y + 1), px, py, energy);
                } else {
                    addGravityForce(index, lattice.getIndex(x, 0), px, py, energy);
                }

                // Up neighbor
                if (y > 0) {
                    addGravityForce(index, lattice.getIndex(x, y - 1), px, py, energy);
                } else {
                    addGravityForce(index, lattice.getIndex(x, lattice.gridHeight - 1), px, py, energy);
                }
            }
        }
    }

    /**
     * Add gravitational attraction force from energetic sphere to neighbor.
     */
    private void addGravityForce(int energyIndex, int neighborIndex, float px1, float py1, float energy) {
        float px2 = lattice.posX[neighborIndex];
        float py2 = lattice.posY[neighborIndex];

        // Vector from neighbor to energy source
        float dx = px1 - px2;
        float dy = py1 - py2;

        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > 0.0001f) {
            // Gravitational force: F = G × energy × distance
            // (Simplified model: linear with distance instead of inverse square)
            // Positive magnitude keeps the direction attractive (toward the energy source)
            float forceMag = PlanckLattice.GRAVITY_G * energy * dist;

            // Normalize direction
            float nx = dx / dist;
            float ny = dy / dist;

            // Apply attractive force to neighbor (pulls toward energy source)
            lattice.forceX[neighborIndex] += forceMag * nx;
            lattice.forceY[neighborIndex] += forceMag * ny;
        }
    }

    /**
     * Vectorized force magnitude calculation (demonstrates Vector API usage).
     * This shows how to vectorize operations when possible.
     */
    public void normalizeForces(float maxForce) {
        int upperBound = PlanckLattice.SPECIES.loopBound(lattice.totalSpheres);

        FloatVector vMaxForce = FloatVector.broadcast(PlanckLattice.SPECIES, maxForce);
        FloatVector vMaxForceSq = vMaxForce.mul(vMaxForce);

        // Vectorized loop
        for (int i = 0; i < upperBound; i += PlanckLattice.SPECIES.length()) {
            FloatVector vFx = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.forceX, i);
            FloatVector vFy = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.forceY, i);

            // Calculate force magnitude squared
            FloatVector vForceSq = vFx.mul(vFx).add(vFy.mul(vFy));

            // Create mask for forces that exceed maximum
            VectorMask<Float> mask = vForceSq.compare(jdk.incubator.vector.VectorOperators.GT, vMaxForceSq);

            // Calculate scale factor only where needed
            FloatVector vScale = vMaxForceSq.div(vForceSq).sqrt();

            // Apply scaling
            vFx = vFx.mul(vScale, mask);
            vFy = vFy.mul(vScale, mask);

            // Store back
            vFx.intoArray(lattice.forceX, i);
            vFy.intoArray(lattice.forceY, i);
        }

        // Handle tail elements
        for (int i = upperBound; i < lattice.totalSpheres; i++) {
            float fx = lattice.forceX[i];
            float fy = lattice.forceY[i];
            float forceSq = fx * fx + fy * fy;

            if (forceSq > maxForce * maxForce) {
                float scale = maxForce / (float) Math.sqrt(forceSq);
                lattice.forceX[i] *= scale;
                lattice.forceY[i] *= scale;
            }
        }
    }
}
