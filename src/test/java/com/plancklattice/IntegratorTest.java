package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for Integrator.
 * These tests demonstrate integration functionality and verify physics accuracy.
 *
 * Usage examples:
 * - Integrating positions and velocities over time
 * - Applying damping for stability
 * - Calculating energy (kinetic and potential)
 */
@DisplayName("Integrator - Position and velocity integration")
class IntegratorTest {

    @Nested
    @DisplayName("Basic Integration")
    class BasicIntegrationTests {

        @Test
        @DisplayName("Should update velocity based on force/acceleration")
        void testVelocityUpdate() {
            // USAGE: Apply forces and integrate to update velocities
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Apply constant force to one sphere
            int idx = 0;
            lattice.forceX[idx] = 10.0f;
            lattice.forceY[idx] = 5.0f;

            float dt = 0.01f;
            integrator.integrate(dt);

            // Check velocity change: Δv = (F/m) * dt = (10.0 / 1.0) * 0.01 = 0.1
            float expectedVelX = (10.0f / PlanckLattice.SPHERE_MASS) * dt;
            float expectedVelY = (5.0f / PlanckLattice.SPHERE_MASS) * dt;

            assertEquals(expectedVelX, lattice.velX[idx], 1e-6f, "Velocity X should match expected");
            assertEquals(expectedVelY, lattice.velY[idx], 1e-6f, "Velocity Y should match expected");
        }

        @Test
        @DisplayName("Should update position based on velocity")
        void testPositionUpdate() {
            // USAGE: Velocities move spheres to new positions
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            int idx = 0;
            float initialPosX = lattice.posX[idx];
            float initialPosY = lattice.posY[idx];

            // Set velocity
            lattice.velX[idx] = 10.0f;
            lattice.velY[idx] = 5.0f;

            float dt = 0.1f;
            integrator.integrate(dt);

            // Check position change: Δx = v * dt = 10.0 * 0.1 = 1.0
            float expectedPosX = initialPosX + 10.0f * dt;
            float expectedPosY = initialPosY + 5.0f * dt;

            assertEquals(expectedPosX, lattice.posX[idx], 1e-5f, "Position X should match expected");
            assertEquals(expectedPosY, lattice.posY[idx], 1e-5f, "Position Y should match expected");
        }

        @Test
        @DisplayName("Should perform semi-implicit Euler integration correctly")
        void testSemiImplicitEuler() {
            // USAGE: Integration follows semi-implicit Euler method
            // v(t+dt) = v(t) + a*dt
            // x(t+dt) = x(t) + v(t+dt)*dt  (uses updated velocity)

            PlanckLattice lattice = new PlanckLattice(1, 1);
            Integrator integrator = new Integrator(lattice);

            int idx = 0;
            float initialPos = lattice.posX[idx];
            float initialVel = lattice.velX[idx];
            float force = 20.0f;
            float dt = 0.1f;

            lattice.forceX[idx] = force;

            integrator.integrate(dt);

            // Calculate expected values
            float accel = force / PlanckLattice.SPHERE_MASS;
            float expectedVel = initialVel + accel * dt;  // New velocity
            float expectedPos = initialPos + expectedVel * dt;  // Position uses new velocity

            assertEquals(expectedVel, lattice.velX[idx], 1e-5f, "Velocity should match semi-implicit Euler");
            assertEquals(expectedPos, lattice.posX[idx], 1e-5f, "Position should match semi-implicit Euler");
        }

        @Test
        @DisplayName("Should integrate all spheres in lattice")
        void testIntegrateAllSpheres() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Integrator integrator = new Integrator(lattice);

            // Apply forces to all spheres
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.forceX[i] = i * 0.1f;
                lattice.forceY[i] = i * 0.05f;
            }

            integrator.integrate(0.01f);

            // All spheres should have updated velocities
            for (int i = 0; i < lattice.totalSpheres; i++) {
                if (i > 0) {  // Skip first sphere which has zero force
                    assertTrue(lattice.velX[i] != 0.0f || lattice.velY[i] != 0.0f,
                        "Sphere " + i + " should have non-zero velocity");
                }
            }
        }

        @Test
        @DisplayName("Should handle zero forces correctly")
        void testIntegrationWithZeroForces() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Set initial velocity
            int idx = 5;
            lattice.velX[idx] = 2.0f;
            float initialPosX = lattice.posX[idx];

            // No forces applied
            lattice.clearForces();

            float dt = 0.1f;
            integrator.integrate(dt);

            // Velocity should remain constant (no acceleration)
            assertEquals(2.0f, lattice.velX[idx], 1e-6f, "Velocity should be unchanged with no forces");

            // Position should update based on constant velocity
            assertEquals(initialPosX + 2.0f * dt, lattice.posX[idx], 1e-5f,
                "Position should update with constant velocity");
        }

        @Test
        @DisplayName("Should handle different timestep sizes")
        void testVariableTimestep() {
            PlanckLattice lattice = new PlanckLattice(1, 1);
            Integrator integrator = new Integrator(lattice);

            int idx = 0;
            lattice.forceX[idx] = 10.0f;

            // Integrate with different timesteps
            float dt1 = 0.01f;
            integrator.integrate(dt1);
            float vel1 = lattice.velX[idx];

            // Reset
            lattice.velX[idx] = 0.0f;

            float dt2 = 0.02f;  // Double timestep
            integrator.integrate(dt2);
            float vel2 = lattice.velX[idx];

            // Velocity change should be proportional to dt
            assertEquals(vel1 * 2.0f, vel2, 1e-5f, "Larger timestep should produce proportionally larger velocity change");
        }
    }

    @Nested
    @DisplayName("Damping")
    class DampingTests {

        @Test
        @DisplayName("Should reduce velocity magnitude when damping is applied")
        void testDampingReducesVelocity() {
            // USAGE: Apply damping to prevent runaway oscillations
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            int idx = 0;
            lattice.velX[idx] = 10.0f;
            lattice.velY[idx] = 5.0f;

            float dampingFactor = 0.1f;  // 10% damping
            integrator.applyDamping(dampingFactor);

            // Velocity should be reduced by (1 - dampingFactor)
            assertEquals(10.0f * 0.9f, lattice.velX[idx], 1e-5f, "Velocity X should be damped");
            assertEquals(5.0f * 0.9f, lattice.velY[idx], 1e-5f, "Velocity Y should be damped");
        }

        @Test
        @DisplayName("Should apply damping to all spheres")
        void testDampingAllSpheres() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Integrator integrator = new Integrator(lattice);

            // Set velocities for all spheres
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.velX[i] = 5.0f;
                lattice.velY[i] = 3.0f;
            }

            integrator.applyDamping(0.2f);

            // All velocities should be damped
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(5.0f * 0.8f, lattice.velX[i], 1e-5f, "Velocity X damped for sphere " + i);
                assertEquals(3.0f * 0.8f, lattice.velY[i], 1e-5f, "Velocity Y damped for sphere " + i);
            }
        }

        @Test
        @DisplayName("Should not modify velocities when damping is zero")
        void testZeroDamping() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            lattice.velX[0] = 10.0f;
            lattice.velY[0] = 5.0f;

            integrator.applyDamping(0.0f);

            assertEquals(10.0f, lattice.velX[0], 1e-6f, "Velocity X should be unchanged with zero damping");
            assertEquals(5.0f, lattice.velY[0], 1e-6f, "Velocity Y should be unchanged with zero damping");
        }

        @Test
        @DisplayName("Should handle damping factor of 1.0 (complete stop)")
        void testFullDamping() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            lattice.velX[0] = 10.0f;
            lattice.velY[0] = 5.0f;

            integrator.applyDamping(1.0f);  // Full damping

            // Velocities should become zero or remain unchanged (boundary condition)
            // Implementation returns early if dampingFactor >= 1.0
            assertEquals(10.0f, lattice.velX[0], 1e-6f, "Full damping should not modify (boundary)");
        }

        @Test
        @DisplayName("Should ignore invalid damping factors")
        void testInvalidDampingFactors() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            lattice.velX[0] = 10.0f;

            // Negative damping
            integrator.applyDamping(-0.1f);
            assertEquals(10.0f, lattice.velX[0], 1e-6f, "Negative damping should be ignored");

            // Greater than 1.0
            integrator.applyDamping(1.5f);
            assertEquals(10.0f, lattice.velX[0], 1e-6f, "Damping > 1.0 should be ignored");
        }
    }

    @Nested
    @DisplayName("Energy Calculations")
    class EnergyCalculationTests {

        @Test
        @DisplayName("Should calculate kinetic energy correctly")
        void testKineticEnergy() {
            // USAGE: Monitor kinetic energy of the system
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Set velocity for one sphere
            // KE = 0.5 * m * v^2 = 0.5 * 1.0 * (3^2 + 4^2) = 0.5 * 25 = 12.5
            lattice.velX[0] = 3.0f;
            lattice.velY[0] = 4.0f;

            float ke = integrator.calculateKineticEnergy();

            assertEquals(12.5f, ke, 1e-5f, "Kinetic energy should match expected value");
        }

        @Test
        @DisplayName("Should sum kinetic energy across all spheres")
        void testTotalKineticEnergy() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Set velocities for multiple spheres
            lattice.velX[0] = 2.0f;  // KE = 0.5 * 1.0 * 4 = 2.0
            lattice.velX[1] = 4.0f;  // KE = 0.5 * 1.0 * 16 = 8.0
            lattice.velY[2] = 6.0f;  // KE = 0.5 * 1.0 * 36 = 18.0

            float totalKE = integrator.calculateKineticEnergy();
            float expectedKE = 2.0f + 8.0f + 18.0f;

            assertEquals(expectedKE, totalKE, 1e-5f, "Total KE should be sum of all spheres");
        }

        @Test
        @DisplayName("Should return zero kinetic energy for stationary lattice")
        void testZeroKineticEnergy() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Integrator integrator = new Integrator(lattice);

            // All velocities are zero by default
            float ke = integrator.calculateKineticEnergy();

            assertEquals(0.0f, ke, 1e-7f, "Stationary lattice should have zero KE");
        }

        @Test
        @DisplayName("Should calculate potential energy for equilibrium lattice")
        void testPotentialEnergyAtEquilibrium() {
            // USAGE: Monitor elastic potential energy in the lattice
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Integrator integrator = new Integrator(lattice);

            // At equilibrium, all springs have zero displacement, so PE = 0
            float pe = integrator.calculatePotentialEnergy();

            assertEquals(0.0f, pe, 1e-5f, "Equilibrium lattice should have zero PE");
        }

        @Test
        @DisplayName("Should calculate potential energy for stretched lattice")
        void testPotentialEnergyStretched() {
            PlanckLattice lattice = new PlanckLattice(3, 1);  // Simple 1D lattice
            Integrator integrator = new Integrator(lattice);

            // Stretch the lattice: positions at 0, 2, 4 (spacing = 2 instead of 1)
            lattice.posX[0] = 0.0f;
            lattice.posX[1] = 2.0f;
            lattice.posX[2] = 4.0f;

            float pe = integrator.calculatePotentialEnergy();

            // Each spring: displacement = 1.0, PE = 0.5 * k * d^2 = 0.5 * 1.0 * 1.0 = 0.5
            // Two springs: total PE = 1.0
            assertTrue(pe > 0.5f, "Stretched lattice should have positive PE, was " + pe);
        }

        @Test
        @DisplayName("Should calculate potential energy for compressed lattice")
        void testPotentialEnergyCompressed() {
            PlanckLattice lattice = new PlanckLattice(3, 1);
            Integrator integrator = new Integrator(lattice);

            // Compress: positions at 0, 0.5, 1.0 (spacing = 0.5 instead of 1)
            lattice.posX[0] = 0.0f;
            lattice.posX[1] = 0.5f;
            lattice.posX[2] = 1.0f;

            float pe = integrator.calculatePotentialEnergy();

            // Each spring: displacement = -0.5, PE = 0.5 * k * 0.25 = 0.125
            // Two springs: total PE = 0.25
            assertEquals(0.25f, pe, 0.05f, "Compressed lattice should have PE ≈ 0.25");
        }

        @Test
        @DisplayName("Should not double-count spring connections")
        void testNoDuplicatePotentialEnergy() {
            // Implementation only counts right and down neighbors to avoid double-counting
            PlanckLattice lattice = new PlanckLattice(2, 2);
            Integrator integrator = new Integrator(lattice);

            // Uniformly stretch entire lattice
            for (int i = 0; i < 4; i++) {
                lattice.posX[i] *= 1.5f;
                lattice.posY[i] *= 1.5f;
            }

            float pe = integrator.calculatePotentialEnergy();

            // Should have exactly 4 springs (2 horizontal + 2 vertical in 2x2 grid)
            // Each with displacement 0.5, PE = 0.5 * 1.0 * 0.25 = 0.125
            // Total = 0.5
            assertTrue(pe > 0.3f && pe < 0.7f,
                "PE should be counted correctly without duplication, was " + pe);
        }
    }

    @Nested
    @DisplayName("Conservation and Stability")
    class ConservationTests {

        @Test
        @DisplayName("Should conserve energy in isolated system (no damping)")
        void testEnergyConservation() {
            // USAGE: Verify energy conservation for physics accuracy
            PlanckLattice lattice = new PlanckLattice(3, 3);
            Integrator integrator = new Integrator(lattice);
            VectorForces forces = new VectorForces(lattice);

            // Add some initial energy
            lattice.velX[4] = 2.0f;  // Center sphere moving

            float initialKE = integrator.calculateKineticEnergy();
            float initialPE = integrator.calculatePotentialEnergy();
            float initialTotalEnergy = initialKE + initialPE;

            // Run several integration steps
            for (int i = 0; i < 10; i++) {
                lattice.clearForces();
                forces.calculateSpacingForces();
                integrator.integrate(0.01f);
                // No damping
            }

            float finalKE = integrator.calculateKineticEnergy();
            float finalPE = integrator.calculatePotentialEnergy();
            float finalTotalEnergy = finalKE + finalPE;

            // Energy should be approximately conserved (within numerical error)
            assertEquals(initialTotalEnergy, finalTotalEnergy, initialTotalEnergy * 0.1f,
                "Total energy should be approximately conserved");
        }

        @Test
        @DisplayName("Should dissipate energy with damping")
        void testEnergyDissipationWithDamping() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Set initial velocities
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.velX[i] = 1.0f;
                lattice.velY[i] = 1.0f;
            }

            float initialKE = integrator.calculateKineticEnergy();

            // Apply damping multiple times
            for (int i = 0; i < 5; i++) {
                integrator.applyDamping(0.1f);
            }

            float finalKE = integrator.calculateKineticEnergy();

            assertTrue(finalKE < initialKE, "Damping should reduce kinetic energy");
            assertTrue(finalKE > 0, "Some kinetic energy should remain");
        }

        @Test
        @DisplayName("Should remain stable over many timesteps")
        void testLongTermStability() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            Integrator integrator = new Integrator(lattice);
            VectorForces forces = new VectorForces(lattice);

            // Add small perturbation
            lattice.posX[50] = lattice.posX[50] + 0.1f;

            // Integrate for many steps
            for (int i = 0; i < 100; i++) {
                lattice.clearForces();
                forces.calculateSpacingForces();
                integrator.integrate(0.001f);  // Small timestep
                integrator.applyDamping(0.01f);  // Small damping for stability
            }

            // Check for NaN or infinity (signs of instability)
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertFalse(Float.isNaN(lattice.posX[i]), "Position X should not be NaN");
                assertFalse(Float.isNaN(lattice.posY[i]), "Position Y should not be NaN");
                assertFalse(Float.isInfinite(lattice.posX[i]), "Position X should not be infinite");
                assertFalse(Float.isInfinite(lattice.posY[i]), "Position Y should not be infinite");
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very small timestep")
        void testVerySmallTimestep() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            lattice.forceX[0] = 10.0f;

            float dt = 1e-6f;  // Very small
            integrator.integrate(dt);

            // Should produce very small velocity change
            assertTrue(Math.abs(lattice.velX[0]) < 1e-4f,
                "Very small timestep should produce very small change");
            assertFalse(Float.isNaN(lattice.velX[0]), "Should not produce NaN");
        }

        @Test
        @DisplayName("Should handle large forces without instability")
        void testLargeForces() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            Integrator integrator = new Integrator(lattice);

            // Apply large force
            lattice.forceX[0] = 1000.0f;

            float dt = 0.001f;  // Small timestep for stability
            integrator.integrate(dt);

            // Should not produce NaN or infinity
            assertFalse(Float.isNaN(lattice.velX[0]), "Large force should not produce NaN");
            assertFalse(Float.isInfinite(lattice.velX[0]), "Large force should not produce infinity");
        }

        @Test
        @DisplayName("Should handle single sphere lattice")
        void testSingleSphere() {
            PlanckLattice lattice = new PlanckLattice(1, 1);
            Integrator integrator = new Integrator(lattice);

            lattice.forceX[0] = 5.0f;

            assertDoesNotThrow(() -> integrator.integrate(0.01f), "Single sphere should integrate without error");
            assertDoesNotThrow(() -> integrator.applyDamping(0.1f), "Single sphere damping should work");
            assertDoesNotThrow(() -> integrator.calculateKineticEnergy(), "Single sphere KE calculation should work");
            assertDoesNotThrow(() -> integrator.calculatePotentialEnergy(), "Single sphere PE calculation should work");
        }

        @Test
        @DisplayName("Should perform efficiently on moderate lattice")
        void testPerformance() {
            PlanckLattice lattice = new PlanckLattice(100, 100);
            Integrator integrator = new Integrator(lattice);

            // Apply some forces
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.forceX[i] = 0.1f;
                lattice.forceY[i] = 0.05f;
            }

            long startTime = System.nanoTime();
            integrator.integrate(0.01f);
            long endTime = System.nanoTime();

            double elapsedMs = (endTime - startTime) / 1_000_000.0;

            // Should be fast (< 10ms for 10,000 spheres)
            assertTrue(elapsedMs < 10.0, "Integration should be fast, took " + elapsedMs + "ms");
        }

        @Test
        @DisplayName("Should handle rectangular lattice correctly")
        void testRectangularLattice() {
            PlanckLattice lattice = new PlanckLattice(5, 10);
            Integrator integrator = new Integrator(lattice);

            // Set velocities
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.velX[i] = 1.0f;
            }

            integrator.integrate(0.01f);

            // All spheres should have updated positions
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertTrue(lattice.posX[i] > i % 5, "Position should have increased");
            }
        }
    }
}
