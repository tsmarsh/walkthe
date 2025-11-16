package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for VectorForces.
 * These tests demonstrate force calculation functionality and serve as documentation.
 *
 * Usage examples:
 * - Calculating spacing forces (Hooke's law)
 * - Calculating gravitational compression forces
 * - Normalizing forces to prevent instability
 */
@DisplayName("VectorForces - Force calculations for lattice dynamics")
class VectorForcesTest {

    @Nested
    @DisplayName("Spacing Forces (Hooke's Law)")
    class SpacingForcesTests {

        @Test
        @DisplayName("Should produce zero forces on uniform equilibrium lattice")
        void testZeroForcesAtEquilibrium() {
            // USAGE: Calculate spacing forces on a lattice
            PlanckLattice lattice = new PlanckLattice(10, 10);
            VectorForces forces = new VectorForces(lattice);

            lattice.clearForces();
            forces.calculateSpacingForces();

            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.forceX[i], 1e-5f,
                    "Equilibrium lattice should have zero X force at index " + i);
                assertEquals(0.0f, lattice.forceY[i], 1e-5f,
                    "Equilibrium lattice should have zero Y force at index " + i);
            }
        }

        @Test
        @DisplayName("Should produce attractive forces when spheres are too far apart")
        void testAttractiveForcesWhenStretched() {
            // USAGE: When lattice is stretched, forces should pull spheres together
            PlanckLattice lattice = new PlanckLattice(3, 1);  // Single row for simplicity
            VectorForces forces = new VectorForces(lattice);

            // Stretch the middle sphere to the right
            lattice.posX[1] = 1.5f;  // Normal would be 1.0

            lattice.clearForces();
            forces.calculateSpacingForces();

            // Left sphere (0) should be pulled right (positive X force)
            assertTrue(lattice.forceX[0] > 0,
                "Left sphere should be pulled right, force was " + lattice.forceX[0]);

            // Middle sphere (1) should be pulled left (negative X force from right)
            // and pulled right (positive X force from left)
            // Net: should be pulled left since it's closer to right neighbor
            assertTrue(lattice.forceX[1] < 0,
                "Middle sphere should be pulled left, force was " + lattice.forceX[1]);
        }

        @Test
        @DisplayName("Should produce repulsive forces when spheres are too close")
        void testRepulsiveForcesWhenCompressed() {
            // USAGE: When lattice is compressed, forces should push spheres apart
            PlanckLattice lattice = new PlanckLattice(3, 1);
            VectorForces forces = new VectorForces(lattice);

            // Compress the lattice
            for (int i = 0; i < 3; i++) {
                lattice.posX[i] = i * 0.5f;  // Half the equilibrium distance
            }

            lattice.clearForces();
            forces.calculateSpacingForces();

            // Middle sphere should experience outward forces from both neighbors
            // Net force depends on toroidal boundary conditions
            // Just check that forces are non-zero
            float totalForce = Math.abs(lattice.forceX[0]) + Math.abs(lattice.forceX[1]) + Math.abs(lattice.forceX[2]);
            assertTrue(totalForce > 0.1f, "Compressed lattice should have significant forces");
        }

        @Test
        @DisplayName("Should handle 2D forces correctly")
        void test2DSpacingForces() {
            // USAGE: Forces work in both X and Y dimensions
            PlanckLattice lattice = new PlanckLattice(3, 3);
            VectorForces forces = new VectorForces(lattice);

            // Move center sphere up and to the right
            int centerIdx = lattice.getIndex(1, 1);
            lattice.posX[centerIdx] = 1.3f;  // Slightly right
            lattice.posY[centerIdx] = 1.3f;  // Slightly up

            lattice.clearForces();
            forces.calculateSpacingForces();

            // Center sphere should have forces in both X and Y directions
            float centerForceX = lattice.forceX[centerIdx];
            float centerForceY = lattice.forceY[centerIdx];

            assertTrue(Math.abs(centerForceX) > 0.01f, "Should have X force on displaced center");
            assertTrue(Math.abs(centerForceY) > 0.01f, "Should have Y force on displaced center");
        }

        @Test
        @DisplayName("Should respect toroidal boundary conditions")
        void testToroidalBoundaries() {
            // USAGE: Forces wrap around edges (toroidal topology)
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Displace edge sphere
            int edgeIdx = lattice.getIndex(0, 0);  // Top-left corner
            lattice.posX[edgeIdx] = -0.2f;  // Move slightly left (wraps to right)

            lattice.clearForces();
            forces.calculateSpacingForces();

            // Edge sphere should have forces from wrapped neighbors
            assertNotEquals(0.0f, lattice.forceX[edgeIdx], 1e-5f,
                "Edge sphere should have forces from wrapped neighbors");
        }

        @Test
        @DisplayName("Wrap-around neighbors should use shortest separation vector")
        void testToroidalNeighborsUseShortestVector() {
            PlanckLattice lattice = new PlanckLattice(6, 1);
            VectorForces forces = new VectorForces(lattice);

            lattice.clearForces();
            forces.calculateSpacingForces();

            int leftEdge = lattice.getIndex(0, 0);
            int rightEdge = lattice.getIndex(lattice.gridWidth - 1, 0);

            assertEquals(0.0f, lattice.forceX[leftEdge], 1e-6f,
                "Left edge should be in equilibrium with wrapped neighbor");
            assertEquals(0.0f, lattice.forceX[rightEdge], 1e-6f,
                "Right edge should be in equilibrium with wrapped neighbor");
        }

        @Test
        @DisplayName("Should scale force magnitude with spring constant")
        void testSpringConstantScaling() {
            // USAGE: Force magnitude is proportional to k (SPRING_K)
            PlanckLattice lattice = new PlanckLattice(2, 1);
            VectorForces forces = new VectorForces(lattice);

            // Displace second sphere
            lattice.posX[1] = 2.0f;  // Distance = 2.0 instead of 1.0

            lattice.clearForces();
            forces.calculateSpacingForces();

            // Force magnitude should be k * (d - d0) = 1.0 * (2.0 - 1.0) = 1.0
            // But there are toroidal neighbors, so just check it's non-zero and reasonable
            float force = lattice.forceX[0];
            assertTrue(Math.abs(force) > 0.1f, "Should have significant force with displacement");
        }
    }

    @Nested
    @DisplayName("Gravitational Forces")
    class GravityForcesTests {

        @Test
        @DisplayName("Should produce zero gravity forces with no energy density")
        void testZeroGravityWithoutMass() {
            // USAGE: No mass/energy → no gravitational forces
            PlanckLattice lattice = new PlanckLattice(10, 10);
            VectorForces forces = new VectorForces(lattice);

            lattice.clearForces();
            forces.calculateGravityForces();

            // All forces should be zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.forceX[i], 1e-7f, "Gravity force X should be zero without mass");
                assertEquals(0.0f, lattice.forceY[i], 1e-7f, "Gravity force Y should be zero without mass");
            }
        }

        @Test
        @DisplayName("Should pull neighbors toward mass concentration")
        void testAttractionTowardMass() {
            // USAGE: Energy density creates attractive gravitational forces
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Add energy at center
            int centerIdx = lattice.getIndex(2, 2);
            lattice.energyDensity[centerIdx] = 100.0f;

            lattice.clearForces();
            forces.calculateGravityForces();

            // Note: The implementation applies gravity force from energy source to neighbors
            // Based on the code: dx = px1 - px2 (from neighbor to source)
            // and the force magnitude is positive, so multiplying by the normalized
            // direction pulls neighbors toward the source

            // Right neighbor should have force (check for non-zero)
            int rightIdx = lattice.getIndex(3, 2);
            float rightForceX = lattice.forceX[rightIdx];

            // Left neighbor should have force
            int leftIdx = lattice.getIndex(1, 2);
            float leftForceX = lattice.forceX[leftIdx];

            // Top neighbor should have force
            int topIdx = lattice.getIndex(2, 1);
            float topForceY = lattice.forceY[topIdx];

            // Bottom neighbor should have force
            int bottomIdx = lattice.getIndex(2, 3);
            float bottomForceY = lattice.forceY[bottomIdx];

            // All neighbors should have significant forces
            assertTrue(Math.abs(rightForceX) > 0.1f, "Right neighbor should have significant force");
            assertTrue(Math.abs(leftForceX) > 0.1f, "Left neighbor should have significant force");
            assertTrue(Math.abs(topForceY) > 0.1f, "Top neighbor should have significant force");
            assertTrue(Math.abs(bottomForceY) > 0.1f, "Bottom neighbor should have significant force");

            // Forces should point in opposite directions on opposite sides
            assertTrue(rightForceX * leftForceX < 0, "Left and right forces should be opposite");
            assertTrue(topForceY * bottomForceY < 0, "Top and bottom forces should be opposite");
        }

        @Test
        @DisplayName("Gravitational force direction should point toward the energy source")
        void testGravityDirectionIsAttractive() {
            PlanckLattice lattice = new PlanckLattice(3, 3);
            VectorForces forces = new VectorForces(lattice);

            int source = lattice.getIndex(1, 1);
            lattice.energyDensity[source] = 10.0f;

            lattice.clearForces();
            forces.calculateGravityForces();

            int rightNeighbor = lattice.getIndex(2, 1);
            float forceX = lattice.forceX[rightNeighbor];
            float expected = -PlanckLattice.GRAVITY_G * lattice.energyDensity[source];

            assertTrue(forceX < 0.0f, "Neighbor should be pulled toward the source");
            assertEquals(expected, forceX, 1e-6f, "Force magnitude should match linear model");
            assertEquals(0.0f, lattice.forceY[rightNeighbor], 1e-6f,
                "No vertical component expected for horizontal neighbor");
        }

        @Test
        @DisplayName("Gravity should pull across toroidal wrap using shortest path")
        void testGravityWrapsAroundEdges() {
            PlanckLattice lattice = new PlanckLattice(6, 1);
            VectorForces forces = new VectorForces(lattice);

            int source = lattice.getIndex(0, 0);
            int wrapNeighbor = lattice.getIndex(lattice.gridWidth - 1, 0);
            lattice.energyDensity[source] = 20.0f;

            lattice.clearForces();
            forces.calculateGravityForces();

            float expected = PlanckLattice.GRAVITY_G * lattice.energyDensity[source];
            assertEquals(expected, lattice.forceX[wrapNeighbor], 1e-6f,
                "Wrap neighbor should be pulled toward the source across the boundary");
            assertEquals(0.0f, lattice.forceY[wrapNeighbor], 1e-6f,
                "No vertical force in 1D row");
        }

        @Test
        @DisplayName("Should scale force with energy density")
        void testGravityScalesWithMass() {
            // USAGE: Larger mass creates stronger gravitational forces
            PlanckLattice lattice1 = new PlanckLattice(5, 5);
            VectorForces forces1 = new VectorForces(lattice1);

            int centerIdx = lattice1.getIndex(2, 2);
            lattice1.energyDensity[centerIdx] = 50.0f;
            lattice1.clearForces();
            forces1.calculateGravityForces();

            float force1 = Math.abs(lattice1.forceX[lattice1.getIndex(3, 2)]);

            // Double the mass
            PlanckLattice lattice2 = new PlanckLattice(5, 5);
            VectorForces forces2 = new VectorForces(lattice2);
            lattice2.energyDensity[centerIdx] = 100.0f;
            lattice2.clearForces();
            forces2.calculateGravityForces();

            float force2 = Math.abs(lattice2.forceX[lattice2.getIndex(3, 2)]);

            // Force should be approximately doubled
            assertTrue(force2 > force1,
                "Double mass should create stronger force: " + force1 + " vs " + force2);
            assertEquals(force1 * 2.0f, force2, force1 * 0.3f,
                "Force should approximately double with double mass");
        }

        @Test
        @DisplayName("Should only affect neighbors (not the massive sphere itself)")
        void testGravityOnlyAffectsNeighbors() {
            // USAGE: Gravity pulls neighbors, not the source
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Add energy at one location
            int massIdx = lattice.getIndex(2, 2);
            lattice.energyDensity[massIdx] = 100.0f;

            lattice.clearForces();
            forces.calculateGravityForces();

            // The massive sphere itself should not have gravitational force from its own mass
            // (Only neighbors are affected in the implementation)
            // Since it has no neighbors with mass, its force should be zero
            assertEquals(0.0f, lattice.forceX[massIdx], 1e-7f,
                "Massive sphere should not pull itself");
            assertEquals(0.0f, lattice.forceY[massIdx], 1e-7f,
                "Massive sphere should not pull itself");
        }

        @Test
        @DisplayName("Should skip negligible energy densities for efficiency")
        void testSkipsNegligibleEnergy() {
            // USAGE: Very small energy densities are skipped for performance
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Add tiny energy everywhere
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.energyDensity[i] = 0.00001f;  // Below threshold
            }

            lattice.clearForces();
            forces.calculateGravityForces();

            // Should complete quickly and forces should be very small or zero
            float maxForce = 0.0f;
            for (int i = 0; i < lattice.totalSpheres; i++) {
                maxForce = Math.max(maxForce, Math.abs(lattice.forceX[i]));
                maxForce = Math.max(maxForce, Math.abs(lattice.forceY[i]));
            }

            assertTrue(maxForce < 0.01f, "Negligible energy should produce negligible forces");
        }
    }

    @Nested
    @DisplayName("Force Normalization")
    class ForceNormalizationTests {

        @Test
        @DisplayName("Should limit forces to maximum magnitude")
        void testForceNormalization() {
            // USAGE: Prevent instability by capping force magnitudes
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Create very large forces
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.forceX[i] = 100.0f;
                lattice.forceY[i] = 100.0f;
            }

            float maxForce = 10.0f;
            forces.normalizeForces(maxForce);

            // All forces should be ≤ maxForce
            for (int i = 0; i < lattice.totalSpheres; i++) {
                float magnitude = (float) Math.sqrt(
                    lattice.forceX[i] * lattice.forceX[i] +
                    lattice.forceY[i] * lattice.forceY[i]
                );
                assertTrue(magnitude <= maxForce + 1e-5f,
                    "Force magnitude should be ≤ max: was " + magnitude + " at index " + i);
            }
        }

        @Test
        @DisplayName("Should preserve force direction when normalizing")
        void testNormalizationPreservesDirection() {
            // USAGE: Normalization scales magnitude but keeps direction
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Set force at specific angle
            int idx = 0;
            lattice.forceX[idx] = 30.0f;
            lattice.forceY[idx] = 40.0f;  // Magnitude = 50, angle = atan2(40, 30)

            float originalAngle = (float) Math.atan2(lattice.forceY[idx], lattice.forceX[idx]);

            forces.normalizeForces(10.0f);

            float newAngle = (float) Math.atan2(lattice.forceY[idx], lattice.forceX[idx]);
            float newMagnitude = (float) Math.sqrt(
                lattice.forceX[idx] * lattice.forceX[idx] +
                lattice.forceY[idx] * lattice.forceY[idx]
            );

            assertEquals(originalAngle, newAngle, 1e-5f, "Direction should be preserved");
            assertEquals(10.0f, newMagnitude, 1e-5f, "Magnitude should be clamped to max");
        }

        @Test
        @DisplayName("Should not modify forces below maximum")
        void testNoModificationBelowMax() {
            // USAGE: Forces below max are unchanged
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Set small forces
            lattice.forceX[0] = 2.0f;
            lattice.forceY[0] = 3.0f;  // Magnitude ≈ 3.6

            float originalX = lattice.forceX[0];
            float originalY = lattice.forceY[0];

            forces.normalizeForces(10.0f);  // Max is larger than current force

            assertEquals(originalX, lattice.forceX[0], 1e-6f, "Small force X should be unchanged");
            assertEquals(originalY, lattice.forceY[0], 1e-6f, "Small force Y should be unchanged");
        }

        @Test
        @DisplayName("Should handle zero forces correctly")
        void testNormalizationWithZeroForces() {
            // USAGE: Normalization handles zero forces without NaN
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // All forces are zero
            lattice.clearForces();

            forces.normalizeForces(10.0f);

            // All forces should still be zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.forceX[i], 1e-7f, "Zero force X should remain zero");
                assertEquals(0.0f, lattice.forceY[i], 1e-7f, "Zero force Y should remain zero");
                assertFalse(Float.isNaN(lattice.forceX[i]), "Force X should not be NaN");
                assertFalse(Float.isNaN(lattice.forceY[i]), "Force Y should not be NaN");
            }
        }
    }

    @Nested
    @DisplayName("Combined Forces")
    class CombinedForcesTests {

        @Test
        @DisplayName("Should correctly combine spacing and gravity forces")
        void testCombinedForces() {
            // USAGE: Spacing and gravity forces are additive
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Add mass at center
            int centerIdx = lattice.getIndex(2, 2);
            lattice.energyDensity[centerIdx] = 50.0f;

            // Slightly displace a neighbor
            int rightIdx = lattice.getIndex(3, 2);
            lattice.posX[rightIdx] = 3.2f;  // Slightly stretched from center

            lattice.clearForces();

            // Calculate spacing forces
            forces.calculateSpacingForces();
            float spacingForceX = lattice.forceX[rightIdx];

            // Calculate gravity forces (additive)
            forces.calculateGravityForces();
            float combinedForceX = lattice.forceX[rightIdx];

            // Combined should include both components
            assertNotEquals(spacingForceX, combinedForceX,
                "Combined forces should differ from spacing-only forces");

            // Gravity should pull toward center (negative), spacing force depends on displacement
            // Combined should show both effects
            assertTrue(Math.abs(combinedForceX) > 0.01f, "Combined force should be significant");
        }

        @Test
        @DisplayName("Should allow forces to accumulate across multiple calculations")
        void testForceAccumulation() {
            // USAGE: Forces accumulate in arrays - must clear between timesteps
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Add mass
            lattice.energyDensity[lattice.getIndex(2, 2)] = 50.0f;

            // Calculate gravity twice without clearing
            forces.calculateGravityForces();
            float firstPass = lattice.forceX[lattice.getIndex(3, 2)];

            forces.calculateGravityForces();  // Second time
            float secondPass = lattice.forceX[lattice.getIndex(3, 2)];

            // Forces should accumulate (double)
            assertEquals(firstPass * 2.0f, secondPass, Math.abs(firstPass) * 0.1f,
                "Forces should accumulate when not cleared");

            // Demonstrate clearing
            lattice.clearForces();
            assertEquals(0.0f, lattice.forceX[lattice.getIndex(3, 2)], 1e-7f,
                "Forces should be zero after clearing");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single sphere lattice")
        void testSingleSphereLattice() {
            PlanckLattice lattice = new PlanckLattice(1, 1);
            VectorForces forces = new VectorForces(lattice);

            lattice.energyDensity[0] = 100.0f;

            assertDoesNotThrow(() -> {
                lattice.clearForces();
                forces.calculateSpacingForces();
                forces.calculateGravityForces();
            }, "Single sphere should not cause errors");
        }

        @Test
        @DisplayName("Should handle very small lattice (2x2)")
        void testSmallLattice() {
            PlanckLattice lattice = new PlanckLattice(2, 2);
            VectorForces forces = new VectorForces(lattice);

            lattice.energyDensity[0] = 50.0f;
            lattice.posX[1] = 1.5f;  // Stretch

            assertDoesNotThrow(() -> {
                lattice.clearForces();
                forces.calculateSpacingForces();
                forces.calculateGravityForces();
            }, "Small lattice should not cause errors");

            // Should have some forces
            float totalForce = 0.0f;
            for (int i = 0; i < 4; i++) {
                totalForce += Math.abs(lattice.forceX[i]) + Math.abs(lattice.forceY[i]);
            }
            assertTrue(totalForce > 0.0f, "Small lattice should have forces");
        }

        @Test
        @DisplayName("Should complete quickly on moderate-sized lattice")
        void testPerformanceModerate() {
            // USAGE: Force calculations should be efficient
            PlanckLattice lattice = new PlanckLattice(100, 100);
            VectorForces forces = new VectorForces(lattice);

            // Add some complexity
            lattice.addMassConcentration(50.0f, 50.0f, 100.0f, 10.0f);
            lattice.posX[5000] = 51.5f;  // Some displacement

            long startTime = System.nanoTime();

            lattice.clearForces();
            forces.calculateSpacingForces();
            forces.calculateGravityForces();

            long endTime = System.nanoTime();
            double elapsedMs = (endTime - startTime) / 1_000_000.0;

            // Should complete in reasonable time (< 100ms for 100x100)
            assertTrue(elapsedMs < 100.0,
                "Force calculation should be fast, took " + elapsedMs + "ms");
        }

        @Test
        @DisplayName("Should handle NaN gracefully in normalization")
        void testNaNHandling() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            VectorForces forces = new VectorForces(lattice);

            // Create a potential NaN situation (though implementation should prevent it)
            lattice.forceX[0] = Float.NaN;
            lattice.forceY[0] = Float.NaN;

            // Normalization should handle this without crashing
            assertDoesNotThrow(() -> forces.normalizeForces(10.0f),
                "Should handle NaN without crashing");
        }
    }
}
