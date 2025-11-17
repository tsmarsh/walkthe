use bytemuck::{Pod, Zeroable};
use glam::{Mat4, Vec3};
use lattice_gpu::DiscreteLatticeGPU;
use std::sync::Arc;
use wgpu::util::DeviceExt;
use winit::{
    event::*,
    event_loop::EventLoop,
    keyboard::{KeyCode, PhysicalKey},
};

#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct CameraUniform {
    view_proj: [[f32; 4]; 4],
}

#[repr(C)]
#[derive(Copy, Clone, Debug, Pod, Zeroable)]
struct ParamsUniform {
    width: u32,
    height: u32,
    depth: u32,
    step_count: u32,
}

struct Camera {
    distance: f32,
    rotation_y: f32,
    rotation_x: f32,
    lattice_size: f32,
}

impl Camera {
    fn new(lattice_size: u32) -> Self {
        Self {
            distance: lattice_size as f32 * 1.5,
            rotation_y: 0.0,
            rotation_x: 0.3,
            lattice_size: lattice_size as f32,
        }
    }

    fn build_view_proj_matrix(&self, aspect: f32) -> Mat4 {
        // Camera position (orbit around origin)
        let eye = Vec3::new(
            self.distance * self.rotation_y.sin() * self.rotation_x.cos(),
            self.distance * self.rotation_x.sin(),
            self.distance * self.rotation_y.cos() * self.rotation_x.cos(),
        );

        let view = Mat4::look_at_rh(eye, Vec3::ZERO, Vec3::Y);
        let proj = Mat4::perspective_rh(45.0_f32.to_radians(), aspect, 0.1, 1000.0);

        proj * view
    }

    fn update(&mut self, delta_x: f32, delta_y: f32, delta_zoom: f32) {
        self.rotation_y += delta_x * 0.01;
        self.rotation_x = (self.rotation_x + delta_y * 0.01).clamp(-1.5, 1.5);
        self.distance = (self.distance + delta_zoom * 0.1)
            .clamp(self.lattice_size * 0.5, self.lattice_size * 5.0);
    }
}

struct Viewer {
    surface: wgpu::Surface<'static>,
    device: Arc<wgpu::Device>,
    queue: Arc<wgpu::Queue>,
    config: wgpu::SurfaceConfiguration,
    size: winit::dpi::PhysicalSize<u32>,
    window: Arc<winit::window::Window>,

    lattice: DiscreteLatticeGPU,
    render_pipeline: wgpu::RenderPipeline,
    camera_buffer: wgpu::Buffer,
    params_buffer: wgpu::Buffer,
    bind_group_layout: wgpu::BindGroupLayout,

    camera: Camera,
    paused: bool,
    mouse_pressed: bool,
    last_mouse_pos: Option<(f64, f64)>,
}

impl Viewer {
    async fn new(window: Arc<winit::window::Window>, lattice_size: u32) -> Self {
        let size = window.inner_size();

        // Create wgpu instance and surface
        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::all(),
            ..Default::default()
        });

        let surface = instance.create_surface(window.clone()).unwrap();

        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .expect("Failed to find GPU adapter");

        let adapter_limits = adapter.limits();
        let mut limits = wgpu::Limits::default();
        limits.max_storage_buffer_binding_size = adapter_limits.max_storage_buffer_binding_size;
        limits.max_buffer_size = adapter_limits.max_buffer_size;

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("GPU Device"),
                    required_features: wgpu::Features::empty(),
                    required_limits: limits,
                    memory_hints: Default::default(),
                },
                None,
            )
            .await
            .expect("Failed to create device");

        let device = Arc::new(device);
        let queue = Arc::new(queue);

        let surface_caps = surface.get_capabilities(&adapter);
        let surface_format = surface_caps
            .formats
            .iter()
            .find(|f| f.is_srgb())
            .copied()
            .unwrap_or(surface_caps.formats[0]);

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface_format,
            width: size.width,
            height: size.height,
            present_mode: wgpu::PresentMode::Fifo,
            alpha_mode: surface_caps.alpha_modes[0],
            view_formats: vec![],
            desired_maximum_frame_latency: 2,
        };
        surface.configure(&device, &config);

        // Create lattice with shared device
        println!("Initializing {}³ quantum lattice on GPU...", lattice_size);
        let mut lattice = DiscreteLatticeGPU::new_with_device(
            device.clone(),
            queue.clone(),
            lattice_size,
            lattice_size,
            lattice_size,
        );
        lattice.initialize_vacuum();

        // Add large spherical energy distribution
        let c = lattice_size / 2;
        let radius = 15i32; // Much larger sphere
        for dz in -radius..=radius {
            for dy in -radius..=radius {
                for dx in -radius..=radius {
                    let dist_sq = dx * dx + dy * dy + dz * dz;
                    if dist_sq <= radius * radius {
                        lattice.add_energy_quantum(
                            (c as i32 + dx) as u32,
                            (c as i32 + dy) as u32,
                            (c as i32 + dz) as u32,
                            3, // Max energy level
                        );
                    }
                }
            }
        }

        // Create camera
        let camera = Camera::new(lattice_size);
        let camera_uniform = CameraUniform {
            view_proj: camera
                .build_view_proj_matrix(size.width as f32 / size.height as f32)
                .to_cols_array_2d(),
        };

        let camera_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Camera Buffer"),
            contents: bytemuck::cast_slice(&[camera_uniform]),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        let params_uniform = ParamsUniform {
            width: lattice_size,
            height: lattice_size,
            depth: lattice_size,
            step_count: 0,
        };

        let params_buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("Params Buffer"),
            contents: bytemuck::cast_slice(&[params_uniform]),
            usage: wgpu::BufferUsages::UNIFORM
                | wgpu::BufferUsages::STORAGE
                | wgpu::BufferUsages::COPY_DST,
        });

        // Load shaders
        let shader = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("Render Shader"),
            source: wgpu::ShaderSource::Wgsl(include_str!("render_shader.wgsl").into()),
        });

        // Create bind group layout
        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("Render Bind Group Layout"),
            entries: &[
                // Camera
                wgpu::BindGroupLayoutEntry {
                    binding: 0,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                // Params
                wgpu::BindGroupLayoutEntry {
                    binding: 1,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Uniform,
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
                // Energy buffer
                wgpu::BindGroupLayoutEntry {
                    binding: 2,
                    visibility: wgpu::ShaderStages::VERTEX,
                    ty: wgpu::BindingType::Buffer {
                        ty: wgpu::BufferBindingType::Storage { read_only: true },
                        has_dynamic_offset: false,
                        min_binding_size: None,
                    },
                    count: None,
                },
            ],
        });

        let pipeline_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("Render Pipeline Layout"),
            bind_group_layouts: &[&bind_group_layout],
            push_constant_ranges: &[],
        });

        let render_pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("Render Pipeline"),
            layout: Some(&pipeline_layout),
            vertex: wgpu::VertexState {
                module: &shader,
                entry_point: "vs_main",
                buffers: &[],
                compilation_options: Default::default(),
            },
            fragment: Some(wgpu::FragmentState {
                module: &shader,
                entry_point: "fs_main",
                targets: &[Some(wgpu::ColorTargetState {
                    format: config.format,
                    blend: Some(wgpu::BlendState::ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
                compilation_options: Default::default(),
            }),
            primitive: wgpu::PrimitiveState {
                topology: wgpu::PrimitiveTopology::PointList,
                strip_index_format: None,
                front_face: wgpu::FrontFace::Ccw,
                cull_mode: None,
                polygon_mode: wgpu::PolygonMode::Fill,
                unclipped_depth: false,
                conservative: false,
            },
            depth_stencil: None,
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        });

        Self {
            surface,
            device,
            queue,
            config,
            size,
            window,
            lattice,
            render_pipeline,
            camera_buffer,
            params_buffer,
            bind_group_layout,
            camera,
            paused: false,
            mouse_pressed: false,
            last_mouse_pos: None,
        }
    }

    fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size = new_size;
            self.config.width = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
        }
    }

    fn input(&mut self, event: &WindowEvent) -> bool {
        match event {
            WindowEvent::KeyboardInput {
                event:
                    KeyEvent {
                        physical_key: PhysicalKey::Code(key),
                        state: ElementState::Pressed,
                        ..
                    },
                ..
            } => match key {
                KeyCode::Space => {
                    self.paused = !self.paused;
                    println!(
                        "Simulation {}",
                        if self.paused { "paused" } else { "running" }
                    );
                    true
                }
                KeyCode::KeyR => {
                    println!("Resetting lattice...");
                    self.lattice.initialize_vacuum();
                    let c = 50; // Assume 100³ lattice
                    let radius = 15i32;
                    for dz in -radius..=radius {
                        for dy in -radius..=radius {
                            for dx in -radius..=radius {
                                let dist_sq = dx * dx + dy * dy + dz * dz;
                                if dist_sq <= radius * radius {
                                    self.lattice.add_energy_quantum(
                                        (c + dx) as u32,
                                        (c + dy) as u32,
                                        (c + dz) as u32,
                                        3,
                                    );
                                }
                            }
                        }
                    }
                    true
                }
                _ => false,
            },
            WindowEvent::MouseInput {
                state,
                button: MouseButton::Left,
                ..
            } => {
                self.mouse_pressed = *state == ElementState::Pressed;
                if !self.mouse_pressed {
                    self.last_mouse_pos = None;
                }
                true
            }
            WindowEvent::CursorMoved { position, .. } => {
                if self.mouse_pressed {
                    if let Some((last_x, last_y)) = self.last_mouse_pos {
                        let delta_x = position.x - last_x;
                        let delta_y = position.y - last_y;
                        self.camera.update(delta_x as f32, -delta_y as f32, 0.0);
                    }
                    self.last_mouse_pos = Some((position.x, position.y));
                }
                true
            }
            WindowEvent::MouseWheel {
                delta: MouseScrollDelta::LineDelta(_, y),
                ..
            } => {
                self.camera.update(0.0, 0.0, -*y);
                true
            }
            _ => false,
        }
    }

    fn update(&mut self) {
        if !self.paused {
            self.lattice.propagate_energy();
        }

        // Update camera
        let camera_uniform = CameraUniform {
            view_proj: self
                .camera
                .build_view_proj_matrix(self.size.width as f32 / self.size.height as f32)
                .to_cols_array_2d(),
        };
        self.queue.write_buffer(
            &self.camera_buffer,
            0,
            bytemuck::cast_slice(&[camera_uniform]),
        );
    }

    fn render(&mut self) -> Result<(), wgpu::SurfaceError> {
        let output = self.surface.get_current_texture()?;
        let view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        // Create bind group with current energy buffer (updates each frame for ping-pong buffers)
        let energy_buffer = self.lattice.get_energy_buffer();
        let bind_group = self.device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("Render Bind Group"),
            layout: &self.bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: self.camera_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: self.params_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: energy_buffer.as_entire_binding(),
                },
            ],
        });

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("Render Encoder"),
            });

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Render Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.01,
                            g: 0.01,
                            b: 0.02,
                            a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: None,
                timestamp_writes: None,
                occlusion_query_set: None,
            });

            render_pass.set_pipeline(&self.render_pipeline);
            render_pass.set_bind_group(0, &bind_group, &[]);

            let total_sites = 100 * 100 * 100; // Assumes 100³ lattice
            render_pass.draw(0..total_sites, 0..1);
        }

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();

        Ok(())
    }
}

#[pollster::main]
async fn main() {
    env_logger::init();

    println!("=== 3D Quantum Lattice Viewer ===");
    println!("Controls:");
    println!("  Mouse drag: Rotate camera");
    println!("  Mouse wheel: Zoom");
    println!("  SPACE: Pause/Resume");
    println!("  R: Reset simulation");
    println!("  ESC: Quit\n");

    let event_loop = EventLoop::new().unwrap();
    let window_attributes = winit::window::Window::default_attributes()
        .with_title("Quantum Lattice 3D Viewer")
        .with_inner_size(winit::dpi::LogicalSize::new(1280, 720));
    let window = Arc::new(event_loop.create_window(window_attributes).unwrap());

    let mut viewer = Viewer::new(window.clone(), 100).await;

    let _ = event_loop
        .run(move |event, target| match event {
            winit::event::Event::WindowEvent {
                ref event,
                window_id,
            } if window_id == viewer.window.id() => {
                if !viewer.input(event) {
                    match event {
                        WindowEvent::CloseRequested
                        | WindowEvent::KeyboardInput {
                            event:
                                KeyEvent {
                                    physical_key: PhysicalKey::Code(KeyCode::Escape),
                                    state: ElementState::Pressed,
                                    ..
                                },
                            ..
                        } => target.exit(),
                        WindowEvent::Resized(physical_size) => {
                            viewer.resize(*physical_size);
                        }
                        WindowEvent::RedrawRequested => {
                            viewer.update();
                            match viewer.render() {
                                Ok(_) => {}
                                Err(wgpu::SurfaceError::Lost) => viewer.resize(viewer.size),
                                Err(wgpu::SurfaceError::OutOfMemory) => target.exit(),
                                Err(e) => eprintln!("{:?}", e),
                            }
                        }
                        _ => {}
                    }
                }
            }
            winit::event::Event::AboutToWait => {
                viewer.window.request_redraw();
            }
            _ => {}
        })
        .unwrap();
}
