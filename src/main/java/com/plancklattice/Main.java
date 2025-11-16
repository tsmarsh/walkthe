package com.plancklattice;

import java.io.File;

/**
 * Main entry point for the Planck Lattice simulation.
 * Provides different scenarios to explore emergent spacetime physics.
 */
public class Main {

    public static void main(String[] args) {
        // Create output directory
        new File("output").mkdirs();

        // Parse command line arguments
        String scenario = "em-wave";
        int gridSize = 200;
        int timesteps = 1000;

        if (args.length > 0) {
            scenario = args[0];
        }
        if (args.length > 1) {
            gridSize = Integer.parseInt(args[1]);
        }
        if (args.length > 2) {
            timesteps = Integer.parseInt(args[2]);
        }

        System.out.println("Planck Lattice Simulation");
        System.out.println("========================");
        System.out.println("Scenario: " + scenario);
        System.out.println();

        switch (scenario.toLowerCase()) {
            case "em-wave":
                runEMWaveScenario(gridSize, timesteps);
                break;
            case "gravity":
                runGravityScenario(gridSize, timesteps);
                break;
            case "combined":
                runCombinedScenario(gridSize, timesteps);
                break;
            case "particle-formation":
                runParticleFormationScenario(gridSize, timesteps);
                break;
            case "particle-collision":
                runParticleCollisionScenario(gridSize, timesteps);
                break;
            case "benchmark":
                runBenchmark(gridSize);
                break;
            default:
                printUsage();
                break;
        }
    }

    /**
     * Scenario 1: EM Wave Propagation
     * Demonstrates light propagating through flat spacetime at inherent speed c.
     */
    private static void runEMWaveScenario(int gridSize, int timesteps) {
        System.out.println("Scenario: EM Wave Propagation");
        System.out.println("A Gaussian wave packet propagates through flat spacetime.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Add EM pulse in center
        float centerX = gridSize / 2.0f;
        float centerY = gridSize / 2.0f;
        float amplitude = 1.0f;
        float sigma = 10.0f;        // Spatial width of pulse
        float wavelength = 5.0f;    // Wavelength

        lattice.addEMPulse(centerX, centerY, amplitude, sigma, wavelength);

        // Run simulation
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(50);  // Output every 50 timesteps
        engine.run(timesteps);

        System.out.println("\nOutput images saved to output/ directory");
        System.out.println("View *_em.ppm files to see wave propagation");
    }

    /**
     * Scenario 2: Gravitational Compression
     * Demonstrates how mass compresses local spacetime geometry.
     */
    private static void runGravityScenario(int gridSize, int timesteps) {
        System.out.println("Scenario: Gravitational Compression");
        System.out.println("Mass concentration compresses the local lattice geometry.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Add mass concentration in center
        float centerX = gridSize / 2.0f;
        float centerY = gridSize / 2.0f;
        float mass = 100.0f;
        float radius = 15.0f;

        lattice.addMassConcentration(centerX, centerY, mass, radius);

        // Run simulation
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(50);
        engine.setVelocityDamping(0.01f);  // Higher damping to reach equilibrium faster
        engine.run(timesteps);

        System.out.println("\nOutput images saved to output/ directory");
        System.out.println("View *_spacing.ppm files to see lattice compression");
        System.out.println("Blue = compressed spacetime near mass");
    }

    /**
     * Scenario 3: Combined EM + Gravity
     * Demonstrates light propagating through curved spacetime.
     * Light should travel slower through compressed regions (gravitational lensing effect).
     */
    private static void runCombinedScenario(int gridSize, int timesteps) {
        System.out.println("Scenario: EM Wave in Curved Spacetime");
        System.out.println("Light propagates through gravitationally compressed spacetime.");
        System.out.println("Expect: slower propagation through compressed regions.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Add mass concentration to one side
        float massX = gridSize * 0.7f;
        float massY = gridSize / 2.0f;
        float mass = 150.0f;
        float massRadius = 20.0f;

        lattice.addMassConcentration(massX, massY, mass, massRadius);

        // Let lattice compress first (pre-run)
        System.out.println("Pre-simulation: letting lattice compress...");
        SimulationEngine preEngine = new SimulationEngine(lattice);
        preEngine.setOutputInterval(Integer.MAX_VALUE); // No output during pre-run
        preEngine.setVelocityDamping(0.02f);
        preEngine.run(500); // 500 steps to reach quasi-equilibrium

        System.out.println("Adding EM pulse...\n");

        // Now add EM pulse on opposite side
        float pulseX = gridSize * 0.3f;
        float pulseY = gridSize / 2.0f;
        float amplitude = 1.0f;
        float sigma = 10.0f;
        float wavelength = 5.0f;

        lattice.addEMPulse(pulseX, pulseY, amplitude, sigma, wavelength);

        // Run main simulation
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(50);
        engine.setVelocityDamping(0.005f);  // Lower damping now
        engine.run(timesteps);

        System.out.println("\nOutput images saved to output/ directory");
        System.out.println("View combined *_em.ppm and *_spacing.ppm files");
        System.out.println("Wave should propagate slower through blue (compressed) regions");
    }

    /**
     * Scenario 4: Particle Formation via Annealing
     * Demonstrates particle emergence from high-energy regions through lattice annealing.
     */
    private static void runParticleFormationScenario(int gridSize, int timesteps) {
        System.out.println("Scenario: Particle Formation via Annealing");
        System.out.println("High-energy EM pulse causes lattice to anneal into stable particle pattern.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Add high-energy EM pulse in center to trigger particle formation
        float centerX = gridSize / 2.0f;
        float centerY = gridSize / 2.0f;
        float amplitude = 5.0f;     // High amplitude to trigger annealing
        float sigma = 8.0f;         // Compact pulse
        float wavelength = 3.0f;

        lattice.addEMPulse(centerX, centerY, amplitude, sigma, wavelength);

        // Also add some energy density to help stabilize
        lattice.addMassConcentration(centerX, centerY, 5.0f, 10.0f);

        // Run simulation
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(25);  // Frequent output to see particle form
        engine.setVelocityDamping(0.005f);
        engine.run(timesteps);

        System.out.println("\nOutput images saved to output/ directory");
        System.out.println("View *_particles.ppm to see formed particles (colored regions)");
        System.out.println("View *_annealing.ppm to see annealing activity (red = active)");
        System.out.println("View *_stability.ppm to see stable regions (green = stable)");
    }

    /**
     * Scenario 5: Particle Collision
     * Two high-energy regions form particles that move toward each other and interact.
     */
    private static void runParticleCollisionScenario(int gridSize, int timesteps) {
        System.out.println("Scenario: Particle Collision");
        System.out.println("Two particles form and collide, demonstrating particle interactions.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Create two high-energy regions on opposite sides
        float leftX = gridSize * 0.3f;
        float rightX = gridSize * 0.7f;
        float centerY = gridSize / 2.0f;

        float amplitude = 4.0f;
        float sigma = 8.0f;
        float wavelength = 3.0f;

        // Left particle
        lattice.addEMPulse(leftX, centerY, amplitude, sigma, wavelength);
        lattice.addMassConcentration(leftX, centerY, 5.0f, 10.0f);

        // Right particle
        lattice.addEMPulse(rightX, centerY, amplitude, sigma, wavelength);
        lattice.addMassConcentration(rightX, centerY, 5.0f, 10.0f);

        // Add initial velocities (push them toward each other)
        for (int i = 0; i < lattice.totalSpheres; i++) {
            float x = lattice.posX[i];
            float y = lattice.posY[i];

            // Left region: push right
            float dxLeft = x - leftX;
            float dyLeft = y - centerY;
            float distLeftSq = dxLeft * dxLeft + dyLeft * dyLeft;
            if (distLeftSq < sigma * sigma) {
                lattice.velX[i] = 0.2f;
            }

            // Right region: push left
            float dxRight = x - rightX;
            float dyRight = y - centerY;
            float distRightSq = dxRight * dxRight + dyRight * dyRight;
            if (distRightSq < sigma * sigma) {
                lattice.velX[i] = -0.2f;
            }
        }

        // Run simulation
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(25);
        engine.setVelocityDamping(0.002f);  // Lower damping to allow movement
        engine.run(timesteps);

        System.out.println("\nOutput images saved to output/ directory");
        System.out.println("View *_particles.ppm to see particle collision");
        System.out.println("Particles should merge, bounce, or annihilate depending on parameters");
    }

    /**
     * Scenario 6: Performance Benchmark
     * Compare vectorized vs scalar performance.
     */
    private static void runBenchmark(int gridSize) {
        System.out.println("Scenario: Performance Benchmark");
        System.out.println("Comparing vectorized vs scalar implementations.");
        System.out.println();

        // Create lattice
        PlanckLattice lattice = new PlanckLattice(gridSize, gridSize);

        // Add some EM activity for realistic workload
        lattice.addEMPulse(gridSize / 2.0f, gridSize / 2.0f, 1.0f, 10.0f, 5.0f);
        lattice.addMassConcentration(gridSize / 2.0f, gridSize / 2.0f, 50.0f, 15.0f);

        // Run benchmark
        SimulationEngine engine = new SimulationEngine(lattice);
        engine.setOutputInterval(Integer.MAX_VALUE); // No output during benchmark
        engine.benchmark(100, 1000);  // 100 warmup, 1000 benchmark steps
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: java --add-modules jdk.incubator.vector com.plancklattice.Main [scenario] [gridSize] [timesteps]");
        System.out.println();
        System.out.println("Scenarios:");
        System.out.println("  em-wave             - EM wave propagation through flat spacetime (default)");
        System.out.println("  gravity             - Gravitational compression of lattice");
        System.out.println("  combined            - EM wave through curved spacetime");
        System.out.println("  particle-formation  - Particle emergence via annealing (Phase 2)");
        System.out.println("  particle-collision  - Two particles collide (Phase 2)");
        System.out.println("  benchmark           - Performance benchmark (vectorized vs scalar)");
        System.out.println();
        System.out.println("Parameters:");
        System.out.println("  gridSize   - Lattice size (default: 200)");
        System.out.println("  timesteps  - Number of simulation steps (default: 1000)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java --add-modules jdk.incubator.vector com.plancklattice.Main em-wave 300 2000");
        System.out.println("  java --add-modules jdk.incubator.vector com.plancklattice.Main gravity 200 1500");
        System.out.println("  java --add-modules jdk.incubator.vector com.plancklattice.Main benchmark 400");
        System.out.println();
        System.out.println("Or using Maven:");
        System.out.println("  mvn exec:java -Dexec.args=\"em-wave 300 2000\"");
    }
}
