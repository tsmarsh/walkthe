package com.plancklattice;

import java.util.*;

/**
 * Detects and tracks stable particle patterns in the lattice.
 * Particles are emergent structures - stable, self-sustaining resonance patterns.
 */
public class ParticleDetector {

    private final PlanckLattice lattice;
    private final AnnealingEngine annealing;
    private int nextParticleId = 1;

    public ParticleDetector(PlanckLattice lattice, AnnealingEngine annealing) {
        this.lattice = lattice;
        this.annealing = annealing;
    }

    /**
     * Detect all particles in the current lattice state.
     * Updates the lattice's particle list.
     */
    public void detectParticles() {
        Set<Integer> processed = new HashSet<>();
        List<ParticlePattern> newParticles = new ArrayList<>();

        // Look for high-energy regions that are stable
        for (int i = 0; i < lattice.totalSpheres; i++) {
            if (processed.contains(i)) continue;

            if (lattice.energyDensity[i] > PlanckLattice.PARTICLE_THRESHOLD &&
                isStable(i)) {

                // Found potential particle - analyze the pattern
                ParticlePattern particle = analyzePattern(i, processed);

                if (particle != null && particle.isSelfSustaining()) {
                    newParticles.add(particle);
                }
            }
        }

        // Update particle tracking
        updateParticleList(newParticles);
    }

    /**
     * Check if a sphere's packing has been stable for sufficient time.
     */
    private boolean isStable(int index) {
        return lattice.stabilityHistory[index] > PlanckLattice.STABILITY_THRESHOLD;
    }

    /**
     * Analyze a high-energy region to determine if it forms a coherent particle.
     * Uses flood-fill to find all connected high-energy spheres.
     */
    private ParticlePattern analyzePattern(int startIndex, Set<Integer> globalProcessed) {
        Set<Integer> pattern = new HashSet<>();
        Queue<Integer> toExplore = new LinkedList<>();

        pattern.add(startIndex);
        toExplore.add(startIndex);
        globalProcessed.add(startIndex);

        // Flood-fill to find connected high-energy region
        while (!toExplore.isEmpty()) {
            int current = toExplore.poll();
            List<Integer> neighbors = annealing.getNeighbors(current);

            for (int neighbor : neighbors) {
                if (lattice.energyDensity[neighbor] > PlanckLattice.PARTICLE_THRESHOLD &&
                    !pattern.contains(neighbor)) {
                    pattern.add(neighbor);
                    toExplore.add(neighbor);
                    globalProcessed.add(neighbor);
                }
            }
        }

        // Need at least a few spheres to be a particle
        if (pattern.size() < 3) {
            return null;
        }

        // Calculate properties
        float totalEnergy = 0.0f;
        for (int idx : pattern) {
            totalEnergy += lattice.energyDensity[idx];
        }

        // Classify packing type
        PackingType type = classifyPacking(pattern);

        // Calculate stability (energy barrier)
        float stability = calculateStability(pattern);

        return new ParticlePattern(nextParticleId++, startIndex, pattern,
                                  totalEnergy, stability, type);
    }

    /**
     * Classify the packing type based on average coordination number.
     */
    private PackingType classifyPacking(Set<Integer> pattern) {
        if (pattern.isEmpty()) return PackingType.CUSTOM;

        // Calculate average coordination within the pattern
        int totalCoordination = 0;
        for (int sphere : pattern) {
            List<Integer> neighbors = annealing.getNeighbors(sphere);
            // Count only neighbors within the pattern
            int internalNeighbors = 0;
            for (int n : neighbors) {
                if (pattern.contains(n)) {
                    internalNeighbors++;
                }
            }
            totalCoordination += internalNeighbors;
        }

        int avgCoordination = totalCoordination / pattern.size();
        return PackingType.fromCoordination(avgCoordination);
    }

    /**
     * Calculate stability as the minimum energy barrier to disrupt the pattern.
     * Higher values = more stable.
     */
    private float calculateStability(Set<Integer> pattern) {
        // Find boundary spheres (have neighbors outside the pattern)
        Set<Integer> boundary = new HashSet<>();

        for (int sphere : pattern) {
            List<Integer> neighbors = annealing.getNeighbors(sphere);
            for (int n : neighbors) {
                if (!pattern.contains(n)) {
                    boundary.add(sphere);
                    break;
                }
            }
        }

        if (boundary.isEmpty()) {
            return Float.MAX_VALUE;  // Completely isolated = very stable
        }

        // Stability is proportional to average energy at boundary
        float totalEnergy = 0.0f;
        for (int sphere : boundary) {
            totalEnergy += lattice.energyDensity[sphere];
        }

        return totalEnergy / boundary.size();
    }

    /**
     * Update the particle list, aging existing particles and removing decayed ones.
     */
    private void updateParticleList(List<ParticlePattern> newParticles) {
        // Age existing particles
        for (ParticlePattern particle : lattice.particles) {
            particle.ageOneTimestep();
        }

        // Replace with new detection results
        lattice.particles.clear();
        lattice.particles.addAll(newParticles);
    }

    /**
     * Propagate internal resonance within detected particles.
     * Energy circulates within the particle structure.
     */
    public void propagateInternalResonance() {
        for (ParticlePattern particle : lattice.particles) {
            propagateResonanceInParticle(particle);
        }
    }

    /**
     * Apply resonance coupling within a single particle.
     */
    private void propagateResonanceInParticle(ParticlePattern particle) {
        Set<Integer> spheres = particle.getParticipatingSpheres();

        // For each sphere in the particle, couple to internal neighbors
        for (int sphere : spheres) {
            float phaseSum = 0.0f;
            int internalNeighbors = 0;

            List<Integer> neighbors = annealing.getNeighbors(sphere);
            for (int neighbor : neighbors) {
                if (spheres.contains(neighbor)) {
                    float phase = (float) Math.atan2(
                        lattice.emFieldImag[neighbor],
                        lattice.emFieldReal[neighbor]
                    );
                    phaseSum += phase;
                    internalNeighbors++;
                }
            }

            if (internalNeighbors > 0) {
                // Adjust EM field to maintain resonance
                float avgPhase = phaseSum / internalNeighbors;
                float amplitude = (float) Math.sqrt(
                    lattice.emFieldReal[sphere] * lattice.emFieldReal[sphere] +
                    lattice.emFieldImag[sphere] * lattice.emFieldImag[sphere]
                );

                // Add resonant coupling (keeps energy circulating)
                float coupling = PlanckLattice.RESONANCE_STRENGTH * amplitude;
                lattice.emFieldReal[sphere] += coupling * (float) Math.cos(avgPhase);
                lattice.emFieldImag[sphere] += coupling * (float) Math.sin(avgPhase);
            }
        }
    }

    /**
     * Print statistics about detected particles.
     */
    public void printParticleStatistics() {
        if (lattice.particles.isEmpty()) {
            return;
        }

        System.out.println("  Particles detected: " + lattice.particles.size());
        for (ParticlePattern p : lattice.particles) {
            float[] com = new float[2];
            p.calculateCenterOfMass(lattice, com);
            System.out.printf("    %s, COM: (%.2f, %.2f)%n", p, com[0], com[1]);
        }
    }

    /**
     * Get the total number of spheres participating in particles.
     */
    public int getTotalParticleSpheres() {
        int total = 0;
        for (ParticlePattern p : lattice.particles) {
            total += p.getSize();
        }
        return total;
    }
}
