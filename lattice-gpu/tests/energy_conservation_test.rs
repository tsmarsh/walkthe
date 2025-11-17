use lattice_gpu::*;

#[test]
fn test_energy_conservation_small_lattice() {
    let mut lattice = pollster::block_on(DiscreteLatticeGPU::new(20, 20, 20));
    lattice.initialize_vacuum();

    // Add energy at center
    lattice.add_energy_quantum(10, 10, 10, 3);

    let initial_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(initial_energy, 3, "Initial energy should be 3 quanta");

    // Propagate 50 steps
    for _ in 0..50 {
        lattice.propagate_energy();
    }

    let final_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(
        final_energy, initial_energy,
        "Energy must be conserved: started with {}, ended with {}",
        initial_energy, final_energy
    );
}

#[test]
fn test_energy_conservation_multiple_sources() {
    let mut lattice = pollster::block_on(DiscreteLatticeGPU::new(30, 30, 30));
    lattice.initialize_vacuum();

    // Add energy at multiple locations
    lattice.add_energy_quantum(10, 10, 10, 3);
    lattice.add_energy_quantum(20, 20, 20, 2);
    lattice.add_energy_quantum(15, 15, 15, 1);

    let initial_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(initial_energy, 6, "Initial energy should be 6 quanta");

    // Propagate 100 steps
    for _ in 0..100 {
        lattice.propagate_energy();
    }

    let final_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(
        final_energy, initial_energy,
        "Energy must be conserved with multiple sources"
    );
}

#[test]
fn test_vacuum_stays_vacuum() {
    let mut lattice = pollster::block_on(DiscreteLatticeGPU::new(20, 20, 20));
    lattice.initialize_vacuum();

    let initial_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(initial_energy, 0, "Vacuum should have zero energy");

    // Propagate vacuum - should stay at zero
    for _ in 0..50 {
        lattice.propagate_energy();
    }

    let final_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(final_energy, 0, "Vacuum should remain at zero energy");
}

#[test]
fn test_large_lattice_conservation() {
    let mut lattice = pollster::block_on(DiscreteLatticeGPU::new(100, 100, 100));
    lattice.initialize_vacuum();

    // Add spherical distribution
    let c = 50;
    for dz in -3i32..=3 {
        for dy in -3i32..=3 {
            for dx in -3i32..=3 {
                if dx * dx + dy * dy + dz * dz <= 9 {
                    lattice.add_energy_quantum(
                        (c + dx) as u32,
                        (c + dy) as u32,
                        (c + dz) as u32,
                        3,
                    );
                }
            }
        }
    }

    let initial_energy = pollster::block_on(lattice.get_total_energy());
    assert!(initial_energy > 0, "Should have non-zero initial energy");

    // Propagate many steps
    for _ in 0..100 {
        lattice.propagate_energy();
    }

    let final_energy = pollster::block_on(lattice.get_total_energy());
    assert_eq!(
        final_energy, initial_energy,
        "Energy must be conserved in large lattice"
    );
}
