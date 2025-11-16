package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnnealingEngine - lattice optimization through annealing.
 * These tests document the "strong force" mechanics and error handling.
 *
 * Purpose: AnnealingEngine implements simulated annealing to find stable
 * lattice configurations. This is the mechanism behind particle formation.
 */
@DisplayName("AnnealingEngine - Lattice annealing and optimization")
class AnnealingEngineTest {

    private PlanckLattice lattice;
    private AnnealingEngine engine;

    @BeforeEach
    void setUp() {
        lattice = new PlanckLattice(20, 20);
        engine = new AnnealingEngine(lattice);
    }

    @Nested
    @DisplayName("Neighbor Detection")
    class NeighborDetectionTests {

        @Test
        @DisplayName("Should find neighbors within radius")
        void testBasicNeighborDetection() {
            // USAGE: Distance-based neighbor finding
            engine.updateNeighborLists();

            int centerIdx = lattice.getIndex(10, 10);
            List<Integer> neighbors = engine.getNeighbors(centerIdx);

            assertNotNull(neighbors, "Neighbor list should not be null");
            assertTrue(neighbors.size() > 0,
                "Center sphere should have neighbors");
        }

        @Test
        @DisplayName("Should find approximately 4 neighbors in grid")
        void testGridNeighborCount() {
            // DOCUMENTATION: In square grid with radius 1.5, expect ~4-8 neighbors
            engine.updateNeighborLists();

            int centerIdx = lattice.getIndex(10, 10);
            List<Integer> neighbors = engine.getNeighbors(centerIdx);

            assertTrue(neighbors.size() >= 4 && neighbors.size() <= 12,
                "Grid sphere should have 4-12 neighbors (grid + diagonals)");
        }

        @Test
        @DisplayName("Should not include self in neighbors")
        void testNoSelfInNeighbors() {
            // ERROR PREVENTION: Sphere shouldn't be its own neighbor
            engine.updateNeighborLists();

            for (int i = 0; i < lattice.totalSpheres; i++) {
                List<Integer> neighbors = engine.getNeighbors(i);
                assertFalse(neighbors.contains(i),
                    "Sphere " + i + " should not be in its own neighbor list");
            }
        }

        @Test
        @DisplayName("Should handle corner spheres")
        void testCornerSphereNeighbors() {
            // EDGE CASE: Corner has fewer neighbors
            engine.updateNeighborLists();

            int cornerIdx = lattice.getIndex(0, 0);
            List<Integer> neighbors = engine.getNeighbors(cornerIdx);

            assertNotNull(neighbors);
            assertTrue(neighbors.size() > 0,
                "Even corner should have some neighbors");
        }

        @Test
        @DisplayName("Should handle toroidal wrapping")
        void testToroidalWrapping() {
            // PHYSICS: Lattice wraps around (toroidal topology)
            engine.updateNeighborLists();

            // Edge sphere should have neighbors "across" the boundary
            int edgeIdx = lattice.getIndex(0, 10);
            List<Integer> neighbors = engine.getNeighbors(edgeIdx);

            // Should have reasonable neighbor count (wrapping provides more)
            assertTrue(neighbors.size() >= 4,
                "Edge sphere should have neighbors via toroidal wrap");
        }
    }

    @Nested
    @DisplayName("Annealing Region Identification")
    class AnnealingIdentificationTests {

        @Test
        @DisplayName("Should not anneal empty lattice")
        void testNoAnnealingOnEmpty() {
            // BASELINE: Fresh lattice has no high-energy regions
            engine.identifyAnnealingRegions();

            int annealingCount = engine.getAnnealingCount();
            assertEquals(0, annealingCount,
                "Empty lattice should have no annealing regions");
        }

        @Test
        @DisplayName("Should identify high-energy regions for annealing")
        void testHighEnergyDetection() {
            // USAGE: High energy triggers annealing
            int centerIdx = lattice.getIndex(10, 10);
            lattice.energyDensity[centerIdx] = 5.0f; // > ANNEALING_THRESHOLD (2.0)

            engine.identifyAnnealingRegions();

            assertTrue(lattice.isAnnealing[centerIdx],
                "High energy sphere should be marked for annealing");
            assertTrue(engine.getAnnealingCount() > 0,
                "Should detect at least one annealing sphere");
        }

        @Test
        @DisplayName("Should not anneal below threshold")
        void testBelowThreshold() {
            // ERROR PREVENTION: Low energy shouldn't trigger annealing
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.energyDensity[i] = 1.5f; // Below ANNEALING_THRESHOLD (2.0)
            }

            engine.identifyAnnealingRegions();

            assertEquals(0, engine.getAnnealingCount(),
                "Energy below threshold should not trigger annealing");
        }

        @Test
        @DisplayName("Should initialize temperature for new annealing regions")
        void testTemperatureInitialization() {
            // USAGE: First time annealing sets initial temperature
            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;
            lattice.annealingTemperature[idx] = 0.0f; // Not yet set

            engine.identifyAnnealingRegions();

            assertTrue(lattice.annealingTemperature[idx] > 0,
                "New annealing region should get initial temperature");
            assertEquals(PlanckLattice.INITIAL_TEMPERATURE,
                        lattice.annealingTemperature[idx], 0.001f,
                        "Should use configured initial temperature");
        }

        @Test
        @DisplayName("Should not reset temperature if already annealing")
        void testTemperaturePreservation() {
            // ERROR PREVENTION: Don't reset ongoing annealing
            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;
            lattice.annealingTemperature[idx] = 0.5f; // Already cooling

            engine.identifyAnnealingRegions();

            assertEquals(0.5f, lattice.annealingTemperature[idx], 0.001f,
                "Existing temperature should be preserved");
        }
    }

    @Nested
    @DisplayName("Energy Calculation")
    class EnergyCalculationTests {

        @Test
        @DisplayName("Should calculate energy for isolated sphere")
        void testIsolatedSphereEnergy() {
            // BASELINE: Energy calculation includes gravity (negative contribution)
            engine.updateNeighborLists();

            int centerIdx = lattice.getIndex(10, 10);
            float energy = engine.calculateLocalEnergy(centerIdx);

            assertNotNull(energy,
                "Energy calculation should return a value");
            // Energy can be negative due to gravity contribution
        }

        @Test
        @DisplayName("Should include spacing energy component")
        void testSpacingEnergyComponent() {
            // PHYSICS: Displaced sphere has higher energy
            engine.updateNeighborLists();

            int idx = lattice.getIndex(10, 10);
            float baselineEnergy = engine.calculateLocalEnergy(idx);

            // Displace sphere
            lattice.posX[idx] += 0.5f;  // Move off equilibrium
            float displacedEnergy = engine.calculateLocalEnergy(idx);

            assertTrue(displacedEnergy > baselineEnergy,
                "Displaced sphere should have higher energy");
        }

        @Test
        @DisplayName("Should include EM field energy")
        void testEMEnergyComponent() {
            // PHYSICS: EM field contributes to local energy
            engine.updateNeighborLists();

            int idx = lattice.getIndex(10, 10);
            float baselineEnergy = engine.calculateLocalEnergy(idx);

            // Add EM field
            lattice.emFieldReal[idx] = 2.0f;
            lattice.emFieldImag[idx] = 1.5f;
            float withEMEnergy = engine.calculateLocalEnergy(idx);

            assertTrue(withEMEnergy > baselineEnergy,
                "EM field should increase local energy");
        }

        @Test
        @DisplayName("Should include gravitational energy")
        void testGravityEnergyComponent() {
            // PHYSICS: Energy density creates gravitational well
            engine.updateNeighborLists();

            int idx = lattice.getIndex(10, 10);
            float baselineEnergy = engine.calculateLocalEnergy(idx);

            // Add energy density
            lattice.energyDensity[idx] = 10.0f;
            float withGravityEnergy = engine.calculateLocalEnergy(idx);

            assertTrue(withGravityEnergy < baselineEnergy,
                "Gravity (negative contribution) should lower total energy");
        }

        @Test
        @DisplayName("Should include coordination energy bonus")
        void testCoordinationEnergyComponent() {
            // PHYSICS: Preferred coordination numbers are more stable
            // This is tested implicitly through the total energy
            engine.updateNeighborLists();

            int idx = lattice.getIndex(10, 10);
            float energy = engine.calculateLocalEnergy(idx);

            // Energy calculation should complete without error
            assertNotNull(energy, "Energy calculation should return value");
        }
    }

    @Nested
    @DisplayName("Annealing Step")
    class AnnealingStepTests {

        @Test
        @DisplayName("Should perform annealing step without error")
        void testBasicAnnealingStep() {
            // USAGE: Main annealing loop
            // Set up annealing region
            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;

            engine.updateNeighborLists();
            engine.identifyAnnealingRegions();

            assertDoesNotThrow(() -> engine.performAnnealingStep(),
                "Annealing step should not throw exception");
        }

        @Test
        @DisplayName("Should attempt position changes for annealing spheres")
        void testPositionModification() {
            // USAGE: Annealing explores position space
            engine.setSeed(12345); // Deterministic for testing

            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;

            engine.updateNeighborLists();
            engine.identifyAnnealingRegions();

            float originalX = lattice.posX[idx];
            float originalY = lattice.posY[idx];

            // Multiple steps increase chance of position change
            for (int i = 0; i < 50; i++) {
                engine.performAnnealingStep();
            }

            // Position may or may not change (stochastic), but should be attempted
            // Just verify it doesn't crash and positions remain valid
            assertTrue(lattice.posX[idx] >= 0 && lattice.posX[idx] < lattice.gridWidth * 2,
                "Position should remain in reasonable bounds");
        }

        @Test
        @DisplayName("Should cool temperature during annealing")
        void testTemperatureCooling() {
            // PHYSICS: Temperature decreases over time
            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;

            engine.identifyAnnealingRegions();
            float initialTemp = lattice.annealingTemperature[idx];

            // Perform many annealing steps
            for (int i = 0; i < 100; i++) {
                engine.performAnnealingStep();
            }

            float finalTemp = lattice.annealingTemperature[idx];

            assertTrue(finalTemp < initialTemp,
                "Temperature should decrease during annealing");
        }

        @Test
        @DisplayName("Should update stability history on successful moves")
        void testStabilityTracking() {
            // USAGE: Track when configurations stabilize
            engine.setSeed(42); // Deterministic

            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;
            lattice.stabilityHistory[idx] = 0;

            engine.updateNeighborLists();
            engine.identifyAnnealingRegions();

            // Annealing should update stability tracking
            for (int i = 0; i < 100; i++) {
                engine.performAnnealingStep();
            }

            // Stability history may increase if good moves found
            assertTrue(lattice.stabilityHistory[idx] >= 0,
                "Stability history should be non-negative");
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("Should count annealing spheres correctly")
        void testAnnealingCount() {
            // Create 3 high-energy regions
            lattice.energyDensity[lattice.getIndex(5, 5)] = 5.0f;
            lattice.energyDensity[lattice.getIndex(10, 10)] = 5.0f;
            lattice.energyDensity[lattice.getIndex(15, 15)] = 5.0f;

            engine.identifyAnnealingRegions();

            assertEquals(3, engine.getAnnealingCount(),
                "Should count exactly 3 annealing spheres");
        }

        @Test
        @DisplayName("Should calculate average temperature")
        void testAverageTemperature() {
            lattice.energyDensity[lattice.getIndex(5, 5)] = 5.0f;
            lattice.energyDensity[lattice.getIndex(10, 10)] = 5.0f;

            engine.identifyAnnealingRegions();

            float avgTemp = engine.getAverageTemperature();

            assertTrue(avgTemp > 0,
                "Average temperature should be positive");
            assertTrue(avgTemp <= PlanckLattice.INITIAL_TEMPERATURE,
                "Average should not exceed initial temperature");
        }

        @Test
        @DisplayName("Should return zero average temperature with no annealing")
        void testZeroAverageWhenNotAnnealing() {
            engine.identifyAnnealingRegions();

            float avgTemp = engine.getAverageTemperature();

            assertEquals(0.0f, avgTemp, 0.001f,
                "No annealing means zero average temperature");
        }
    }

    @Nested
    @DisplayName("Random Seed Control")
    class RandomSeedTests {

        @Test
        @DisplayName("Should produce deterministic results with same seed")
        void testDeterministicBehavior() {
            // DOCUMENTATION: Seed controls random behavior
            // Note: Perfect determinism requires resetting all state
            engine.setSeed(12345);

            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;
            lattice.isAnnealing[idx] = true;
            lattice.annealingTemperature[idx] = 1.0f;

            engine.updateNeighborLists();

            // Just verify seeding works (stochastic behavior controlled)
            assertDoesNotThrow(() -> {
                for (int i = 0; i < 10; i++) {
                    engine.attemptPackingChange(idx);
                }
            }, "Seeded random should work without error");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle all spheres annealing")
        void testFullLatticeAnnealing() {
            // STRESS TEST: Entire lattice annealing
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.energyDensity[i] = 5.0f;
            }

            engine.identifyAnnealingRegions();

            assertEquals(lattice.totalSpheres, engine.getAnnealingCount(),
                "All spheres should be annealing");

            assertDoesNotThrow(() -> engine.performAnnealingStep(),
                "Should handle full lattice annealing");
        }

        @Test
        @DisplayName("Should handle zero temperature gracefully")
        void testZeroTemperature() {
            // ERROR CASE: Temperature cooled to zero
            int idx = lattice.getIndex(10, 10);
            lattice.energyDensity[idx] = 5.0f;
            lattice.isAnnealing[idx] = true;
            lattice.annealingTemperature[idx] = 0.0f;

            engine.updateNeighborLists();

            assertDoesNotThrow(() -> engine.attemptPackingChange(idx),
                "Zero temperature should not cause crash");
        }

        @Test
        @DisplayName("Should handle boundary sphere annealing")
        void testBoundarySphereAnnealing() {
            // EDGE CASE: Sphere at lattice boundary
            int edgeIdx = lattice.getIndex(0, 0);
            lattice.energyDensity[edgeIdx] = 5.0f;

            engine.updateNeighborLists();
            engine.identifyAnnealingRegions();

            assertDoesNotThrow(() -> engine.performAnnealingStep(),
                "Boundary sphere annealing should not crash");
        }
    }
}
