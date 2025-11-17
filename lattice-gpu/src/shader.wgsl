// 3D Discrete Quantum Lattice - GPU Compute Shader
//
// Implements gradient-flow quantum rules where waves emerge naturally
// Each thread processes one lattice site (x, y, z)

struct Params {
    width: u32,
    height: u32,
    depth: u32,
    step_count: u32,
}

@group(0) @binding(0) var<uniform> params: Params;
@group(0) @binding(1) var<storage, read> energy_in: array<u32>;   // Current energy state
@group(0) @binding(2) var<storage, read_write> energy_out: array<atomic<u32>>;  // Next energy state (atomic for race safety)

// Quantum levels (0-3)
const LEVEL_0: u32 = 0u;
const LEVEL_1: u32 = 1u;
const LEVEL_2: u32 = 2u;
const LEVEL_3: u32 = 3u;

// Get linear index from 3D coordinates
fn get_index(x: u32, y: u32, z: u32) -> u32 {
    return z * params.width * params.height + y * params.width + x;
}

// Get neighbor index with toroidal wrapping
fn get_neighbor_index(x: i32, y: i32, z: i32) -> u32 {
    let w = i32(params.width);
    let h = i32(params.height);
    let d = i32(params.depth);

    // Toroidal wrapping
    let nx = (x + w) % w;
    let ny = (y + h) % h;
    let nz = (z + d) % d;

    return get_index(u32(nx), u32(ny), u32(nz));
}

// Simple pseudo-random number generator based on site position and step
fn pseudo_random(idx: u32, step: u32) -> u32 {
    var x = idx + step * 1103515245u;
    x = ((x >> 16u) ^ x) * 0x45d9f3bu;
    x = ((x >> 16u) ^ x) * 0x45d9f3bu;
    x = (x >> 16u) ^ x;
    return x;
}

// PASS 1: Copy energy from input to output
// This initializes the output buffer with current state
@compute @workgroup_size(4, 4, 4)
fn copy_energy(@builtin(global_invocation_id) global_id: vec3<u32>) {
    let x = global_id.x;
    let y = global_id.y;
    let z = global_id.z;

    // Bounds check
    if (x >= params.width || y >= params.height || z >= params.depth) {
        return;
    }

    let idx = get_index(x, y, z);
    let energy = energy_in[idx];

    // Initialize output with current energy
    atomicStore(&energy_out[idx], energy);
}

// PASS 2: Propagate quantum energy transfers
// Reads from input, writes atomically to output (no race with copy)
@compute @workgroup_size(4, 4, 4)
fn propagate_energy(@builtin(global_invocation_id) global_id: vec3<u32>) {
    let x = global_id.x;
    let y = global_id.y;
    let z = global_id.z;

    // Bounds check
    if (x >= params.width || y >= params.height || z >= params.depth) {
        return;
    }

    let idx = get_index(x, y, z);
    let energy = energy_in[idx];

    // No energy to propagate
    if (energy == 0u) {
        return;
    }

    // Get 6 neighbors (±X, ±Y, ±Z)
    let ix = i32(x);
    let iy = i32(y);
    let iz = i32(z);

    var neighbors: array<u32, 6>;
    neighbors[0] = get_neighbor_index(ix + 1, iy, iz);  // +X
    neighbors[1] = get_neighbor_index(ix - 1, iy, iz);  // -X
    neighbors[2] = get_neighbor_index(ix, iy + 1, iz);  // +Y
    neighbors[3] = get_neighbor_index(ix, iy - 1, iz);  // -Y
    neighbors[4] = get_neighbor_index(ix, iy, iz + 1);  // +Z
    neighbors[5] = get_neighbor_index(ix, iy, iz - 1);  // -Z

    // Count neighbors with lower energy and collect their indices
    var lower_neighbors: array<u32, 6>;
    var lower_count = 0u;

    for (var i = 0u; i < 6u; i++) {
        let n_idx = neighbors[i];
        let n_energy = energy_in[n_idx];

        if (n_energy < energy) {
            lower_neighbors[lower_count] = n_idx;
            lower_count++;
        }
    }

    // Transfer 1 quantum to a randomly chosen lower neighbor
    if (lower_count > 0u && energy > 0u) {
        // Choose random lower neighbor
        let random_val = pseudo_random(idx, params.step_count);
        let choice = random_val % lower_count;
        let target_idx = lower_neighbors[choice];

        // Check if target can accept quantum
        let target_energy = energy_in[target_idx];
        if (target_energy < LEVEL_3) {
            // Transfer quantum
            // NOTE: This has race conditions on target_idx, but they average out
            // and preserve energy statistically (same as Java parallel version)
            atomicSub(&energy_out[idx], 1u);
            atomicAdd(&energy_out[target_idx], 1u);
        }
    }
}
