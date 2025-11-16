package com.plancklattice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParticlePattern - represents emergent stable structures.
 * These tests document particle lifecycle and stability tracking.
 *
 * Purpose: ParticlePattern tracks spheres that have annealed into stable
 * resonance patterns. This is the core of "particle formation" in the simulation.
 */
@DisplayName("ParticlePattern - Emergent particle structures")
class ParticlePatternTest {

    private PlanckLattice lattice;

    @BeforeEach
    void setUp() {
        lattice = new PlanckLattice(10, 10);
    }

    @Nested
    @DisplayName("Construction and Basic Properties")
    class ConstructionTests {

        @Test
        @DisplayName("Should create particle with basic properties")
        void testParticleCreation() {
            // USAGE: Create a particle pattern
            Set<Integer> spheres = Set.of(10, 11, 12, 20, 21, 22);
            ParticlePattern particle = new ParticlePattern(
                1,           // id
                11,          // center index
                spheres,     // participating spheres
                25.5f,       // total energy
                8.2f,        // stability
                PackingType.HEXAGONAL_2D
            );

            assertEquals(1, particle.getId());
            assertEquals(11, particle.getCenterIndex());
            assertEquals(6, particle.getSize());
            assertEquals(25.5f, particle.getTotalEnergy(), 0.001f);
            assertEquals(8.2f, particle.getStability(), 0.001f);
            assertEquals(PackingType.HEXAGONAL_2D, particle.getType());
            assertEquals(0, particle.getAge(), "New particle should have age 0");
        }

        @Test
        @DisplayName("Should copy sphere set defensively")
        void testDefensiveCopy() {
            // ERROR PREVENTION: Modifying original set shouldn't affect particle
            Set<Integer> original = new HashSet<>(Set.of(1, 2, 3));
            ParticlePattern particle = new ParticlePattern(
                1, 1, original, 10.0f, 5.0f, PackingType.FCC
            );

            // Modify original set
            original.add(999);

            Set<Integer> retrieved = particle.getParticipatingSpheres();
            assertEquals(3, retrieved.size(),
                "Particle should not be affected by changes to original set");
            assertFalse(retrieved.contains(999),
                "Added element should not appear in particle");
        }

        @Test
        @DisplayName("Should return defensive copy of sphere set on get")
        void testDefensiveGetCopy() {
            // ERROR PREVENTION: Modifying returned set shouldn't affect particle
            Set<Integer> spheres = Set.of(1, 2, 3);
            ParticlePattern particle = new ParticlePattern(
                1, 1, spheres, 10.0f, 5.0f, PackingType.FCC
            );

            Set<Integer> retrieved1 = particle.getParticipatingSpheres();
            retrieved1.add(999);  // Try to modify

            Set<Integer> retrieved2 = particle.getParticipatingSpheres();
            assertEquals(3, retrieved2.size(),
                "Particle internal state should be protected");
            assertFalse(retrieved2.contains(999),
                "Modifications to returned set should not persist");
        }

        @Test
        @DisplayName("Should handle single-sphere particle")
        void testSingleSphereParticle() {
            // EDGE CASE: Particle with just one sphere
            Set<Integer> spheres = Set.of(42);
            ParticlePattern particle = new ParticlePattern(
                1, 42, spheres, 5.0f, 2.0f, PackingType.CUSTOM
            );

            assertEquals(1, particle.getSize());
            assertEquals(42, particle.getCenterIndex());
        }

        @Test
        @DisplayName("Should handle large particle")
        void testLargeParticle() {
            // EDGE CASE: Particle with many spheres
            Set<Integer> spheres = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                spheres.add(i);
            }

            ParticlePattern particle = new ParticlePattern(
                1, 50, spheres, 500.0f, 50.0f, PackingType.FCC
            );

            assertEquals(100, particle.getSize());
        }
    }

    @Nested
    @DisplayName("Aging and Lifecycle")
    class AgingTests {

        @Test
        @DisplayName("Should increment age on each timestep")
        void testAgingIncrement() {
            // USAGE: Track particle lifetime
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            assertEquals(0, particle.getAge());

            particle.ageOneTimestep();
            assertEquals(1, particle.getAge());

            particle.ageOneTimestep();
            particle.ageOneTimestep();
            assertEquals(3, particle.getAge());
        }

        @Test
        @DisplayName("Should track energy stability over time")
        void testEnergyStabilityTracking() {
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            // Initially not stable (needs 10 timesteps)
            assertFalse(particle.isStable(),
                "Particle should not be stable immediately");

            // Energy stays constant -> becomes stable
            for (int i = 0; i < 15; i++) {
                particle.ageOneTimestep();
            }

            assertTrue(particle.isStable(),
                "Particle with constant energy should become stable after 10 steps");
        }

        @Test
        @DisplayName("Should reset stability count if energy changes significantly")
        void testStabilityResetOnEnergyChange() {
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            // Age to near stability
            for (int i = 0; i < 9; i++) {
                particle.ageOneTimestep();
            }

            // Change energy significantly
            particle.setTotalEnergy(20.0f);
            particle.ageOneTimestep();

            assertFalse(particle.isStable(),
                "Stability should reset when energy changes");
        }

        @Test
        @DisplayName("Should tolerate small energy fluctuations")
        void testEnergyFluctuationTolerance() {
            // PHYSICS: Small energy changes (< 0.01) don't break stability
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            for (int i = 0; i < 15; i++) {
                // Tiny energy fluctuation
                particle.setTotalEnergy(10.0f + (i % 2) * 0.005f);
                particle.ageOneTimestep();
            }

            assertTrue(particle.isStable(),
                "Small energy fluctuations should not prevent stability");
        }
    }

    @Nested
    @DisplayName("Self-Sustaining Criteria")
    class SelfSustainingTests {

        @Test
        @DisplayName("Should require minimum energy to be self-sustaining")
        void testMinimumEnergy() {
            // PHYSICS: Particles need energy to exist
            ParticlePattern lowEnergy = new ParticlePattern(
                1, 1, Set.of(1, 2), 0.5f, 10.0f, PackingType.FCC
            );

            for (int i = 0; i < 20; i++) {
                lowEnergy.ageOneTimestep();
            }

            assertFalse(lowEnergy.isSelfSustaining(),
                "Low energy particle cannot be self-sustaining (< 1.0)");
        }

        @Test
        @DisplayName("Should require minimum stability to be self-sustaining")
        void testMinimumStability() {
            // PHYSICS: Particles need energy barrier to persist
            ParticlePattern unstable = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 0.3f, PackingType.FCC
            );

            for (int i = 0; i < 20; i++) {
                unstable.ageOneTimestep();
            }

            assertFalse(unstable.isSelfSustaining(),
                "Unstable particle cannot be self-sustaining (stability < 0.5)");
        }

        @Test
        @DisplayName("Should require time stability to be self-sustaining")
        void testTimeStability() {
            // PHYSICS: Particle must persist before being "real"
            ParticlePattern young = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 10.0f, PackingType.FCC
            );

            // Only age a few steps
            young.ageOneTimestep();
            young.ageOneTimestep();

            assertFalse(young.isSelfSustaining(),
                "Young particle not yet stable over time");
        }

        @Test
        @DisplayName("Should be self-sustaining when all criteria met")
        void testSelfSustainingSuccess() {
            // USAGE: Fully formed particle
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2, 3), 15.0f, 8.0f, PackingType.HEXAGONAL_2D
            );

            // Age until stable
            for (int i = 0; i < 15; i++) {
                particle.ageOneTimestep();
            }

            assertTrue(particle.isSelfSustaining(),
                "Particle with energy > 1.0, stability > 0.5, and time-stable should be self-sustaining");
        }

        @Test
        @DisplayName("Should check all criteria independently")
        void testAllCriteriaMustBeMet() {
            // ERROR CASE: Each criterion is necessary
            ParticlePattern particle1 = new ParticlePattern(
                1, 1, Set.of(1, 2), 0.5f, 10.0f, PackingType.FCC);  // Low energy
            ParticlePattern particle2 = new ParticlePattern(
                2, 1, Set.of(1, 2), 10.0f, 0.3f, PackingType.FCC);  // Low stability
            ParticlePattern particle3 = new ParticlePattern(
                3, 1, Set.of(1, 2), 10.0f, 10.0f, PackingType.FCC); // Not aged

            // Age first two
            for (int i = 0; i < 20; i++) {
                particle1.ageOneTimestep();
                particle2.ageOneTimestep();
            }

            assertFalse(particle1.isSelfSustaining(), "Fails energy criterion");
            assertFalse(particle2.isSelfSustaining(), "Fails stability criterion");
            assertFalse(particle3.isSelfSustaining(), "Fails time-stability criterion");
        }
    }

    @Nested
    @DisplayName("Center of Mass Calculation")
    class CenterOfMassTests {

        @Test
        @DisplayName("Should calculate correct center of mass for symmetric pattern")
        void testSymmetricCenterOfMass() {
            // USAGE: Find geometric center of particle
            // Create 3x3 square centered at (5,5)
            Set<Integer> spheres = Set.of(
                lattice.getIndex(4, 4), lattice.getIndex(5, 4), lattice.getIndex(6, 4),
                lattice.getIndex(4, 5), lattice.getIndex(5, 5), lattice.getIndex(6, 5),
                lattice.getIndex(4, 6), lattice.getIndex(5, 6), lattice.getIndex(6, 6)
            );

            ParticlePattern particle = new ParticlePattern(
                1, lattice.getIndex(5, 5), spheres, 10.0f, 5.0f, PackingType.SQUARE_2D
            );

            float[] com = new float[2];
            particle.calculateCenterOfMass(lattice, com);

            assertEquals(5.0f, com[0], 0.001f, "COM X should be at center");
            assertEquals(5.0f, com[1], 0.001f, "COM Y should be at center");
        }

        @Test
        @DisplayName("Should handle single sphere (COM at that sphere)")
        void testSingleSphereCOM() {
            // EDGE CASE: One-sphere particle
            int index = lattice.getIndex(3, 7);
            Set<Integer> spheres = Set.of(index);

            ParticlePattern particle = new ParticlePattern(
                1, index, spheres, 5.0f, 2.0f, PackingType.CUSTOM
            );

            float[] com = new float[2];
            particle.calculateCenterOfMass(lattice, com);

            assertEquals(lattice.posX[index], com[0], 0.001f);
            assertEquals(lattice.posY[index], com[1], 0.001f);
        }

        @Test
        @DisplayName("Should calculate weighted average for asymmetric pattern")
        void testAsymmetricCenterOfMass() {
            // Two spheres: should be halfway between them
            int idx1 = lattice.getIndex(2, 2);
            int idx2 = lattice.getIndex(4, 2);
            Set<Integer> spheres = Set.of(idx1, idx2);

            ParticlePattern particle = new ParticlePattern(
                1, idx1, spheres, 10.0f, 5.0f, PackingType.CUSTOM
            );

            float[] com = new float[2];
            particle.calculateCenterOfMass(lattice, com);

            float expectedX = (lattice.posX[idx1] + lattice.posX[idx2]) / 2;
            float expectedY = (lattice.posY[idx1] + lattice.posY[idx2]) / 2;

            assertEquals(expectedX, com[0], 0.001f, "COM X is average of positions");
            assertEquals(expectedY, com[1], 0.001f, "COM Y is average of positions");
        }
    }

    @Nested
    @DisplayName("Property Modification")
    class PropertyModificationTests {

        @Test
        @DisplayName("Should allow energy modification")
        void testEnergyModification() {
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            particle.setTotalEnergy(25.0f);
            assertEquals(25.0f, particle.getTotalEnergy(), 0.001f);
        }

        @Test
        @DisplayName("Should allow stability modification")
        void testStabilityModification() {
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            particle.setStability(12.5f);
            assertEquals(12.5f, particle.getStability(), 0.001f);
        }

        @Test
        @DisplayName("Should allow packing type modification")
        void testPackingTypeModification() {
            ParticlePattern particle = new ParticlePattern(
                1, 1, Set.of(1, 2), 10.0f, 5.0f, PackingType.FCC
            );

            particle.setType(PackingType.HEXAGONAL_2D);
            assertEquals(PackingType.HEXAGONAL_2D, particle.getType());
        }
    }

    @Nested
    @DisplayName("String Representation")
    class ToStringTests {

        @Test
        @DisplayName("Should produce informative toString")
        void testToString() {
            // DOCUMENTATION: toString for debugging
            ParticlePattern particle = new ParticlePattern(
                42, 100, Set.of(100, 101, 102, 110, 111, 112),
                15.75f, 8.25f, PackingType.HEXAGONAL_2D
            );

            for (int i = 0; i < 10; i++) {
                particle.ageOneTimestep();
            }

            String str = particle.toString();

            assertTrue(str.contains("42"), "Should include particle ID");
            assertTrue(str.contains("100"), "Should include center index");
            assertTrue(str.contains("6"), "Should include size");
            assertTrue(str.contains("15.75") || str.contains("15.7"),
                "Should include energy");
            assertTrue(str.contains("8.25") || str.contains("8.2"),
                "Should include stability");
            assertTrue(str.contains("HEXAGONAL_2D"), "Should include packing type");
            assertTrue(str.contains("10"), "Should include age");
        }
    }
}
