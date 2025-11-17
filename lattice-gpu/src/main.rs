use lattice_gpu::DiscreteLatticeGPU;
use std::time::Instant;

fn main() {
    env_logger::init();
    println!("=== GPU-Accelerated 3D Discrete Quantum Lattice ===\n");

    // Test different lattice sizes - up to driver limit (2GB per buffer)
    // Max theoretical: 812³ (536M sites = 2.14 GB)
    let test_configs = vec![
        (200, 100), // 8M sites, 100 iterations
        (300, 100), // 27M sites, 100 iterations
        (400, 100), // 64M sites, 100 iterations
        (500, 100), // 125M sites, 100 iterations
        (600, 50),  // 216M sites, 50 iterations
        (700, 50),  // 343M sites, 50 iterations - pushing the limit!
    ];

    for (size, iterations) in test_configs {
        let total_sites = size * size * size;
        let data_size_mb = (total_sites * 4) as f64 / (1024.0 * 1024.0);

        println!(
            "=== {}³ Lattice ({} sites, {:.1} MB) ===\n",
            size, total_sites, data_size_mb
        );

        let mut lattice = pollster::block_on(DiscreteLatticeGPU::new(size, size, size));
        lattice.initialize_vacuum();

        // Add spherical energy distribution
        let c = size / 2;
        for dz in -3i32..=3 {
            for dy in -3i32..=3 {
                for dx in -3i32..=3 {
                    if dx * dx + dy * dy + dz * dz <= 9 {
                        lattice.add_energy_quantum(
                            (c as i32 + dx) as u32,
                            (c as i32 + dy) as u32,
                            (c as i32 + dz) as u32,
                            3,
                        );
                    }
                }
            }
        }

        let initial_energy = pollster::block_on(lattice.get_total_energy());
        println!("Initial energy: {} quanta\n", initial_energy);

        // Warmup
        for _ in 0..10 {
            lattice.propagate_energy();
        }
        // Wait for GPU to finish warmup
        pollster::block_on(async {
            lattice.get_total_energy().await;
        });

        // Benchmark
        let start = Instant::now();
        for _ in 0..iterations {
            lattice.propagate_energy();
        }
        // Wait for GPU to finish all work
        let final_energy = pollster::block_on(lattice.get_total_energy());
        let elapsed = start.elapsed();

        let time_ms = elapsed.as_secs_f64() * 1000.0;
        let avg_ms = time_ms / iterations as f64;
        let throughput = total_sites as f64 * iterations as f64 / elapsed.as_secs_f64();

        println!("GPU Performance:");
        println!(
            "  Total time: {:.2} ms for {} iterations",
            time_ms, iterations
        );
        println!("  Per iteration: {:.3} ms", avg_ms);
        println!("  Throughput: {:.2e} sites/sec", throughput);
        println!("  GB/sec (read+write): {:.2}", (throughput * 8.0) / 1e9);
        println!("  Final energy: {} quanta", final_energy);

        if final_energy != initial_energy {
            println!("  ⚠ Energy drift: {} -> {}", initial_energy, final_energy);
        } else {
            println!("  ✓ Energy conserved");
        }

        println!("\n{}\n", "=".repeat(60));
    }

    println!("GPU compute complete!");
    println!("\nNote: GPU runs MILLIONS of threads in parallel");
    println!("      Each site computed simultaneously");
    println!("      Expect 100-1000x speedup vs CPU");
}
