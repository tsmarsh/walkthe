package com.plancklattice;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a stable resonance pattern in the lattice (i.e., a "particle").
 * Particles are emergent structures formed through lattice annealing.
 */
public class ParticlePattern {
    private final int id;                          // Unique identifier
    private final int centerIndex;                 // Core sphere of the pattern
    private final Set<Integer> participatingSpheres; // All spheres in this pattern
    private float totalEnergy;                      // Energy in this resonance
    private float stability;                        // Energy barrier depth
    private int age;                                // How many timesteps it has existed
    private PackingType type;                       // Packing configuration

    // Tracking for stability determination
    private float lastTotalEnergy;
    private int unchangedCount;

    /**
     * Create a new particle pattern.
     *
     * @param id Unique particle identifier
     * @param centerIndex Index of central sphere
     * @param participatingSpheres Set of all sphere indices in this particle
     * @param totalEnergy Total energy in the pattern
     * @param stability Energy barrier (how hard to disrupt)
     * @param type Packing type classification
     */
    public ParticlePattern(int id, int centerIndex, Set<Integer> participatingSpheres,
                          float totalEnergy, float stability, PackingType type) {
        this.id = id;
        this.centerIndex = centerIndex;
        this.participatingSpheres = new HashSet<>(participatingSpheres);
        this.totalEnergy = totalEnergy;
        this.stability = stability;
        this.age = 0;
        this.type = type;
        this.lastTotalEnergy = totalEnergy;
        this.unchangedCount = 0;
    }

    /**
     * Update particle state for a new timestep.
     */
    public void ageOneTimestep() {
        age++;

        // Track energy stability
        if (Math.abs(totalEnergy - lastTotalEnergy) < 0.01f) {
            unchangedCount++;
        } else {
            unchangedCount = 0;
        }
        lastTotalEnergy = totalEnergy;
    }

    /**
     * Check if this pattern is truly stable (energy hasn't changed much).
     */
    public boolean isStable() {
        return unchangedCount > 10;  // Stable for 10+ timesteps
    }

    /**
     * Check if particle is self-sustaining (has internal resonance).
     */
    public boolean isSelfSustaining() {
        // For now, consider particles with sufficient energy and stability
        return totalEnergy > 1.0f && stability > 0.5f && isStable();
    }

    /**
     * Get the number of spheres in this particle.
     */
    public int getSize() {
        return participatingSpheres.size();
    }

    /**
     * Calculate center of mass position.
     */
    public void calculateCenterOfMass(PlanckLattice lattice, float[] comOut) {
        float sumX = 0.0f;
        float sumY = 0.0f;

        for (int idx : participatingSpheres) {
            sumX += lattice.posX[idx];
            sumY += lattice.posY[idx];
        }

        int size = participatingSpheres.size();
        comOut[0] = sumX / size;
        comOut[1] = sumY / size;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public int getCenterIndex() {
        return centerIndex;
    }

    public Set<Integer> getParticipatingSpheres() {
        return new HashSet<>(participatingSpheres);
    }

    public float getTotalEnergy() {
        return totalEnergy;
    }

    public void setTotalEnergy(float totalEnergy) {
        this.totalEnergy = totalEnergy;
    }

    public float getStability() {
        return stability;
    }

    public void setStability(float stability) {
        this.stability = stability;
    }

    public int getAge() {
        return age;
    }

    public PackingType getType() {
        return type;
    }

    public void setType(PackingType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        float[] com = new float[2];
        return String.format("Particle %d: Center idx=%d, Size=%d, Energy=%.2f, Stability=%.2f, Type=%s, Age=%d",
                           id, centerIndex, getSize(), totalEnergy, stability, type, age);
    }
}
