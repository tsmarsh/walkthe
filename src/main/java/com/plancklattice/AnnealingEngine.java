package com.plancklattice;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import java.util.*;

/**
 * Handles lattice annealing - the process by which high-energy regions
 * optimize their local packing to find stable, low-energy configurations.
 * This is the mechanism behind "strong force" and particle formation.
 */
public class AnnealingEngine {

    private static final VectorSpecies<Float> SPECIES = PlanckLattice.SPECIES;

    private final PlanckLattice lattice;
    private final Random random;

    // Neighbor cache for performance
    private final List<List<Integer>> neighborCache;

    private static final float MAX_POSITION_DELTA = 0.3f;  // Max change in position per annealing step

    public AnnealingEngine(PlanckLattice lattice) {
        this.lattice = lattice;
        this.random = new Random(42);  // Seeded for reproducibility in testing
        this.neighborCache = new ArrayList<>(lattice.totalSpheres);

        // Initialize neighbor cache
        for (int i = 0; i < lattice.totalSpheres; i++) {
            neighborCache.add(new ArrayList<>());
        }

        // Build initial neighbor lists
        updateNeighborLists();
    }

    /**
     * Set random seed for reproducible testing.
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    /**
     * Update neighbor lists based on current positions (distance-based).
     * This is expensive, so only call when positions have changed significantly.
     */
    public void updateNeighborLists() {
        float radiusSq = PlanckLattice.NEIGHBOR_RADIUS * PlanckLattice.NEIGHBOR_RADIUS;

        for (int i = 0; i < lattice.totalSpheres; i++) {
            neighborCache.get(i).clear();

            float xi = lattice.posX[i];
            float yi = lattice.posY[i];

            for (int j = 0; j < lattice.totalSpheres; j++) {
                if (i == j) continue;

                float dx = lattice.posX[j] - xi;
                float dy = lattice.posY[j] - yi;

                // Handle toroidal wrapping
                float width = lattice.gridWidth * PlanckLattice.EQUILIBRIUM_DISTANCE;
                float height = lattice.gridHeight * PlanckLattice.EQUILIBRIUM_DISTANCE;

                if (dx > width / 2) dx -= width;
                if (dx < -width / 2) dx += width;
                if (dy > height / 2) dy -= height;
                if (dy < -height / 2) dy += height;

                float distSq = dx * dx + dy * dy;

                if (distSq < radiusSq) {
                    neighborCache.get(i).add(j);
                }
            }
        }
    }

    /**
     * Get neighbors for a given sphere index.
     */
    public List<Integer> getNeighbors(int index) {
        return neighborCache.get(index);
    }

    /**
     * Identify which regions should be annealing based on energy density.
     * Uses vectorized operations for performance.
     */
    public void identifyAnnealingRegions() {
        int upperBound = SPECIES.loopBound(lattice.totalSpheres);

        FloatVector threshold = FloatVector.broadcast(SPECIES, PlanckLattice.ANNEALING_THRESHOLD);
        FloatVector initialTemp = FloatVector.broadcast(SPECIES, PlanckLattice.INITIAL_TEMPERATURE);

        // Vectorized check: where energy density > threshold
        for (int i = 0; i < upperBound; i += SPECIES.length()) {
            FloatVector energy = FloatVector.fromArray(SPECIES, lattice.energyDensity, i);
            VectorMask<Float> shouldAnneal = energy.compare(VectorOperators.GT, threshold);

            // Set isAnnealing flags
            for (int lane = 0; lane < SPECIES.length(); lane++) {
                int idx = i + lane;
                if (shouldAnneal.laneIsSet(lane)) {
                    lattice.isAnnealing[idx] = true;
                    if (lattice.annealingTemperature[idx] == 0.0f) {
                        lattice.annealingTemperature[idx] = PlanckLattice.INITIAL_TEMPERATURE;
                    }
                } else {
                    lattice.isAnnealing[idx] = false;
                }
            }
        }

        // Handle tail elements
        for (int i = upperBound; i < lattice.totalSpheres; i++) {
            if (lattice.energyDensity[i] > PlanckLattice.ANNEALING_THRESHOLD) {
                lattice.isAnnealing[i] = true;
                if (lattice.annealingTemperature[i] == 0.0f) {
                    lattice.annealingTemperature[i] = PlanckLattice.INITIAL_TEMPERATURE;
                }
            } else {
                lattice.isAnnealing[i] = false;
            }
        }
    }

    /**
     * Calculate local energy for a sphere.
     * Includes spacing energy, coordination energy, EM energy, and gravity.
     */
    public float calculateLocalEnergy(int index) {
        float energy = 0.0f;

        float xi = lattice.posX[index];
        float yi = lattice.posY[index];

        List<Integer> neighbors = getNeighbors(index);

        // Spring energy from spacing
        for (int neighbor : neighbors) {
            float dx = lattice.posX[neighbor] - xi;
            float dy = lattice.posY[neighbor] - yi;

            // Handle toroidal wrapping
            float width = lattice.gridWidth * PlanckLattice.EQUILIBRIUM_DISTANCE;
            float height = lattice.gridHeight * PlanckLattice.EQUILIBRIUM_DISTANCE;

            if (dx > width / 2) dx -= width;
            if (dx < -width / 2) dx += width;
            if (dy > height / 2) dy -= height;
            if (dy < -height / 2) dy += height;

            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float delta = dist - PlanckLattice.EQUILIBRIUM_DISTANCE;
            float springE = 0.5f * PlanckLattice.SPRING_K * delta * delta;
            energy += springE;
        }

        // Coordination energy (certain neighbor counts are more stable)
        int coordination = neighbors.size();
        energy += getCoordinationEnergy(coordination);

        // EM field energy
        float emE = lattice.emFieldReal[index] * lattice.emFieldReal[index] +
                   lattice.emFieldImag[index] * lattice.emFieldImag[index];
        energy += emE;

        // Gravitational potential (negative contribution)
        energy -= PlanckLattice.GRAVITY_G * lattice.energyDensity[index];

        return energy;
    }

    /**
     * Get energy bonus/penalty for a given coordination number.
     */
    private float getCoordinationEnergy(int coordination) {
        if (coordination >= 0 && coordination < PlanckLattice.COORDINATION_ENERGY.length) {
            return PlanckLattice.COORDINATION_ENERGY[coordination];
        }
        return 0.5f;  // Penalty for unusual coordination
    }

    /**
     * Attempt to change the packing configuration for a sphere using simulated annealing.
     * Uses Metropolis criterion for stochastic acceptance.
     */
    public void attemptPackingChange(int sphereIndex) {
        if (!lattice.isAnnealing[sphereIndex]) return;

        // Calculate current energy
        float currentEnergy = calculateLocalEnergy(sphereIndex);

        // Generate proposed position (small random perturbation)
        float oldX = lattice.posX[sphereIndex];
        float oldY = lattice.posY[sphereIndex];

        float deltaX = (random.nextFloat() - 0.5f) * 2.0f * MAX_POSITION_DELTA;
        float deltaY = (random.nextFloat() - 0.5f) * 2.0f * MAX_POSITION_DELTA;

        lattice.posX[sphereIndex] = oldX + deltaX;
        lattice.posY[sphereIndex] = oldY + deltaY;

        // Calculate proposed energy
        float proposedEnergy = calculateLocalEnergy(sphereIndex);

        // Metropolis criterion
        float deltaE = proposedEnergy - currentEnergy;
        float temperature = lattice.annealingTemperature[sphereIndex];

        boolean accept = false;
        if (deltaE < 0) {
            // Always accept energy decrease
            accept = true;
        } else if (temperature > 0) {
            // Stochastically accept energy increase
            float probability = (float) Math.exp(-deltaE / temperature);
            accept = random.nextFloat() < probability;
        }

        if (accept) {
            // Keep new position
            lattice.structuralEnergy[sphereIndex] = proposedEnergy;
            // Update stability if energy decreased
            if (deltaE < 0) {
                lattice.stabilityHistory[sphereIndex]++;
            } else {
                lattice.stabilityHistory[sphereIndex] = 0;
            }
        } else {
            // Reject: restore old position
            lattice.posX[sphereIndex] = oldX;
            lattice.posY[sphereIndex] = oldY;
        }

        // Cool down over time
        lattice.annealingTemperature[sphereIndex] *= PlanckLattice.COOLING_RATE;
    }

    /**
     * Perform annealing step for all active annealing regions.
     */
    public void performAnnealingStep() {
        // Attempt packing changes for all annealing spheres
        for (int i = 0; i < lattice.totalSpheres; i++) {
            if (lattice.isAnnealing[i]) {
                attemptPackingChange(i);
            }
        }
    }

    /**
     * Check if neighbor lists need updating (positions changed significantly).
     */
    public boolean needsNeighborUpdate() {
        // Simple heuristic: update every N annealing steps or if any sphere moved significantly
        // For now, we'll update periodically in the simulation engine
        return false;
    }

    /**
     * Get count of spheres currently annealing.
     */
    public int getAnnealingCount() {
        int count = 0;
        for (boolean annealing : lattice.isAnnealing) {
            if (annealing) count++;
        }
        return count;
    }

    /**
     * Get average annealing temperature across all spheres.
     */
    public float getAverageTemperature() {
        float sum = 0.0f;
        int count = 0;
        for (int i = 0; i < lattice.totalSpheres; i++) {
            if (lattice.isAnnealing[i]) {
                sum += lattice.annealingTemperature[i];
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0f;
    }
}
