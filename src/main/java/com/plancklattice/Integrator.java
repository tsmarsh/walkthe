package com.plancklattice;

import jdk.incubator.vector.FloatVector;

/**
 * Vectorized position and velocity integration using Verlet method.
 * Updates sphere positions based on accumulated forces.
 */
public class Integrator {

    private final PlanckLattice lattice;

    public Integrator(PlanckLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Update positions and velocities using Velocity Verlet integration.
     * This is a symplectic integrator that conserves energy well.
     *
     * Velocity Verlet:
     *   v(t+dt/2) = v(t) + a(t) * dt/2
     *   x(t+dt) = x(t) + v(t+dt/2) * dt
     *   v(t+dt) = v(t+dt/2) + a(t+dt) * dt/2
     *
     * For simplicity, we're using semi-implicit Euler here (simpler, still stable):
     *   v(t+dt) = v(t) + a(t) * dt
     *   x(t+dt) = x(t) + v(t+dt) * dt
     */
    public void integrate(float dt) {
        int upperBound = PlanckLattice.SPECIES.loopBound(lattice.totalSpheres);

        // Precompute constants as vectors
        FloatVector vDt = FloatVector.broadcast(PlanckLattice.SPECIES, dt);
        FloatVector vMassInv = FloatVector.broadcast(PlanckLattice.SPECIES, 1.0f / PlanckLattice.SPHERE_MASS);

        // Vectorized integration loop
        for (int i = 0; i < upperBound; i += PlanckLattice.SPECIES.length()) {
            // Load current state
            FloatVector vPosX = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.posX, i);
            FloatVector vPosY = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.posY, i);
            FloatVector vVelX = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.velX, i);
            FloatVector vVelY = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.velY, i);
            FloatVector vForceX = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.forceX, i);
            FloatVector vForceY = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.forceY, i);

            // Calculate acceleration: a = F / m
            FloatVector vAccelX = vForceX.mul(vMassInv);
            FloatVector vAccelY = vForceY.mul(vMassInv);

            // Update velocity: v = v + a * dt
            vVelX = vVelX.add(vAccelX.mul(vDt));
            vVelY = vVelY.add(vAccelY.mul(vDt));

            // Update position: x = x + v * dt
            vPosX = vPosX.add(vVelX.mul(vDt));
            vPosY = vPosY.add(vVelY.mul(vDt));

            // Store updated state
            vPosX.intoArray(lattice.posX, i);
            vPosY.intoArray(lattice.posY, i);
            vVelX.intoArray(lattice.velX, i);
            vVelY.intoArray(lattice.velY, i);
        }

        // Handle tail elements (scalar code for remaining elements)
        float dtScalar = dt;
        float massInv = 1.0f / PlanckLattice.SPHERE_MASS;

        for (int i = upperBound; i < lattice.totalSpheres; i++) {
            // Calculate acceleration
            float ax = lattice.forceX[i] * massInv;
            float ay = lattice.forceY[i] * massInv;

            // Update velocity
            lattice.velX[i] += ax * dtScalar;
            lattice.velY[i] += ay * dtScalar;

            // Update position
            lattice.posX[i] += lattice.velX[i] * dtScalar;
            lattice.posY[i] += lattice.velY[i] * dtScalar;
        }
    }

    /**
     * Apply velocity damping to prevent runaway oscillations.
     * Useful for stability, especially during initial transients.
     */
    public void applyDamping(float dampingFactor) {
        if (dampingFactor <= 0.0f || dampingFactor >= 1.0f) return;

        int upperBound = PlanckLattice.SPECIES.loopBound(lattice.totalSpheres);

        FloatVector vDamping = FloatVector.broadcast(PlanckLattice.SPECIES, 1.0f - dampingFactor);

        // Vectorized damping
        for (int i = 0; i < upperBound; i += PlanckLattice.SPECIES.length()) {
            FloatVector vVelX = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.velX, i);
            FloatVector vVelY = FloatVector.fromArray(PlanckLattice.SPECIES, lattice.velY, i);

            vVelX = vVelX.mul(vDamping);
            vVelY = vVelY.mul(vDamping);

            vVelX.intoArray(lattice.velX, i);
            vVelY.intoArray(lattice.velY, i);
        }

        // Handle tail
        float damping = 1.0f - dampingFactor;
        for (int i = upperBound; i < lattice.totalSpheres; i++) {
            lattice.velX[i] *= damping;
            lattice.velY[i] *= damping;
        }
    }

    /**
     * Calculate total kinetic energy in the lattice.
     */
    public float calculateKineticEnergy() {
        float totalKE = 0.0f;

        for (int i = 0; i < lattice.totalSpheres; i++) {
            float vx = lattice.velX[i];
            float vy = lattice.velY[i];
            float speedSq = vx * vx + vy * vy;
            totalKE += 0.5f * PlanckLattice.SPHERE_MASS * speedSq;
        }

        return totalKE;
    }

    /**
     * Calculate total potential energy from spacing (elastic potential).
     */
    public float calculatePotentialEnergy() {
        float totalPE = 0.0f;

        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);
                float px = lattice.posX[index];
                float py = lattice.posY[index];

                // Only count right and down neighbors to avoid double-counting
                if (x < lattice.gridWidth - 1) {
                    int rightIndex = lattice.getIndex(x + 1, y);
                    float dx = lattice.posX[rightIndex] - px;
                    float dy = lattice.posY[rightIndex] - py;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float displacement = dist - PlanckLattice.EQUILIBRIUM_DISTANCE;
                    totalPE += 0.5f * PlanckLattice.SPRING_K * displacement * displacement;
                }

                if (y < lattice.gridHeight - 1) {
                    int downIndex = lattice.getIndex(x, y + 1);
                    float dx = lattice.posX[downIndex] - px;
                    float dy = lattice.posY[downIndex] - py;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    float displacement = dist - PlanckLattice.EQUILIBRIUM_DISTANCE;
                    totalPE += 0.5f * PlanckLattice.SPRING_K * displacement * displacement;
                }
            }
        }

        return totalPE;
    }
}
