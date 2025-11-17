// 3D Point Cloud Rendering Shader
// Renders energy sites as colored points in 3D space

struct Camera {
    view_proj: mat4x4<f32>,
}

struct Params {
    width: u32,
    height: u32,
    depth: u32,
    step_count: u32,
}

@group(0) @binding(0) var<uniform> camera: Camera;
@group(0) @binding(1) var<uniform> params: Params;
@group(0) @binding(2) var<storage, read> energy: array<u32>;

struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec4<f32>,
    @location(1) @interpolate(flat) point_size: f32,
}

// Get linear index from 3D coordinates
fn get_index(x: u32, y: u32, z: u32) -> u32 {
    return z * params.width * params.height + y * params.width + x;
}

// Heat map color: 0=black, 1=blue, 2=yellow, 3=red
fn energy_color(level: u32) -> vec4<f32> {
    if (level == 0u) {
        return vec4<f32>(0.0, 0.0, 0.0, 0.0); // Transparent (skip)
    } else if (level == 1u) {
        return vec4<f32>(0.0, 0.5, 1.0, 0.8); // Blue
    } else if (level == 2u) {
        return vec4<f32>(1.0, 1.0, 0.0, 0.9); // Yellow
    } else {
        return vec4<f32>(1.0, 0.2, 0.0, 1.0); // Red-orange
    }
}

@vertex
fn vs_main(@builtin(vertex_index) vertex_index: u32) -> VertexOutput {
    var output: VertexOutput;

    let total_sites = params.width * params.height * params.depth;

    // Skip if beyond array bounds
    if (vertex_index >= total_sites) {
        output.position = vec4<f32>(0.0, 0.0, 0.0, 0.0);
        output.color = vec4<f32>(0.0, 0.0, 0.0, 0.0);
        output.point_size = 0.0;
        return output;
    }

    // Get energy at this site
    let level = energy[vertex_index];

    // Skip empty sites
    if (level == 0u) {
        output.position = vec4<f32>(0.0, 0.0, 0.0, 0.0);
        output.color = vec4<f32>(0.0, 0.0, 0.0, 0.0);
        output.point_size = 0.0;
        return output;
    }

    // Convert linear index to 3D coordinates
    let z = vertex_index / (params.width * params.height);
    let remainder = vertex_index % (params.width * params.height);
    let y = remainder / params.width;
    let x = remainder % params.width;

    // Center the lattice at origin
    let half_w = f32(params.width) * 0.5;
    let half_h = f32(params.height) * 0.5;
    let half_d = f32(params.depth) * 0.5;

    let world_pos = vec3<f32>(
        f32(x) - half_w,
        f32(y) - half_h,
        f32(z) - half_d
    );

    // Apply camera transform
    output.position = camera.view_proj * vec4<f32>(world_pos, 1.0);
    output.color = energy_color(level);
    output.point_size = f32(level) * 2.0; // Bigger points for higher energy

    return output;
}

@fragment
fn fs_main(input: VertexOutput) -> @location(0) vec4<f32> {
    // Simple point rendering
    return input.color;
}
