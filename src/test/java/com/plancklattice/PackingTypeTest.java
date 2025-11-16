package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PackingType enum - lattice packing configurations.
 * These tests document the classification system and verify energy bonuses.
 *
 * Purpose: PackingType classifies local lattice configurations by coordination number
 * and assigns stability bonuses. This is critical for annealing physics.
 */
@DisplayName("PackingType - Lattice packing classification")
class PackingTypeTest {

    @Nested
    @DisplayName("Coordination Number Classification")
    class CoordinationTests {

        @Test
        @DisplayName("Should classify 4 neighbors as square 2D packing")
        void testSquare2D() {
            // USAGE: 4-neighbor configuration is square lattice in 2D
            PackingType type = PackingType.fromCoordination(4);

            assertEquals(PackingType.SQUARE_2D, type,
                "4 neighbors should be classified as square 2D packing");
            assertEquals(4, type.getIdealCoordination(),
                "Square 2D has 4 ideal neighbors");
        }

        @Test
        @DisplayName("Should classify 6 neighbors as hexagonal 2D packing")
        void testHexagonal2D() {
            // USAGE: 6-neighbor configuration is optimal 2D packing
            PackingType type = PackingType.fromCoordination(6);

            assertEquals(PackingType.HEXAGONAL_2D, type,
                "6 neighbors should be classified as hexagonal 2D");
            assertEquals(6, type.getIdealCoordination(),
                "Hexagonal 2D has 6 ideal neighbors");
        }

        @Test
        @DisplayName("Should classify 8 neighbors as body-centered packing")
        void testBodyCentered() {
            // USAGE: 8 neighbors indicates body-centered cubic structure
            PackingType type = PackingType.fromCoordination(8);

            assertEquals(PackingType.BODY_CENTERED, type,
                "8 neighbors should be body-centered");
            assertEquals(8, type.getIdealCoordination());
        }

        @Test
        @DisplayName("Should classify 12 neighbors as FCC (most dense)")
        void testFCC() {
            // USAGE: 12 neighbors is densest sphere packing (FCC or HCP)
            PackingType type = PackingType.fromCoordination(12);

            assertEquals(PackingType.FCC, type,
                "12 neighbors should be FCC (face-centered cubic)");
            assertEquals(12, type.getIdealCoordination());
        }

        @Test
        @DisplayName("Should classify unusual coordination as CUSTOM")
        void testCustomCoordination() {
            // ERROR CASE: Non-standard coordination numbers
            PackingType type1 = PackingType.fromCoordination(3);
            PackingType type2 = PackingType.fromCoordination(7);
            PackingType type3 = PackingType.fromCoordination(99);

            assertEquals(PackingType.CUSTOM, type1, "3 neighbors -> CUSTOM");
            assertEquals(PackingType.CUSTOM, type2, "7 neighbors -> CUSTOM");
            assertEquals(PackingType.CUSTOM, type3, "99 neighbors -> CUSTOM");
        }

        @Test
        @DisplayName("Should handle zero coordination")
        void testZeroCoordination() {
            // ERROR CASE: Isolated sphere with no neighbors
            PackingType type = PackingType.fromCoordination(0);

            assertEquals(PackingType.CUSTOM, type,
                "0 neighbors (isolated sphere) should be CUSTOM");
        }

        @Test
        @DisplayName("Should handle negative coordination gracefully")
        void testNegativeCoordination() {
            // ERROR CASE: Invalid negative input
            PackingType type = PackingType.fromCoordination(-5);

            assertEquals(PackingType.CUSTOM, type,
                "Negative coordination should default to CUSTOM");
        }
    }

    @Nested
    @DisplayName("Energy Bonus System")
    class EnergyTests {

        @Test
        @DisplayName("Hexagonal 2D should have strong negative energy (most stable 2D)")
        void testHexagonalEnergy() {
            // PHYSICS: Hexagonal packing is optimal in 2D, should have lowest energy
            float energy = PackingType.HEXAGONAL_2D.getEnergyBonus();

            assertTrue(energy < 0, "Stable packings have negative energy bonus");
            assertEquals(-0.8f, energy, 0.001f,
                "Hexagonal 2D should have -0.8 energy bonus");
        }

        @Test
        @DisplayName("FCC should have strongest negative energy (most stable 3D)")
        void testFCCEnergy() {
            // PHYSICS: FCC/HCP are densest sphere packings in 3D
            float energy = PackingType.FCC.getEnergyBonus();

            assertTrue(energy < 0, "FCC is very stable");
            assertEquals(-1.0f, energy, 0.001f,
                "FCC should have -1.0 energy bonus (most stable)");
        }

        @Test
        @DisplayName("Body-centered should be moderately stable")
        void testBodyCenteredEnergy() {
            float energy = PackingType.BODY_CENTERED.getEnergyBonus();

            assertTrue(energy < 0, "Body-centered is stable but not optimal");
            assertEquals(-0.5f, energy, 0.001f,
                "Body-centered should have -0.5 energy bonus");
        }

        @Test
        @DisplayName("Square 2D should be neutral (not particularly stable)")
        void testSquareEnergy() {
            float energy = PackingType.SQUARE_2D.getEnergyBonus();

            assertEquals(0.0f, energy, 0.001f,
                "Square packing is neutral (neither stable nor unstable)");
        }

        @Test
        @DisplayName("Simple cubic should be neutral")
        void testSimpleCubicEnergy() {
            float energy = PackingType.SIMPLE_CUBIC.getEnergyBonus();

            assertEquals(0.0f, energy, 0.001f,
                "Simple cubic is neutral baseline");
        }

        @Test
        @DisplayName("Custom packing should have positive energy (unstable)")
        void testCustomEnergy() {
            // PHYSICS: Non-standard packings are energetically unfavorable
            float energy = PackingType.CUSTOM.getEnergyBonus();

            assertTrue(energy > 0, "Custom/unusual packings should be penalized");
            assertEquals(0.5f, energy, 0.001f,
                "Custom packing should have +0.5 energy penalty");
        }

        @Test
        @DisplayName("Energy ordering should reflect stability hierarchy")
        void testEnergyHierarchy() {
            // PHYSICS: Most stable to least stable ordering
            float fcc = PackingType.FCC.getEnergyBonus();
            float hex = PackingType.HEXAGONAL_2D.getEnergyBonus();
            float bcc = PackingType.BODY_CENTERED.getEnergyBonus();
            float square = PackingType.SQUARE_2D.getEnergyBonus();
            float custom = PackingType.CUSTOM.getEnergyBonus();

            assertTrue(fcc < hex, "FCC more stable than hexagonal 2D");
            assertTrue(hex < bcc, "Hexagonal 2D more stable than body-centered");
            assertTrue(bcc < square, "Body-centered more stable than square");
            assertTrue(square < custom, "Square more stable than custom");
        }
    }

    @Nested
    @DisplayName("Ideal Coordination Numbers")
    class IdealCoordinationTests {

        @Test
        @DisplayName("Each packing type should have correct ideal coordination")
        void testAllIdealCoordinations() {
            assertEquals(6, PackingType.SIMPLE_CUBIC.getIdealCoordination());
            assertEquals(12, PackingType.FCC.getIdealCoordination());
            assertEquals(12, PackingType.HCP.getIdealCoordination());
            assertEquals(8, PackingType.BODY_CENTERED.getIdealCoordination());
            assertEquals(6, PackingType.HEXAGONAL_2D.getIdealCoordination());
            assertEquals(4, PackingType.SQUARE_2D.getIdealCoordination());
            assertEquals(0, PackingType.CUSTOM.getIdealCoordination(),
                "Custom has no defined ideal coordination");
        }

        @Test
        @DisplayName("FCC and HCP should both have 12 neighbors (different stacking)")
        void testFCCvsHCP() {
            // DOCUMENTATION: FCC and HCP are both 12-neighbor packings
            // They differ in stacking sequence (ABC vs AB), not coordination
            assertEquals(12, PackingType.FCC.getIdealCoordination());
            assertEquals(12, PackingType.HCP.getIdealCoordination());
            assertEquals(PackingType.FCC.getEnergyBonus(),
                        PackingType.HCP.getEnergyBonus(),
                        "FCC and HCP should have same energy (both optimal)");
        }
    }

    @Nested
    @DisplayName("Classification Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle boundary coordination numbers consistently")
        void testBoundaryValues() {
            // Test around classification boundaries
            assertEquals(PackingType.CUSTOM, PackingType.fromCoordination(1));
            assertEquals(PackingType.CUSTOM, PackingType.fromCoordination(2));
            assertEquals(PackingType.CUSTOM, PackingType.fromCoordination(3));
            assertEquals(PackingType.SQUARE_2D, PackingType.fromCoordination(4));
            assertEquals(PackingType.CUSTOM, PackingType.fromCoordination(5));
            assertEquals(PackingType.HEXAGONAL_2D, PackingType.fromCoordination(6));
            assertEquals(PackingType.CUSTOM, PackingType.fromCoordination(7));
            assertEquals(PackingType.BODY_CENTERED, PackingType.fromCoordination(8));
        }

        @Test
        @DisplayName("Should classify very high coordination as CUSTOM")
        void testHighCoordination() {
            // ERROR CASE: Unrealistic number of neighbors
            PackingType type = PackingType.fromCoordination(1000);

            assertEquals(PackingType.CUSTOM, type,
                "Very high coordination should be CUSTOM");
        }
    }
}
