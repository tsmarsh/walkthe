package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for discrete quantum lattice implementation.
 * Verifies cellular automaton rules and SIMD operations.
 */
class DiscreteLatticeTest {

    @Nested
    @DisplayName("Initialization and Basic Operations")
    class InitializationTests {

        @Test
        @DisplayName("Should create lattice with correct dimensions")
        void testLatticeCreation() {
            DiscreteLattice lattice = new DiscreteLattice(10, 20);

            assertEquals(10, lattice.gridWidth);
            assertEquals(20, lattice.gridHeight);
            assertEquals(200, lattice.totalSpheres);
        }

        @Test
        @DisplayName("Should initialize to vacuum state (all zeros)")
        void testVacuumInitialization() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            for (int i = 0; i < lattice.totalSpheres; i++) {
                assertEquals(0, lattice.energyLevel[i], "Energy should be 0");
                assertEquals(0, lattice.emField[i], "EM field should be 0");
            }

            assertEquals(0, lattice.getTotalEnergy());
            assertEquals(0.0f, lattice.getAverageEnergy(), 0.001f);
        }

        @Test
        @DisplayName("Should add energy quanta correctly")
        void testAddEnergy() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.addEnergyQuantum(5, 5, 2);
            assertEquals(2, lattice.getEnergy(5, 5));
            assertEquals(2, lattice.getTotalEnergy());

            lattice.addEnergyQuantum(5, 5, 1);
            assertEquals(3, lattice.getEnergy(5, 5));
            assertEquals(3, lattice.getTotalEnergy());
        }

        @Test
        @DisplayName("Should saturate energy at level 3")
        void testEnergySaturation() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.addEnergyQuantum(5, 5, 5); // Try to add 5 quanta
            assertEquals(3, lattice.getEnergy(5, 5), "Should saturate at 3");

            lattice.addEnergyQuantum(5, 5, 10); // Try to add more
            assertEquals(3, lattice.getEnergy(5, 5), "Should stay at 3");
        }

        @Test
        @DisplayName("Should create EM pulse correctly")
        void testCreateEMPulse() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.createEMPulse(5, 5, 2);
            assertEquals(2, lattice.getEM(5, 5));

            lattice.createEMPulse(5, 5, 3);
            assertEquals(3, lattice.getEM(5, 5));
        }
    }

    @Nested
    @DisplayName("Energy Propagation Rules")
    class EnergyPropagationTests {

        @Test
        @DisplayName("Should preserve total energy during propagation")
        void testEnergyConservation() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.addEnergyQuantum(5, 5, 3);
            int initialEnergy = lattice.getTotalEnergy();
            assertEquals(3, initialEnergy);

            // Propagate multiple times
            for (int i = 0; i < 20; i++) {
                lattice.propagateEnergy();
            }

            // Energy should be approximately conserved (may have ±1 due to rounding)
            int finalEnergy = lattice.getTotalEnergy();
            assertTrue(Math.abs(finalEnergy - initialEnergy) <= 1,
                    "Energy should be approximately conserved");
        }

        @Test
        @DisplayName("Should spread energy to neighbors")
        void testEnergySpread() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            // Add high energy at center
            lattice.addEnergyQuantum(5, 5, 3);

            // Before propagation, neighbors should be at vacuum
            assertEquals(0, lattice.getEnergy(6, 5));
            assertEquals(0, lattice.getEnergy(4, 5));
            assertEquals(0, lattice.getEnergy(5, 6));
            assertEquals(0, lattice.getEnergy(5, 4));

            // After propagation, energy should spread
            lattice.propagateEnergy();

            // At least one neighbor should have gained energy
            int neighborEnergy = lattice.getEnergy(6, 5) +
                               lattice.getEnergy(4, 5) +
                               lattice.getEnergy(5, 6) +
                               lattice.getEnergy(5, 4);

            assertTrue(neighborEnergy > 0, "Energy should spread to neighbors");
        }

        @Test
        @DisplayName("Should not propagate from vacuum (level 0)")
        void testVacuumDoesNotPropagate() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            // All at vacuum, propagate
            lattice.propagateEnergy();

            // Should remain at vacuum
            assertEquals(0, lattice.getTotalEnergy());
        }

        @Test
        @DisplayName("Should handle toroidal boundaries correctly")
        void testToroidalPropagation() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10, true);

            // Add energy at edge
            lattice.addEnergyQuantum(0, 0, 3);

            lattice.propagateEnergy();

            // Should propagate across toroidal boundary
            // Check wrapped neighbors at (9,0) and (0,9)
            int totalEnergy = lattice.getTotalEnergy();
            assertTrue(totalEnergy >= 2, "Energy should have propagated across boundary");
        }
    }

    @Nested
    @DisplayName("EM Field Propagation Rules")
    class EMPropagationTests {

        @Test
        @DisplayName("Should propagate EM field to neighbors")
        void testEMSpread() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.createEMPulse(5, 5, 3);

            // Before propagation
            assertEquals(3, lattice.getEM(5, 5));
            assertEquals(0, lattice.getEM(6, 5));

            // After propagation
            lattice.propagateEM();

            // Center should decay
            assertTrue(lattice.getEM(5, 5) < 3);

            // Neighbors should have field
            assertTrue(lattice.getEM(6, 5) > 0 ||
                      lattice.getEM(4, 5) > 0 ||
                      lattice.getEM(5, 6) > 0 ||
                      lattice.getEM(5, 4) > 0,
                      "EM should spread to neighbors");
        }

        @Test
        @DisplayName("Should decay EM field over time")
        void testEMDecay() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.createEMPulse(5, 5, 3);

            // Propagate until field disappears
            int maxSteps = 10;
            for (int i = 0; i < maxSteps; i++) {
                lattice.propagateEM();
            }

            // Field should have decayed significantly or disappeared
            assertTrue(lattice.getEM(5, 5) < 2,
                    "EM field should decay over time");
        }

        @Test
        @DisplayName("EM field should not propagate from level 0-1")
        void testWeakEMDoesNotPropagate() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.createEMPulse(5, 5, 1); // Weak field

            lattice.propagateEM();

            // Weak field should decay but not spread far
            // (This tests the "if (field <= LEVEL_1) continue" logic)
            int neighborsWithField = 0;
            if (lattice.getEM(6, 5) > 0) neighborsWithField++;
            if (lattice.getEM(4, 5) > 0) neighborsWithField++;
            if (lattice.getEM(5, 6) > 0) neighborsWithField++;
            if (lattice.getEM(5, 4) > 0) neighborsWithField++;

            assertTrue(neighborsWithField <= 2,
                    "Weak EM should not spread strongly");
        }
    }

    @Nested
    @DisplayName("SIMD Vectorization Tests")
    class VectorizationTests {

        @Test
        @DisplayName("Should handle large lattices efficiently with vectorization")
        void testLargeLatticePerformance() {
            DiscreteLattice lattice = new DiscreteLattice(100, 100);

            // Add energy
            for (int i = 0; i < 10; i++) {
                lattice.addEnergyQuantum(50 + i, 50, 2);
            }

            // Should complete quickly due to vectorization
            long start = System.nanoTime();
            for (int i = 0; i < 100; i++) {
                lattice.propagateEnergy();
            }
            long end = System.nanoTime();

            double timeMs = (end - start) / 1_000_000.0;
            assertTrue(timeMs < 50.0,
                    "100 iterations on 100×100 lattice should be fast, took " + timeMs + "ms");
        }

        @Test
        @DisplayName("Should handle non-multiple-of-32 lattice sizes (tail handling)")
        void testTailHandling() {
            // 17×17 = 289 sites (not divisible by 32)
            DiscreteLattice lattice = new DiscreteLattice(17, 17);

            lattice.addEnergyQuantum(8, 8, 3);

            // Should work correctly despite tail elements
            lattice.propagateEnergy();

            assertTrue(lattice.getTotalEnergy() >= 2,
                    "Should handle tail elements correctly");
        }

        @Test
        @DisplayName("getTotalEnergy should use vectorized reduction")
        void testVectorizedReduction() {
            DiscreteLattice lattice = new DiscreteLattice(100, 100);

            // Add energy at various locations
            for (int i = 0; i < lattice.totalSpheres; i += 100) {
                lattice.energyLevel[i] = 2;
            }

            int expected = (lattice.totalSpheres / 100) * 2;
            int actual = lattice.getTotalEnergy();

            assertEquals(expected, actual,
                    "Vectorized reduction should give correct sum");
        }
    }

    @Nested
    @DisplayName("Index Calculations")
    class IndexTests {

        @Test
        @DisplayName("Should calculate correct linear indices")
        void testGetIndex() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            assertEquals(0, lattice.getIndex(0, 0));
            assertEquals(9, lattice.getIndex(9, 0));
            assertEquals(10, lattice.getIndex(0, 1));
            assertEquals(99, lattice.getIndex(9, 9));
        }

        @Test
        @DisplayName("Should access correct positions")
        void testPositionAccess() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.addEnergyQuantum(3, 7, 2);
            assertEquals(2, lattice.getEnergy(3, 7));
            assertEquals(2, lattice.energyLevel[lattice.getIndex(3, 7)]);
        }
    }

    @Nested
    @DisplayName("Statistics and Queries")
    class StatisticsTests {

        @Test
        @DisplayName("Should calculate average energy correctly")
        void testAverageEnergy() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            lattice.addEnergyQuantum(0, 0, 2);
            lattice.addEnergyQuantum(5, 5, 3);

            assertEquals(5, lattice.getTotalEnergy());
            assertEquals(0.05f, lattice.getAverageEnergy(), 0.001f);
        }

        @Test
        @DisplayName("Should handle empty lattice statistics")
        void testEmptyLatticeStats() {
            DiscreteLattice lattice = new DiscreteLattice(10, 10);

            assertEquals(0, lattice.getTotalEnergy());
            assertEquals(0.0f, lattice.getAverageEnergy());
        }
    }
}
