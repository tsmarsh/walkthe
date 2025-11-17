// GPU-Accelerated 3D Discrete Quantum Lattice Library
//
// Cross-platform GPU compute for massive lattice simulations
// - Works on NVIDIA (Vulkan), AMD (Vulkan), Apple Silicon (Metal)
// - Two-pass algorithm ensures perfect energy conservation
// - Supports up to 700³ lattices (~343M sites, 1.3GB) on RTX 4080

use bytemuck::{Pod, Zeroable};
use wgpu::util::DeviceExt;

#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct Params {
    width: u32,
    height: u32,
    depth: u32,
    step_count: u32,
}

pub struct DiscreteLatticeGPU {
    device: wgpu::Device,
    queue: wgpu::Queue,
    copy_pipeline: wgpu::ComputePipeline,
    propagate_pipeline: wgpu::ComputePipeline,
    bind_group_layout: wgpu::BindGroupLayout,
    params_buffer: wgpu::Buffer,
    energy_buffer_a: wgpu::Buffer,
    energy_buffer_b: wgpu::Buffer,
    staging_buffer: wgpu::Buffer,
    width: u32,
    height: u32,
    depth: u32,
    total_sites: usize,
    step_count: u32,
}

impl DiscreteLatticeGPU {
    pub async fn new(width: u32, height: u32, depth: u32) -> Self {
        // Initialize GPU
        let instance = wgpu::Instance::default();
        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                ..Default::default()
            })
            .await
            .expect("Failed to find GPU adapter");

        // Query adapter's actual limits
        let adapter_limits = adapter.limits();

        // Request maximum limits, but don't exceed what adapter supports
        // RTX 4080: 2 GB, lavapipe: 2 GB - 1 byte
        let mut limits = wgpu::Limits::default();
        limits.max_storage_buffer_binding_size = adapter_limits.max_storage_buffer_binding_size;
        limits.max_buffer_size = adapter_limits.max_buffer_size;

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("Quantum Lattice GPU"),
                    required_features: wgpu::Features::empty(),
                    required_limits: limits,
                    memory_hints: Default::default(),
                },
                None,
            )
            .await
            .expect("Failed to create device");

        let total_sites = (width * height * depth) as usize;

        // Create buffers
        let params = Params {
            width,
            height,
            depth,
            step_count: 0,
        };

        let params_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Params Buffer"),
            contents: bytemuck::cast_slice(&[params]),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        // Energy buffers (ping-pong)
        let energy_buffer_a = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Energy Buffer A"),
            size: (total_sites * std::mem::size_of::<u32>()) as u64,
            usage: wgpu::BufferUsages::STORAGE
                | wgpu::BufferUsages::COPY_DST
                | wgpu::BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });

        let energy_buffer_b = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Energy Buffer B"),
            size: (total_sites * std::mem::size_of::<u32>()) as u64,
            usage: wgpu::BufferUsages::STORAGE
                | wgpu::BufferUsages::COPY_DST
                | wgpu::BufferUsages::COPY_SRC,
            mapped_at_creation: false,
        });

        // Staging buffer for reading results back
        let staging_buffer = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("Staging Buffer"),
            size: (total_sites * std::mem::size_of::<u32>()) as u64,
            usage: wgpu::BufferUsages::MAP_READ | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        // Load shader
        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Compute Shader"),
            source: wgpu::ShaderSource::Wgsl(include_str!("shader.wgsl").into()),
        });

        // Create bind group layout
        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Bind Group Layout"),
            entries: &[
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::COMPUTE,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::COMPUTE,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Storage { read_only: true },
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                wgpu::BindGroupLayoutEntry {
                    binding: 2,
                    visibility: wgpu::ShaderStages::COMPUTE,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Storage { read_only: false },
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
            ],
        });

        // Create compute pipelines
        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Pipeline Layout"),
            bind_group_layouts: &[&bind_group_layout],
            push_constant_ranges: &[],
        });

        let copy_pipeline = device.create_compute_pipeline(&wgpu::ComputePipelineDescriptor {
            label: Some("Copy Pipeline"),
            layout: Some(&pipeline_layout),
            module: &shader,
            entry_point: "copy_energy",
            compilation_options: Default::default(),
            cache: None,
        });

        let propagate_pipeline = device.create_compute_pipeline(&wgpu::ComputePipelineDescriptor {
            label: Some("Propagate Pipeline"),
            layout: Some(&pipeline_layout),
            module: &shader,
            entry_point: "propagate_energy",
            compilation_options: Default::default(),
            cache: None,
        });

        Self {
            device,
            queue,
            copy_pipeline,
            propagate_pipeline,
            bind_group_layout,
            params_buffer,
            energy_buffer_a,
            energy_buffer_b,
            staging_buffer,
            width,
            height,
            depth,
            total_sites,
            step_count: 0,
        }
    }

    pub fn initialize_vacuum(&mut self) {
        let zero_data = vec![0u32; self.total_sites];
        self.queue
            .write_buffer(&self.energy_buffer_a, 0, bytemuck::cast_slice(&zero_data));
        self.queue
            .write_buffer(&self.energy_buffer_b, 0, bytemuck::cast_slice(&zero_data));
    }

    pub fn add_energy_quantum(&mut self, x: u32, y: u32, z: u32, quanta: u32) {
        let idx = (z * self.width * self.height + y * self.width + x) as usize;

        // Read current state
        let mut energy_data = vec![0u32; self.total_sites];
        pollster::block_on(async {
            let mut encoder = self.device.create_command_encoder(&Default::default());
            encoder.copy_buffer_to_buffer(
                &self.energy_buffer_a,
                0,
                &self.staging_buffer,
                0,
                (self.total_sites * std::mem::size_of::<u32>()) as u64,
            );
            self.queue.submit(Some(encoder.finish()));

            let buffer_slice = self.staging_buffer.slice(..);
            let (sender, receiver) = flume::bounded(1);
            buffer_slice.map_async(wgpu::MapMode::Read, move |result| {
                sender.send(result).unwrap();
            });
            self.device.poll(wgpu::Maintain::Wait);
            receiver.recv_async().await.unwrap().unwrap();

            let data = buffer_slice.get_mapped_range();
            energy_data.copy_from_slice(bytemuck::cast_slice(&data));
            drop(data);
            self.staging_buffer.unmap();
        });

        // Modify
        energy_data[idx] = (energy_data[idx] + quanta).min(3);

        // Write back
        self.queue
            .write_buffer(&self.energy_buffer_a, 0, bytemuck::cast_slice(&energy_data));
    }

    pub fn propagate_energy(&mut self) {
        // Update step count
        let params = Params {
            width: self.width,
            height: self.height,
            depth: self.depth,
            step_count: self.step_count,
        };
        self.queue
            .write_buffer(&self.params_buffer, 0, bytemuck::cast_slice(&[params]));

        // Determine which buffers to use (ping-pong)
        let (input_buffer, output_buffer) = if self.step_count % 2 == 0 {
            (&self.energy_buffer_a, &self.energy_buffer_b)
        } else {
            (&self.energy_buffer_b, &self.energy_buffer_a)
        };

        // Create bind group
        let bind_group = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Bind Group"),
            layout: &self.bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: self.params_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: input_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: output_buffer.as_entire_binding(),
                },
            ],
        });

        // Workgroups: 4x4x4 = 64 threads per workgroup
        let workgroups_x = (self.width + 3) / 4;
        let workgroups_y = (self.height + 3) / 4;
        let workgroups_z = (self.depth + 3) / 4;

        // Dispatch PASS 1: Copy energy
        let mut encoder = self.device.create_command_encoder(&Default::default());
        {
            let mut compute_pass = encoder.begin_compute_pass(&wgpu::ComputePassDescriptor {
                label: Some("Copy Pass"),
                timestamp_writes: None,
            });
            compute_pass.set_pipeline(&self.copy_pipeline);
            compute_pass.set_bind_group(0, &bind_group, &[]);
            compute_pass.dispatch_workgroups(workgroups_x, workgroups_y, workgroups_z);
        }
        self.queue.submit(Some(encoder.finish()));

        // Dispatch PASS 2: Propagate transfers
        let mut encoder = self.device.create_command_encoder(&Default::default());
        {
            let mut compute_pass = encoder.begin_compute_pass(&wgpu::ComputePassDescriptor {
                label: Some("Propagate Pass"),
                timestamp_writes: None,
            });
            compute_pass.set_pipeline(&self.propagate_pipeline);
            compute_pass.set_bind_group(0, &bind_group, &[]);
            compute_pass.dispatch_workgroups(workgroups_x, workgroups_y, workgroups_z);
        }
        self.queue.submit(Some(encoder.finish()));

        self.step_count += 1;
    }

    pub fn get_energy_buffer(&self) -> &wgpu::Buffer {
        // Return the current active buffer for rendering
        if self.step_count % 2 == 0 {
            &self.energy_buffer_a
        } else {
            &self.energy_buffer_b
        }
    }

    pub async fn get_total_energy(&self) -> u32 {
        // Use the current active buffer
        let active_buffer = if self.step_count % 2 == 0 {
            &self.energy_buffer_a
        } else {
            &self.energy_buffer_b
        };

        let mut encoder = self.device.create_command_encoder(&Default::default());
        encoder.copy_buffer_to_buffer(
            active_buffer,
            0,
            &self.staging_buffer,
            0,
            (self.total_sites * std::mem::size_of::<u32>()) as u64,
        );
        self.queue.submit(Some(encoder.finish()));

        let buffer_slice = self.staging_buffer.slice(..);
        let (sender, receiver) = flume::bounded(1);
        buffer_slice.map_async(wgpu::MapMode::Read, move |result| {
            sender.send(result).unwrap();
        });
        self.device.poll(wgpu::Maintain::Wait);
        receiver.recv_async().await.unwrap().unwrap();

        let data = buffer_slice.get_mapped_range();
        let energy_data: &[u32] = bytemuck::cast_slice(&data);
        let total: u32 = energy_data.iter().sum();
        drop(data);
        self.staging_buffer.unmap();

        total
    }
}
