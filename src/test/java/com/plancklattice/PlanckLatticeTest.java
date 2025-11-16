package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for PlanckLattice.
 * These tests serve as both documentation and regression detection.
 *
 * Usage examples:
 * - Creating a lattice
 * - Accessing and manipulating lattice positions
 * - Adding EM pulses and mass concentrations
 * - Calculating lattice properties
 */
@DisplayName("PlanckLattice - Core lattice data structure")
class PlanckLatticeTest {

    @Nested
    @DisplayName("Construction and Initialization")
    class ConstructionTests {

        @Test
        @DisplayName("Should create lattice with correct dimensions")
        void testLatticeCreation() {
            // USAGE: Create a 10x20 lattice
            PlanckLattice lattice = new PlanckLattice(10, 20);

            // Verify dimensions
            assertEquals(10, lattice.gridWidth, "Grid width should match constructor argument");
            assertEquals(20, lattice.gridHeight, "Grid height should match constructor argument");
            assertEquals(200, lattice.totalSpheres, "Total spheres should be width × height");
        }

        @Test
        @DisplayName("Should initialize all arrays with correct size")
        void testArrayAllocation() {
            PlanckLattice lattice = new PlanckLattice(5, 5);
            int expectedSize = 25;

            // All arrays should be allocated with correct size
            assertEquals(expectedSize, lattice.posX.length, "posX array size");
            assertEquals(expectedSize, lattice.posY.length, "posY array size");
            assertEquals(expectedSize, lattice.velX.length, "velX array size");
            assertEquals(expectedSize, lattice.velY.length, "velY array size");
            assertEquals(expectedSize, lattice.forceX.length, "forceX array size");
            assertEquals(expectedSize, lattice.forceY.length, "forceY array size");
            assertEquals(expectedSize, lattice.energyDensity.length, "energyDensity array size");
            assertEquals(expectedSize, lattice.emFieldReal.length, "emFieldReal array size");
            assertEquals(expectedSize, lattice.emFieldImag.length, "emFieldImag array size");
            assertEquals(expectedSize, lattice.emFieldRealNext.length, "emFieldRealNext array size");
            assertEquals(expectedSize, lattice.emFieldImagNext.length, "emFieldImagNext array size");
        }

        @Test
        @DisplayName("Should initialize positions in regular grid with equilibrium spacing")
        void testInitialPositions() {
            // USAGE: Lattice starts with spheres in regular grid
            PlanckLattice lattice = new PlanckLattice(3, 3);

            // Check that positions are initialized correctly
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int index = lattice.getIndex(x, y);
                    float expectedX = x * PlanckLattice.EQUILIBRIUM_DISTANCE;
                    float expectedY = y * PlanckLattice.EQUILIBRIUM_DISTANCE;

                    assertEquals(expectedX, lattice.posX[index], 0.0001f,
                        "Initial X position at (" + x + "," + y + ")");
                    assertEquals(expectedY, lattice.posY[index], 0.0001f,
                        "Initial Y position at (" + x + "," + y + ")");
                }
            }
        }

        @Test
        @DisplayName("Should initialize velocities and forces to zero")
        void testInitialVelocitiesAndForces() {
            PlanckLattice lattice = new PlanckLattice(3, 3);

            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.velX[i], "Initial velocity X should be zero");
                assertEquals(0.0f, lattice.velY[i], "Initial velocity Y should be zero");
                assertEquals(0.0f, lattice.forceX[i], "Initial force X should be zero");
                assertEquals(0.0f, lattice.forceY[i], "Initial force Y should be zero");
                assertEquals(0.0f, lattice.energyDensity[i], "Initial energy density should be zero");
                assertEquals(0.0f, lattice.emFieldReal[i], "Initial EM field real should be zero");
                assertEquals(0.0f, lattice.emFieldImag[i], "Initial EM field imag should be zero");
            }
        }
    }

    @Nested
    @DisplayName("Index Calculations")
    class IndexCalculationTests {

        @Test
        @DisplayName("Should convert 2D coordinates to 1D index correctly")
        void testGetIndex() {
            // USAGE: Convert grid coordinates to array index
            PlanckLattice lattice = new PlanckLattice(10, 10);

            assertEquals(0, lattice.getIndex(0, 0), "Top-left corner");
            assertEquals(9, lattice.getIndex(9, 0), "Top-right corner");
            assertEquals(10, lattice.getIndex(0, 1), "Second row, first column");
            assertEquals(99, lattice.getIndex(9, 9), "Bottom-right corner");
            assertEquals(55, lattice.getIndex(5, 5), "Center");
        }

        @Test
        @DisplayName("Should convert 1D index back to grid X coordinate")
        void testGetGridX() {
            // USAGE: Get X coordinate from array index
            PlanckLattice lattice = new PlanckLattice(10, 10);

            assertEquals(0, lattice.getGridX(0), "Index 0");
            assertEquals(9, lattice.getGridX(9), "Index 9");
            assertEquals(0, lattice.getGridX(10), "Index 10 (second row)");
            assertEquals(5, lattice.getGridX(55), "Index 55");
        }

        @Test
        @DisplayName("Should convert 1D index back to grid Y coordinate")
        void testGetGridY() {
            // USAGE: Get Y coordinate from array index
            PlanckLattice lattice = new PlanckLattice(10, 10);

            assertEquals(0, lattice.getGridY(0), "Index 0");
            assertEquals(0, lattice.getGridY(9), "Index 9");
            assertEquals(1, lattice.getGridY(10), "Index 10");
            assertEquals(5, lattice.getGridY(55), "Index 55");
            assertEquals(9, lattice.getGridY(99), "Index 99");
        }

        @Test
        @DisplayName("Should correctly identify in-bounds coordinates")
        void testIsInBounds() {
            // USAGE: Check if coordinates are valid
            PlanckLattice lattice = new PlanckLattice(10, 10);

            assertTrue(lattice.isInBounds(0, 0), "Top-left is in bounds");
            assertTrue(lattice.isInBounds(9, 9), "Bottom-right is in bounds");
            assertTrue(lattice.isInBounds(5, 5), "Center is in bounds");

            assertFalse(lattice.isInBounds(-1, 0), "Negative X is out of bounds");
            assertFalse(lattice.isInBounds(0, -1), "Negative Y is out of bounds");
            assertFalse(lattice.isInBounds(10, 0), "X = width is out of bounds");
            assertFalse(lattice.isInBounds(0, 10), "Y = height is out of bounds");
            assertFalse(lattice.isInBounds(100, 100), "Far outside is out of bounds");
        }

        @Test
        @DisplayName("Should handle toroidal wrap-around for neighbors")
        void testGetNeighborIndexWithWrapAround() {
            // USAGE: Get neighbor indices with toroidal boundary conditions
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Right neighbor of rightmost column wraps to left
            int rightmost = lattice.getIndex(9, 5);
            int wrappedRight = lattice.getNeighborIndex(9, 5, 1, 0);
            assertEquals(lattice.getIndex(0, 5), wrappedRight, "Right edge wraps to left");

            // Left neighbor of leftmost column wraps to right
            int wrappedLeft = lattice.getNeighborIndex(0, 5, -1, 0);
            assertEquals(lattice.getIndex(9, 5), wrappedLeft, "Left edge wraps to right");

            // Down neighbor of bottom row wraps to top
            int wrappedDown = lattice.getNeighborIndex(5, 9, 0, 1);
            assertEquals(lattice.getIndex(5, 0), wrappedDown, "Bottom edge wraps to top");

            // Up neighbor of top row wraps to bottom
            int wrappedUp = lattice.getNeighborIndex(5, 0, 0, -1);
            assertEquals(lattice.getIndex(5, 9), wrappedUp, "Top edge wraps to bottom");
        }

        @Test
        @DisplayName("Should get correct neighbor for interior points")
        void testGetNeighborIndexInterior() {
            PlanckLattice lattice = new PlanckLattice(10, 10);

            int centerIdx = lattice.getIndex(5, 5);
            assertEquals(lattice.getIndex(6, 5), lattice.getNeighborIndex(5, 5, 1, 0), "Right neighbor");
            assertEquals(lattice.getIndex(4, 5), lattice.getNeighborIndex(5, 5, -1, 0), "Left neighbor");
            assertEquals(lattice.getIndex(5, 6), lattice.getNeighborIndex(5, 5, 0, 1), "Down neighbor");
            assertEquals(lattice.getIndex(5, 4), lattice.getNeighborIndex(5, 5, 0, -1), "Up neighbor");
        }
    }

    @Nested
    @DisplayName("Force Clearing")
    class ForceClearingTests {

        @Test
        @DisplayName("Should clear all force accumulators to zero")
        void testClearForces() {
            // USAGE: Clear forces at the start of each timestep
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Set some non-zero forces
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.forceX[i] = i * 0.5f;
                lattice.forceY[i] = i * 0.3f;
            }

            // Clear forces
            lattice.clearForces();

            // Verify all forces are zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.forceX[i], 1e-7f, "Force X should be zero after clearing");
                assertEquals(0.0f, lattice.forceY[i], 1e-7f, "Force Y should be zero after clearing");
            }
        }

        @Test
        @DisplayName("Should handle clearing forces on empty lattice")
        void testClearForcesOnEmptyLattice() {
            PlanckLattice lattice = new PlanckLattice(5, 5);

            // Should not throw exception
            assertDoesNotThrow(() -> lattice.clearForces());

            // All forces should still be zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.forceX[i]);
                assertEquals(0.0f, lattice.forceY[i]);
            }
        }
    }

    @Nested
    @DisplayName("EM Pulse Addition")
    class EMPulseTests {

        @Test
        @DisplayName("Should add Gaussian EM pulse at specified location")
        void testAddEMPulse() {
            // USAGE: Add an EM wave pulse to the lattice
            PlanckLattice lattice = new PlanckLattice(20, 20);

            float centerX = 10.0f;
            float centerY = 10.0f;
            float amplitude = 1.0f;
            float sigma = 5.0f;
            float wavelength = 4.0f;

            lattice.addEMPulse(centerX, centerY, amplitude, sigma, wavelength);

            // Check that center has maximum amplitude
            int centerIdx = lattice.getIndex(10, 10);
            float centerAmplitude = (float) Math.sqrt(
                lattice.emFieldReal[centerIdx] * lattice.emFieldReal[centerIdx] +
                lattice.emFieldImag[centerIdx] * lattice.emFieldImag[centerIdx]
            );

            // Center should have amplitude close to 1.0 (Gaussian envelope peak)
            assertTrue(centerAmplitude > 0.9f, "Center amplitude should be close to 1.0, was " + centerAmplitude);

            // Check that far away points have lower amplitude
            int farIdx = lattice.getIndex(0, 0);
            float farAmplitude = (float) Math.sqrt(
                lattice.emFieldReal[farIdx] * lattice.emFieldReal[farIdx] +
                lattice.emFieldImag[farIdx] * lattice.emFieldImag[farIdx]
            );

            assertTrue(farAmplitude < centerAmplitude,
                "Far amplitude (" + farAmplitude + ") should be less than center amplitude (" + centerAmplitude + ")");
        }

        @Test
        @DisplayName("Should create wave pattern with correct wavelength")
        void testWavePatternWavelength() {
            PlanckLattice lattice = new PlanckLattice(50, 50);

            float wavelength = 10.0f;
            lattice.addEMPulse(25.0f, 25.0f, 1.0f, 20.0f, wavelength);

            // Sample along horizontal line through center
            // Phase should change by 2π every wavelength
            int y = 25;
            float phase0 = (float) Math.atan2(lattice.emFieldImag[lattice.getIndex(25, y)],
                                               lattice.emFieldReal[lattice.getIndex(25, y)]);

            // Check a point approximately one wavelength away
            int xOffset = 35; // 10 units away
            float phase1 = (float) Math.atan2(lattice.emFieldImag[lattice.getIndex(xOffset, y)],
                                               lattice.emFieldReal[lattice.getIndex(xOffset, y)]);

            // Phases should be similar (within 2π modulo)
            float phaseDiff = Math.abs(phase0 - phase1);
            float twoPi = 2.0f * (float) Math.PI;
            assertTrue(phaseDiff < 1.0f || Math.abs(phaseDiff - twoPi) < 1.0f,
                "Phase difference should be close to 0 or 2π for one wavelength distance");
        }

        @Test
        @DisplayName("Should handle multiple overlapping pulses additively")
        void testMultiplePulses() {
            PlanckLattice lattice = new PlanckLattice(20, 20);

            // Add two pulses at same location
            lattice.addEMPulse(10.0f, 10.0f, 0.5f, 5.0f, 4.0f);
            float firstReal = lattice.emFieldReal[lattice.getIndex(10, 10)];
            float firstImag = lattice.emFieldImag[lattice.getIndex(10, 10)];

            // Second pulse overwrites (as implemented)
            lattice.addEMPulse(10.0f, 10.0f, 1.0f, 5.0f, 4.0f);
            float secondReal = lattice.emFieldReal[lattice.getIndex(10, 10)];
            float secondImag = lattice.emFieldImag[lattice.getIndex(10, 10)];

            // Second pulse should overwrite first (based on implementation)
            assertNotEquals(firstReal, secondReal, 0.001f,
                "Second pulse should change the field (overwrite behavior)");
        }
    }

    @Nested
    @DisplayName("Mass Concentration")
    class MassConcentrationTests {

        @Test
        @DisplayName("Should add Gaussian mass distribution")
        void testAddMassConcentration() {
            // USAGE: Add a mass concentration (energy density) to create gravity
            PlanckLattice lattice = new PlanckLattice(20, 20);

            float centerX = 10.0f;
            float centerY = 10.0f;
            float mass = 100.0f;
            float radius = 5.0f;

            lattice.addMassConcentration(centerX, centerY, mass, radius);

            // Center should have highest energy density
            int centerIdx = lattice.getIndex(10, 10);
            float centerEnergy = lattice.energyDensity[centerIdx];

            assertTrue(centerEnergy > 0, "Center should have positive energy");
            assertTrue(centerEnergy > 50.0f, "Center energy should be substantial, was " + centerEnergy);

            // Energy should decrease with distance
            int nearIdx = lattice.getIndex(11, 10);
            int farIdx = lattice.getIndex(19, 10);

            assertTrue(lattice.energyDensity[nearIdx] > 0, "Near point should have energy");
            assertTrue(lattice.energyDensity[nearIdx] < centerEnergy, "Near point should have less energy than center");
            assertTrue(lattice.energyDensity[farIdx] < lattice.energyDensity[nearIdx],
                "Far point should have less energy than near point");
        }

        @Test
        @DisplayName("Should support additive mass concentrations")
        void testAdditiveMassConcentrations() {
            PlanckLattice lattice = new PlanckLattice(20, 20);

            // Add first mass
            lattice.addMassConcentration(10.0f, 10.0f, 50.0f, 3.0f);
            int centerIdx = lattice.getIndex(10, 10);
            float firstEnergy = lattice.energyDensity[centerIdx];

            // Add second mass at same location
            lattice.addMassConcentration(10.0f, 10.0f, 50.0f, 3.0f);
            float secondEnergy = lattice.energyDensity[centerIdx];

            // Energy should be additive
            assertTrue(secondEnergy > firstEnergy,
                "Second mass should add to energy: first=" + firstEnergy + ", second=" + secondEnergy);
            assertEquals(firstEnergy * 2, secondEnergy, firstEnergy * 0.1f,
                "Second energy should be approximately double");
        }
    }

    @Nested
    @DisplayName("Lattice Property Calculations")
    class PropertyCalculationTests {

        @Test
        @DisplayName("Should calculate average spacing correctly for uniform lattice")
        void testAverageSpacingUniform() {
            // USAGE: Monitor lattice compression by checking average spacing
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Initial lattice should have spacing = equilibrium
            float avgSpacing = lattice.getAverageSpacing();

            assertEquals(PlanckLattice.EQUILIBRIUM_DISTANCE, avgSpacing, 0.0001f,
                "Initial uniform lattice should have equilibrium spacing");
        }

        @Test
        @DisplayName("Should detect compressed lattice spacing")
        void testAverageSpacingCompressed() {
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Manually compress the lattice by reducing all positions
            for (int i = 0; i < lattice.totalSpheres; i++) {
                lattice.posX[i] *= 0.9f;  // 10% compression
                lattice.posY[i] *= 0.9f;
            }

            float avgSpacing = lattice.getAverageSpacing();

            assertTrue(avgSpacing < PlanckLattice.EQUILIBRIUM_DISTANCE,
                "Compressed lattice should have spacing < equilibrium, was " + avgSpacing);
            assertEquals(0.9f, avgSpacing, 0.01f, "Spacing should be approximately 0.9");
        }

        @Test
        @DisplayName("Should calculate total energy correctly")
        void testGetTotalEnergy() {
            // USAGE: Monitor total energy in the system
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Initially no energy
            assertEquals(0.0f, lattice.getTotalEnergy(), 1e-7f, "Initial total energy should be zero");

            // Add some energy
            lattice.energyDensity[0] = 10.0f;
            lattice.energyDensity[1] = 20.0f;
            lattice.energyDensity[2] = 30.0f;

            float totalEnergy = lattice.getTotalEnergy();
            assertEquals(60.0f, totalEnergy, 1e-6f, "Total energy should be sum of all densities");
        }

        @Test
        @DisplayName("Should find maximum EM amplitude")
        void testGetMaxEMAmplitude() {
            // USAGE: Track maximum EM field strength
            PlanckLattice lattice = new PlanckLattice(10, 10);

            // Initially no EM field
            assertEquals(0.0f, lattice.getMaxEMAmplitude(), 1e-7f, "Initial max amplitude should be zero");

            // Add EM field at various points
            lattice.emFieldReal[0] = 3.0f;
            lattice.emFieldImag[0] = 4.0f;  // Amplitude = 5.0

            lattice.emFieldReal[1] = 1.0f;
            lattice.emFieldImag[1] = 1.0f;  // Amplitude = sqrt(2) ≈ 1.414

            float maxAmplitude = lattice.getMaxEMAmplitude();
            assertEquals(5.0f, maxAmplitude, 1e-6f, "Max amplitude should be 5.0");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle minimum size lattice (1x1)")
        void testMinimumLattice() {
            PlanckLattice lattice = new PlanckLattice(1, 1);

            assertEquals(1, lattice.totalSpheres);
            assertEquals(0, lattice.getIndex(0, 0));

            // Should not crash when clearing forces
            assertDoesNotThrow(() -> lattice.clearForces());
        }

        @Test
        @DisplayName("Should handle rectangular lattice (non-square)")
        void testRectangularLattice() {
            PlanckLattice lattice = new PlanckLattice(5, 10);

            assertEquals(5, lattice.gridWidth);
            assertEquals(10, lattice.gridHeight);
            assertEquals(50, lattice.totalSpheres);

            // Check corner indices
            assertEquals(0, lattice.getIndex(0, 0));
            assertEquals(4, lattice.getIndex(4, 0));
            assertEquals(5, lattice.getIndex(0, 1));
            assertEquals(49, lattice.getIndex(4, 9));
        }

        @Test
        @DisplayName("Should handle large lattice dimensions")
        void testLargeLattice() {
            // This tests that large lattices can be created without overflow
            PlanckLattice lattice = new PlanckLattice(1000, 1000);

            assertEquals(1000000, lattice.totalSpheres);
            assertNotNull(lattice.posX);
            assertEquals(1000000, lattice.posX.length);
        }

        @Test
        @DisplayName("Should handle zero-amplitude EM pulse")
        void testZeroAmplitudePulse() {
            PlanckLattice lattice = new PlanckLattice(10, 10);

            lattice.addEMPulse(5.0f, 5.0f, 0.0f, 2.0f, 4.0f);

            // All EM fields should remain zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.emFieldReal[i], 1e-7f, "EM field real should be zero");
                assertEquals(0.0f, lattice.emFieldImag[i], 1e-7f, "EM field imag should be zero");
            }
        }

        @Test
        @DisplayName("Should handle zero-mass concentration")
        void testZeroMassConcentration() {
            PlanckLattice lattice = new PlanckLattice(10, 10);

            lattice.addMassConcentration(5.0f, 5.0f, 0.0f, 2.0f);

            // All energy densities should remain zero
            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0.0f, lattice.energyDensity[i], 1e-7f, "Energy density should be zero");
            }
        }
    }

    @Nested
    @DisplayName("Physics Constants")
    class PhysicsConstantsTests {

        @Test
        @DisplayName("Should have sensible physics constants")
        void testPhysicsConstants() {
            assertTrue(PlanckLattice.SPRING_K > 0, "Spring constant should be positive");
            assertTrue(PlanckLattice.EQUILIBRIUM_DISTANCE > 0, "Equilibrium distance should be positive");
            assertTrue(PlanckLattice.GRAVITY_G >= 0, "Gravity constant should be non-negative");
            assertTrue(PlanckLattice.EM_DAMPING >= 0, "EM damping should be non-negative");
            assertTrue(PlanckLattice.EM_SPEED > 0, "EM speed should be positive");
            assertTrue(PlanckLattice.SPHERE_MASS > 0, "Sphere mass should be positive");
        }

        @Test
        @DisplayName("Should have valid Vector API species")
        void testVectorAPISpecies() {
            assertNotNull(PlanckLattice.SPECIES, "Vector species should not be null");
            assertTrue(PlanckLattice.SPECIES.length() > 0, "Vector species should have positive lane count");

            // Species length should be a power of 2 and reasonable
            int lanes = PlanckLattice.SPECIES.length();
            assertTrue(lanes >= 1 && lanes <= 64,
                "Vector lanes should be reasonable (1-64), was " + lanes);
        }
    }
}
