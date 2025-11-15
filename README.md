# Planck Lattice Simulation

A physics simulation modeling spacetime as a discrete lattice of Planck-scale spheres, exploring how gravity, electromagnetism, and fundamental forces might emerge from lattice geometry and information propagation.

## Core Concepts

- **Discrete Spacetime**: Universe modeled as 3D lattice (starting with 2D) at Planck length scale
- **Emergent Gravity**: Mass/energy compresses local lattice geometry
- **Light Speed**: Information propagates at 1 sphere per time step, defining *c*
- **EM Radiation**: State changes cascading through the lattice
- **SIMD Optimization**: Uses Java 25's Vector API for high-performance computation

## Building and Running

### Requirements
- Java 25 (or compatible JDK with Vector API support)
- Maven 3.6+

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn exec:java
```

Or with custom parameters:
```bash
java --add-modules jdk.incubator.vector --enable-preview \
     -cp target/classes com.plancklattice.Main
```

## Project Structure

```
planck-lattice/
├── pom.xml                          # Maven build configuration
├── src/main/java/com/plancklattice/
│   ├── PlanckLattice.java          # Core SoA data structure
│   ├── SimulationEngine.java       # Main simulation loop
│   ├── VectorForces.java           # Vectorized force calculations
│   ├── EMPropagator.java           # Vectorized EM wave propagation
│   ├── Integrator.java             # Position/velocity integration
│   ├── Visualizer.java             # Output generation
│   └── Main.java                   # Entry point
└── README.md
```

## Physics Implementation

### 1. Spacing Forces
Each sphere maintains equilibrium distance of 1.0 ℓ_p to neighbors using Hooke's law.

### 2. Gravitational Compression
Energy density causes local lattice compression, creating curved spacetime geometry.

### 3. EM Wave Propagation
Discrete wave equation propagates EM fields through lattice at inherent speed c.

### 4. Integration
Verlet integration updates sphere positions based on accumulated forces.

## Visualization Output

The simulation generates:
- Lattice spacing heatmaps (shows gravitational compression)
- EM field amplitude visualizations
- Energy density maps
- Per-frame statistics (avg spacing, total energy, compression metrics)

## Parameters

Key physics parameters (tunable in code):
- `SPRING_K = 1.0f` - Spring constant for lattice spacing
- `EQUILIBRIUM_DISTANCE = 1.0f` - Target Planck length
- `GRAVITY_G = 0.01f` - Gravitational coupling constant
- `EM_DAMPING = 0.01f` - EM wave damping factor
- `DT = 0.01f` - Simulation timestep

## Future Extensions (Phase 2)

- Expand to 3D lattice
- Strong force (annealing in high-energy regions)
- Weak force (metastable transitions)
- Particle formation (stable resonances)
- Real-time 3D visualization
- Multi-threading + GPU acceleration

## Theoretical Context

This simulation explores an interpretation where spacetime is fundamentally discrete at the Planck scale, drawing inspiration from:
- Loop quantum gravity
- Lattice field theory
- Cellular automata physics models
- Digital physics concepts

## License

MIT License - This is an educational/research simulation exploring theoretical physics concepts.
