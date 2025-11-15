package com.plancklattice;

import java.io.IOException;

/**
 * Main simulation engine that orchestrates the physics calculations.
 * Runs the simulation loop and coordinates all components.
 */
public class SimulationEngine {

    private final PlanckLattice lattice;
    private final VectorForces forces;
    private final Integrator integrator;
    private final EMPropagator emPropagator;
    private final Visualizer visualizer;

    private int currentTimestep;
    private float simulationTime;

    // Simulation parameters
    private float dt = 0.01f;                   // Timestep
    private float velocityDamping = 0.001f;     // Small damping for stability
    private int outputInterval = 100;            // Output every N timesteps
    private boolean useVectorizedEM = true;     // Use vectorized EM propagation

    public SimulationEngine(PlanckLattice lattice) {
        this.lattice = lattice;
        this.forces = new VectorForces(lattice);
        this.integrator = new Integrator(lattice);
        this.emPropagator = new EMPropagator(lattice);
        this.visualizer = new Visualizer(lattice);

        this.currentTimestep = 0;
        this.simulationTime = 0.0f;
    }

    /**
     * Run a single simulation timestep.
     */
    public void step() {
        // 1. Clear force accumulators
        lattice.clearForces();

        // 2. Calculate all forces
        forces.calculateSpacingForces();
        forces.calculateGravityForces();

        // Optional: Normalize forces to prevent instability
        // forces.normalizeForces(10.0f);

        // 3. Update positions and velocities
        integrator.integrate(dt);

        // 4. Apply damping for stability
        if (velocityDamping > 0.0f) {
            integrator.applyDamping(velocityDamping);
        }

        // 5. Propagate EM waves
        if (useVectorizedEM) {
            emPropagator.propagateVectorized(dt);
        } else {
            emPropagator.propagate(dt);
        }

        // 6. Update time
        currentTimestep++;
        simulationTime += dt;
    }

    /**
     * Run the simulation for a specified number of timesteps.
     */
    public void run(int totalTimesteps) {
        System.out.println("Starting Planck Lattice Simulation");
        System.out.println("Grid size: " + lattice.gridWidth + "x" + lattice.gridHeight);
        System.out.println("Total spheres: " + lattice.totalSpheres);
        System.out.println("Vector species: " + PlanckLattice.SPECIES);
        System.out.println("SIMD lanes: " + PlanckLattice.SPECIES.length());
        System.out.println("Timestep dt: " + dt);
        System.out.println("Total timesteps: " + totalTimesteps);
        System.out.println();

        long startTime = System.nanoTime();
        long lastOutputTime = startTime;

        for (int t = 0; t < totalTimesteps; t++) {
            step();

            // Output statistics and images periodically
            if (t % outputInterval == 0 || t == totalTimesteps - 1) {
                long currentTime = System.nanoTime();
                double elapsedMs = (currentTime - lastOutputTime) / 1_000_000.0;
                lastOutputTime = currentTime;

                visualizer.printStatistics(currentTimestep, elapsedMs);

                // Save images
                try {
                    String prefix = String.format("output/frame_%05d", currentTimestep);
                    visualizer.generateSpacingHeatmap(prefix + "_spacing.ppm");
                    visualizer.generateEMFieldImage(prefix + "_em.ppm");
                    visualizer.generateEnergyDensityImage(prefix + "_energy.ppm");

                    // Append to CSV
                    visualizer.appendStatisticsToCSV("output/statistics.csv", currentTimestep, elapsedMs);
                } catch (IOException e) {
                    System.err.println("Error writing output: " + e.getMessage());
                }
            }

            // Progress indicator
            if (t % (totalTimesteps / 10) == 0 && t > 0) {
                double percentComplete = (t * 100.0) / totalTimesteps;
                long currentTime = System.nanoTime();
                double totalElapsedSec = (currentTime - startTime) / 1_000_000_000.0;
                System.out.printf("Progress: %.0f%% (%.2f seconds elapsed)%n", percentComplete, totalElapsedSec);
            }
        }

        long endTime = System.nanoTime();
        double totalSeconds = (endTime - startTime) / 1_000_000_000.0;
        double stepsPerSecond = totalTimesteps / totalSeconds;

        System.out.println("\n=== Simulation Complete ===");
        System.out.printf("Total time: %.2f seconds%n", totalSeconds);
        System.out.printf("Performance: %.1f timesteps/second%n", stepsPerSecond);
        System.out.printf("Final simulation time: %.4f%n", simulationTime);
    }

    /**
     * Run with performance benchmarking (compare vectorized vs scalar).
     */
    public void benchmark(int warmupSteps, int benchmarkSteps) {
        System.out.println("=== Performance Benchmark ===");
        System.out.println("Grid size: " + lattice.gridWidth + "x" + lattice.gridHeight);
        System.out.println("Vector species: " + PlanckLattice.SPECIES);
        System.out.println("SIMD lanes: " + PlanckLattice.SPECIES.length());
        System.out.println();

        // Warmup
        System.out.println("Warming up (" + warmupSteps + " steps)...");
        for (int i = 0; i < warmupSteps; i++) {
            step();
        }

        // Benchmark vectorized version
        System.out.println("Benchmarking vectorized version (" + benchmarkSteps + " steps)...");
        useVectorizedEM = true;
        long startVec = System.nanoTime();
        for (int i = 0; i < benchmarkSteps; i++) {
            step();
        }
        long endVec = System.nanoTime();
        double vecTime = (endVec - startVec) / 1_000_000_000.0;

        // Reset simulation
        resetSimulation();

        // Benchmark scalar version
        System.out.println("Benchmarking scalar version (" + benchmarkSteps + " steps)...");
        useVectorizedEM = false;
        long startScalar = System.nanoTime();
        for (int i = 0; i < benchmarkSteps; i++) {
            step();
        }
        long endScalar = System.nanoTime();
        double scalarTime = (endScalar - startScalar) / 1_000_000_000.0;

        // Results
        System.out.println("\n=== Benchmark Results ===");
        System.out.printf("Vectorized: %.3f seconds (%.1f steps/sec)%n",
                          vecTime, benchmarkSteps / vecTime);
        System.out.printf("Scalar: %.3f seconds (%.1f steps/sec)%n",
                          scalarTime, benchmarkSteps / scalarTime);
        System.out.printf("Speedup: %.2fx%n", scalarTime / vecTime);
    }

    /**
     * Reset simulation to initial state.
     */
    private void resetSimulation() {
        currentTimestep = 0;
        simulationTime = 0.0f;

        // Clear all arrays
        for (int i = 0; i < lattice.totalSpheres; i++) {
            lattice.velX[i] = 0.0f;
            lattice.velY[i] = 0.0f;
            lattice.forceX[i] = 0.0f;
            lattice.forceY[i] = 0.0f;
            lattice.emFieldReal[i] = 0.0f;
            lattice.emFieldImag[i] = 0.0f;
        }

        // Reinitialize positions
        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);
                lattice.posX[index] = x * PlanckLattice.EQUILIBRIUM_DISTANCE;
                lattice.posY[index] = y * PlanckLattice.EQUILIBRIUM_DISTANCE;
            }
        }
    }

    // Getters and setters
    public void setTimestep(float dt) {
        this.dt = dt;
    }

    public void setVelocityDamping(float damping) {
        this.velocityDamping = damping;
    }

    public void setOutputInterval(int interval) {
        this.outputInterval = interval;
    }

    public int getCurrentTimestep() {
        return currentTimestep;
    }

    public float getSimulationTime() {
        return simulationTime;
    }
}
