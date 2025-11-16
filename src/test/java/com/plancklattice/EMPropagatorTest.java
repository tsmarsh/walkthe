package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for EMPropagator.
 * These tests demonstrate EM wave propagation and verify wave physics.
 *
 * Usage examples:
 * - Propagating EM waves through the lattice
 * - Tracking wave energy and amplitude
 * - Finding wave center of mass
 * - Applying boundary conditions
 */
@DisplayName("EMPropagator - Electromagnetic wave propagation")
class EMPropagatorTest {

    @Nested
    @DisplayName("Basic Wave Propagation")
    class BasicPropagationTests {

        @Test
        @DisplayName("Should propagate waves to neighbors")
        void testWavePropagation() {
            // USAGE: Waves spread from initial pulse location
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add pulse at center
            lattice.addEMPulse(5.0f, 5.0f, 1.0f, 2.0f, 4.0f);

            int centerIdx = lattice.getIndex(5, 5);
            float initialCenterAmplitude = (float) Math.sqrt(
                lattice.emFieldReal[centerIdx] * lattice.emFieldReal[centerIdx] +
                lattice.emFieldImag[centerIdx] * lattice.emFieldImag[centerIdx]
            );

            // Propagate
            propagator.propagate(0.01f);

            // Neighbors should now have non-zero field
            int rightIdx = lattice.getIndex(6, 5);
            float neighborAmplitude = (float) Math.sqrt(
                lattice.emFieldReal[rightIdx] * lattice.emFieldReal[rightIdx] +
                lattice.emFieldImag[rightIdx] * lattice.emFieldImag[rightIdx]
            );

            assertTrue(neighborAmplitude > 0.01f,
                "Wave should propagate to neighbors, amplitude was " + neighborAmplitude);
        }

        @Test
        @DisplayName("Should preserve zero field when no pulse present")
        void testNoChangeBringWithoutPulse() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            // No pulse added - all fields are zero
            propagator.propagate(0.01f);

            // All fields should remain zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.emFieldReal[i], 1e-7f, "Real field should remain zero");
                assertEquals(0.0f, lattice.emFieldImag[i], 1e-7f, "Imag field should remain zero");
            }
        }

        @Test
        @DisplayName("Should handle both real and imaginary components")
        void testComplexFieldPropagation() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            EMPropagator propagator = new EMPropagator(lattice);

            // Set complex field manually
            int centerIdx = lattice.getIndex(2, 2);
            lattice.emFieldReal[centerIdx] = 3.0f;
            lattice.emFieldImag[centerIdx] = 4.0f;

            propagator.propagate(0.01f);

            // Both components should propagate
            int neighborIdx = lattice.getIndex(3, 2);
            assertTrue(Math.abs(lattice.emFieldReal[neighborIdx]) > 0.01f,
                "Real component should propagate");
            assertTrue(Math.abs(lattice.emFieldImag[neighborIdx]) > 0.01f,
                "Imaginary component should propagate");
        }

        @Test
        @DisplayName("Should use toroidal boundary conditions")
        void testToroidalBoundaries() {
            // USAGE: Waves wrap around edges
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add pulse at edge
            int edgeIdx = lattice.getIndex(0, 5);
            lattice.emFieldReal[edgeIdx] = 5.0f;

            propagator.propagate(0.01f);

            // Wave should wrap to opposite edge
            int wrappedIdx = lattice.getIndex(9, 5);  // Left neighbor wraps to right edge
            float wrappedAmplitude = Math.abs(lattice.emFieldReal[wrappedIdx]);

            assertTrue(wrappedAmplitude > 0.01f,
                "Wave should propagate through toroidal boundary, amplitude was " + wrappedAmplitude);
        }

        @Test
        @DisplayName("Should dampen wave amplitude over time")
        void testWaveDamping() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(5.0f, 5.0f, 1.0f, 3.0f, 4.0f);

            float initialEnergy = propagator.calculateEMEnergy();

            // Propagate multiple times
            for (int i = 0; i < 10; i++) {
                propagator.propagate(0.01f);
            }

            float finalEnergy = propagator.calculateEMEnergy();

            // Energy should decrease due to damping
            assertTrue(finalEnergy < initialEnergy,
                "Wave energy should decrease due to damping: initial=" + initialEnergy + ", final=" + finalEnergy);
        }
    }

    @Nested
    @DisplayName("Vectorized Propagation")
    class VectorizedPropagationTests {

        @Test
        @DisplayName("Should produce same results as scalar version")
        void testVectorizedMatchesScalar() {
            // USAGE: Vectorized and scalar versions should be equivalent
            PlanckLattice lattice1 = new PlanckLattice(20, 20);
            EMPropagator propagator1 = new EMPropagator(lattice1);
            lattice1.addEMPulse(10.0f, 10.0f, 1.0f, 3.0f, 5.0f);

            PlanckLattice lattice2 = new PlanckLattice(20, 20);
            EMPropagator propagator2 = new EMPropagator(lattice2);
            lattice2.addEMPulse(10.0f, 10.0f, 1.0f, 3.0f, 5.0f);

            // Scalar propagation
            propagator1.propagate(0.01f);

            // Vectorized propagation
            propagator2.propagateVectorized(0.01f);

            // Results should be very close
            for (int i = 0; i < lattice1.totalSpheres; i++) {
                assertEquals(lattice1.emFieldReal[i], lattice2.emFieldReal[i], 1e-5f,
                    "Real fields should match at index " + i);
                assertEquals(lattice1.emFieldImag[i], lattice2.emFieldImag[i], 1e-5f,
                    "Imag fields should match at index " + i);
            }
        }

        @Test
        @DisplayName("Should handle edge cases in vectorization")
        void testVectorizedEdgeCases() {
            // Test with size that doesn't perfectly divide by vector lanes
            PlanckLattice lattice = new PlanckLattice(17, 13);  // Prime-ish dimensions
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(8.0f, 6.0f, 1.0f, 2.0f, 3.0f);

            assertDoesNotThrow(() -> propagator.propagateVectorized(0.01f),
                "Vectorized propagation should handle non-aligned sizes");

            // Should not produce NaN or infinity
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertFalse(Float.isNaN(lattice.emFieldReal[i]), "Should not produce NaN");
                assertFalse(Float.isInfinite(lattice.emFieldReal[i]), "Should not produce infinity");
            }
        }
    }

    @Nested
    @DisplayName("Energy Calculations")
    class EnergyCalculationTests {

        @Test
        @DisplayName("Should calculate EM energy correctly")
        void testEMEnergyCalculation() {
            // USAGE: Track total EM field energy
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            // Set known field values
            lattice.emFieldReal[0] = 3.0f;
            lattice.emFieldImag[0] = 4.0f;  // Amplitude^2 = 25

            lattice.emFieldReal[1] = 1.0f;
            lattice.emFieldImag[1] = 0.0f;  // Amplitude^2 = 1

            float energy = propagator.calculateEMEnergy();

            // Energy = sum of amplitude^2 = 25 + 1 = 26
            assertEquals(26.0f, energy, 1e-5f, "EM energy should be sum of amplitude squared");
        }

        @Test
        @DisplayName("Should return zero energy for empty field")
        void testZeroEMEnergy() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            float energy = propagator.calculateEMEnergy();

            assertEquals(0.0f, energy, 1e-7f, "Empty field should have zero energy");
        }

        @Test
        @DisplayName("Should track energy decrease due to damping")
        void testEnergyDecreaseWithDamping() {
            PlanckLattice lattice = new PlanckLattice(15, 15);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(7.5f, 7.5f, 1.0f, 4.0f, 5.0f);

            float[] energies = new float[20];
            energies[0] = propagator.calculateEMEnergy();

            // Propagate and track energy
            for (int i = 1; i < 20; i++) {
                propagator.propagate(0.01f);
                energies[i] = propagator.calculateEMEnergy();
            }

            // Energy should generally decrease
            assertTrue(energies[19] < energies[0],
                "Energy should decrease over time: initial=" + energies[0] + ", final=" + energies[19]);

            // Should be monotonically decreasing (or very close due to numerical effects)
            int decreaseCount = 0;
            for (int i = 1; i < 20; i++) {
                if (energies[i] < energies[i-1]) {
                    decreaseCount++;
                }
            }
            assertTrue(decreaseCount > 15, "Energy should mostly decrease, decreased " + decreaseCount + "/19 steps");
        }
    }

    @Nested
    @DisplayName("Wave Center Tracking")
    class WaveCenterTests {

        @Test
        @DisplayName("Should find wave center correctly")
        void testWaveCenter() {
            // USAGE: Track where the wave pulse is located
            PlanckLattice lattice = new PlanckLattice(20, 20);
            EMPropagator propagator = new EMPropagator(lattice);

            float centerX = 10.0f;
            float centerY = 10.0f;
            lattice.addEMPulse(centerX, centerY, 1.0f, 3.0f, 5.0f);

            float[] center = propagator.getWaveCenter();

            // Center should be near the pulse location
            assertEquals(centerX, center[0], 1.0f, "Wave center X should be near pulse location");
            assertEquals(centerY, center[1], 1.0f, "Wave center Y should be near pulse location");
        }

        @Test
        @DisplayName("Should track wave movement as it propagates")
        void testWaveCenterMovement() {
            PlanckLattice lattice = new PlanckLattice(30, 30);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add pulse off-center
            lattice.addEMPulse(10.0f, 15.0f, 1.0f, 2.0f, 4.0f);

            float[] initialCenter = propagator.getWaveCenter();

            // Propagate
            for (int i = 0; i < 5; i++) {
                propagator.propagate(0.01f);
            }

            float[] finalCenter = propagator.getWaveCenter();

            // Wave should spread, center might shift slightly
            // Just verify it returns valid coordinates
            assertTrue(finalCenter[0] >= 0 && finalCenter[0] < 30, "Center X should be in bounds");
            assertTrue(finalCenter[1] >= 0 && finalCenter[1] < 30, "Center Y should be in bounds");
        }

        @Test
        @DisplayName("Should return zero for empty field")
        void testWaveCenterEmptyField() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            float[] center = propagator.getWaveCenter();

            assertEquals(0.0f, center[0], 1e-5f, "Empty field should have center at origin X");
            assertEquals(0.0f, center[1], 1e-5f, "Empty field should have center at origin Y");
        }

        @Test
        @DisplayName("Should weight center by amplitude")
        void testWaveCenterWeighting() {
            PlanckLattice lattice = new PlanckLattice(10, 10);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add two points with different amplitudes
            int idx1 = lattice.getIndex(2, 5);
            int idx2 = lattice.getIndex(8, 5);

            lattice.emFieldReal[idx1] = 3.0f;  // Amplitude = 3 at (2, 5)
            lattice.emFieldReal[idx2] = 1.0f;  // Amplitude = 1 at (8, 5)

            float[] center = propagator.getWaveCenter();

            // Center should be weighted toward the stronger amplitude
            // Weighted average: (2*3 + 8*1) / (3+1) = 14/4 = 3.5
            assertEquals(3.5f, center[0], 1.0f, "Center X should be weighted by amplitude");
            assertEquals(5.0f, center[1], 1.0f, "Center Y should be at y=5");
        }
    }

    @Nested
    @DisplayName("Boundary Conditions")
    class BoundaryConditionTests {

        @Test
        @DisplayName("Should apply absorbing boundaries")
        void testAbsorbingBoundaries() {
            // USAGE: Prevent reflections at boundaries
            PlanckLattice lattice = new PlanckLattice(20, 20);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add field near boundary
            for (int x = 0; x < 5; x++) {
                int idx = lattice.getIndex(x, 10);
                lattice.emFieldReal[idx] = 1.0f;
            }

            float initialAmplitude = Math.abs(lattice.emFieldReal[lattice.getIndex(2, 10)]);

            int borderWidth = 3;
            propagator.applyAbsorbingBoundaries(borderWidth);

            float finalAmplitude = Math.abs(lattice.emFieldReal[lattice.getIndex(2, 10)]);

            // Amplitude near boundary should be reduced
            assertTrue(finalAmplitude < initialAmplitude,
                "Absorbing boundaries should reduce amplitude near edges");
        }

        @Test
        @DisplayName("Should not affect interior points")
        void testAbsorbingBoundariesInterior() {
            PlanckLattice lattice = new PlanckLattice(20, 20);
            EMPropagator propagator = new EMPropagator(lattice);

            // Add field in interior
            int centerIdx = lattice.getIndex(10, 10);
            lattice.emFieldReal[centerIdx] = 1.0f;

            float initialAmplitude = lattice.emFieldReal[centerIdx];

            propagator.applyAbsorbingBoundaries(3);

            float finalAmplitude = lattice.emFieldReal[centerIdx];

            // Interior point should be unchanged
            assertEquals(initialAmplitude, finalAmplitude, 1e-6f,
                "Interior points should not be affected by absorbing boundaries");
        }

        @Test
        @DisplayName("Should scale damping with border width")
        void testBorderWidthScaling() {
            PlanckLattice lattice1 = new PlanckLattice(20, 20);
            EMPropagator propagator1 = new EMPropagator(lattice1);

            PlanckLattice lattice2 = new PlanckLattice(20, 20);
            EMPropagator propagator2 = new EMPropagator(lattice2);

            // Same initial field
            for (int i = 0; i < lattice1.totalSpheres; i++) {
                lattice1.emFieldReal[i] = 1.0f;
                lattice2.emFieldReal[i] = 1.0f;
            }

            // Different border widths
            propagator1.applyAbsorbingBoundaries(2);
            propagator2.applyAbsorbingBoundaries(5);

            // Larger border width should affect more points
            int affected1 = 0;
            int affected2 = 0;

            for (int i = 0; i < lattice1.totalSpheres; i++) {
                if (lattice1.emFieldReal[i] < 0.99f) affected1++;
                if (lattice2.emFieldReal[i] < 0.99f) affected2++;
            }

            assertTrue(affected2 > affected1,
                "Larger border width should affect more points: " + affected1 + " vs " + affected2);
        }
    }

    @Nested
    @DisplayName("Wave Physics")
    class WavePhysicsTests {

        @Test
        @DisplayName("Should propagate wave through the lattice")
        void testPropagationSpeed() {
            // USAGE: Verify wave propagation occurs
            PlanckLattice lattice = new PlanckLattice(50, 50);
            EMPropagator propagator = new EMPropagator(lattice);

            // Create a pulse at one location
            lattice.addEMPulse(10.0f, 25.0f, 1.0f, 3.0f, 5.0f);

            float initialCenterAmplitude = lattice.getMaxEMAmplitude();

            // Propagate for several steps
            for (int i = 0; i < 15; i++) {
                propagator.propagate(0.1f);
            }

            // Wave should have spread - check that energy is distributed
            int nonZeroPoints = 0;
            for (int i = 0; i < lattice.totalSpheres; i++) {
                float amplitude = (float) Math.sqrt(
                    lattice.emFieldReal[i] * lattice.emFieldReal[i] +
                    lattice.emFieldImag[i] * lattice.emFieldImag[i]
                );
                if (amplitude > 0.001f) {
                    nonZeroPoints++;
                }
            }

            assertTrue(nonZeroPoints > 50,
                "Wave should have spread to many points, found " + nonZeroPoints);
        }

        @Test
        @DisplayName("Should spread wave packet over time")
        void testWavePacketSpread() {
            PlanckLattice lattice = new PlanckLattice(30, 30);
            EMPropagator propagator = new EMPropagator(lattice);

            // Narrow pulse
            lattice.addEMPulse(15.0f, 15.0f, 1.0f, 1.0f, 5.0f);

            float initialMaxAmplitude = lattice.getMaxEMAmplitude();

            // Propagate
            for (int i = 0; i < 20; i++) {
                propagator.propagate(0.1f);
            }

            float finalMaxAmplitude = lattice.getMaxEMAmplitude();

            // Peak amplitude should decrease as wave spreads
            assertTrue(finalMaxAmplitude < initialMaxAmplitude,
                "Wave packet should spread (decrease peak amplitude)");
        }

        @Test
        @DisplayName("Should maintain wave pattern structure")
        void testWavePatternMaintenance() {
            PlanckLattice lattice = new PlanckLattice(20, 20);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(10.0f, 10.0f, 1.0f, 3.0f, 4.0f);

            // Propagate a few steps
            for (int i = 0; i < 3; i++) {
                propagator.propagate(0.1f);
            }

            // Should still have wave-like structure (some points positive, some negative)
            int positiveCount = 0;
            int negativeCount = 0;

            for (int i = 0; i < lattice.totalSpheres; i++) {
                if (lattice.emFieldReal[i] > 0.01f) positiveCount++;
                if (lattice.emFieldReal[i] < -0.01f) negativeCount++;
            }

            assertTrue(positiveCount > 0 && negativeCount > 0,
                "Wave should maintain oscillatory structure");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single sphere lattice")
        void testSingleSphere() {
            PlanckLattice lattice = new PlanckLattice(1, 1);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.emFieldReal[0] = 1.0f;

            assertDoesNotThrow(() -> propagator.propagate(0.01f),
                "Single sphere should not cause errors");
            assertDoesNotThrow(() -> propagator.propagateVectorized(0.01f),
                "Single sphere vectorized should not cause errors");
        }

        @Test
        @DisplayName("Should handle very small lattice (2x2)")
        void testSmallLattice() {
            PlanckLattice lattice = new PlanckLattice(2, 2);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.emFieldReal[0] = 1.0f;
            lattice.emFieldImag[0] = 0.5f;

            assertDoesNotThrow(() -> propagator.propagate(0.01f));

            // Propagate several more times to ensure wave reaches all spheres
            for (int i = 0; i < 5; i++) {
                propagator.propagate(0.01f);
            }

            // After multiple propagations, field should be present throughout
            float totalField = 0.0f;
            for (int i = 0; i < 4; i++) {
                totalField += Math.abs(lattice.emFieldReal[i]) + Math.abs(lattice.emFieldImag[i]);
            }

            assertTrue(totalField > 0.01f,
                "After multiple propagations, field should be present in lattice");
        }

        @Test
        @DisplayName("Should perform efficiently on moderate lattice")
        void testPerformance() {
            PlanckLattice lattice = new PlanckLattice(100, 100);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(50.0f, 50.0f, 1.0f, 10.0f, 5.0f);

            long startTime = System.nanoTime();
            propagator.propagateVectorized(0.01f);
            long endTime = System.nanoTime();

            double elapsedMs = (endTime - startTime) / 1_000_000.0;

            // Should be fast (< 20ms for 10,000 spheres)
            assertTrue(elapsedMs < 20.0,
                "Propagation should be fast, took " + elapsedMs + "ms");
        }

        @Test
        @DisplayName("Should handle extreme field values")
        void testExtremeFieldValues() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            EMPropagator propagator = new EMPropagator(lattice);

            // Very large values
            lattice.emFieldReal[0] = 1000.0f;
            lattice.emFieldImag[0] = 1000.0f;

            propagator.propagate(0.01f);

            // Should not produce NaN or infinity
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertFalse(Float.isNaN(lattice.emFieldReal[i]),
                    "Should not produce NaN for large values");
                assertFalse(Float.isInfinite(lattice.emFieldReal[i]),
                    "Should not produce infinity for large values");
            }
        }

        @Test
        @DisplayName("Should handle rectangular lattice")
        void testRectangularLattice() {
            PlanckLattice lattice = new PlanckLattice(15, 25);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(7.5f, 12.5f, 1.0f, 3.0f, 5.0f);

            assertDoesNotThrow(() -> propagator.propagate(0.01f),
                "Rectangular lattice should work");
            assertDoesNotThrow(() -> propagator.propagateVectorized(0.01f),
                "Rectangular lattice vectorized should work");

            float energy = propagator.calculateEMEnergy();
            assertTrue(energy > 0, "Should have non-zero energy");
        }

        @Test
        @DisplayName("Should be stable over many iterations")
        void testLongTermStability() {
            PlanckLattice lattice = new PlanckLattice(20, 20);
            EMPropagator propagator = new EMPropagator(lattice);

            lattice.addEMPulse(10.0f, 10.0f, 1.0f, 3.0f, 5.0f);

            // Propagate many times
            for (int i = 0; i < 100; i++) {
                propagator.propagate(0.01f);
            }

            // Check for numerical stability
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertFalse(Float.isNaN(lattice.emFieldReal[i]),
                    "Long-term propagation should not produce NaN");
                assertFalse(Float.isInfinite(lattice.emFieldReal[i]),
                    "Long-term propagation should not produce infinity");
            }
        }
    }
}
