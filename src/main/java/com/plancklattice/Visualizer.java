package com.plancklattice;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Visualization and output generation for the Planck lattice simulation.
 * Generates PPM images and statistics.
 */
public class Visualizer {

    private final PlanckLattice lattice;

    public Visualizer(PlanckLattice lattice) {
        this.lattice = lattice;
    }

    /**
     * Generate a PPM image showing lattice spacing as a heatmap.
     * Blue = compressed (< 1.0), Green = equilibrium (1.0), Red = stretched (> 1.0)
     */
    public void generateSpacingHeatmap(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Calculate spacing for each sphere (average to its neighbors)
        float[] spacing = new float[lattice.totalSpheres];
        float minSpacing = Float.MAX_VALUE;
        float maxSpacing = Float.MIN_VALUE;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = lattice.getIndex(x, y);
                float px = lattice.posX[index];
                float py = lattice.posY[index];

                float avgSpacing = 0.0f;
                int count = 0;

                // Average distance to all 4 neighbors
                int[] dx = {1, -1, 0, 0};
                int[] dy = {0, 0, 1, -1};

                for (int i = 0; i < 4; i++) {
                    int neighborIdx = lattice.getNeighborIndex(x, y, dx[i], dy[i]);
                    float npx = lattice.posX[neighborIdx];
                    float npy = lattice.posY[neighborIdx];
                    float dist = (float) Math.sqrt((npx - px) * (npx - px) + (npy - py) * (npy - py));
                    avgSpacing += dist;
                    count++;
                }

                spacing[index] = avgSpacing / count;
                minSpacing = Math.min(minSpacing, spacing[index]);
                maxSpacing = Math.max(maxSpacing, spacing[index]);
            }
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);
                    float s = spacing[index];

                    // Map spacing to color
                    int[] rgb = spacingToColor(s, minSpacing, maxSpacing);
                    writer.println(rgb[0] + " " + rgb[1] + " " + rgb[2]);
                }
            }
        }
    }

    /**
     * Generate a PPM image showing EM field amplitude.
     */
    public void generateEMFieldImage(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Find max amplitude for normalization
        float maxAmplitude = 0.0001f; // Avoid division by zero
        for (int i = 0; i < lattice.totalSpheres; i++) {
            float amplitude = (float) Math.sqrt(
                lattice.emFieldReal[i] * lattice.emFieldReal[i] +
                lattice.emFieldImag[i] * lattice.emFieldImag[i]
            );
            maxAmplitude = Math.max(maxAmplitude, amplitude);
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);
                    float real = lattice.emFieldReal[index];
                    float imag = lattice.emFieldImag[index];
                    float amplitude = (float) Math.sqrt(real * real + imag * imag);

                    // Normalize and map to color (blue to white to red)
                    float normalized = amplitude / maxAmplitude;
                    int[] rgb = amplitudeToColor(normalized, real, imag);
                    writer.println(rgb[0] + " " + rgb[1] + " " + rgb[2]);
                }
            }
        }
    }

    /**
     * Generate a PPM image showing energy density.
     */
    public void generateEnergyDensityImage(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Find max energy for normalization
        float maxEnergy = 0.0001f;
        for (int i = 0; i < lattice.totalSpheres; i++) {
            maxEnergy = Math.max(maxEnergy, lattice.energyDensity[i]);
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);
                    float energy = lattice.energyDensity[index];
                    float normalized = energy / maxEnergy;

                    // Yellow to red gradient for energy
                    int r = 255;
                    int g = (int) (255 * (1.0f - normalized * 0.7f));
                    int b = 0;

                    writer.println(r + " " + g + " " + b);
                }
            }
        }
    }

    /**
     * Map spacing value to RGB color.
     * Blue = compressed, Green = equilibrium, Red = stretched
     */
    private int[] spacingToColor(float spacing, float min, float max) {
        // Normalize around equilibrium (1.0)
        float normalized = (spacing - PlanckLattice.EQUILIBRIUM_DISTANCE) /
                           Math.max(Math.abs(max - PlanckLattice.EQUILIBRIUM_DISTANCE),
                                    Math.abs(min - PlanckLattice.EQUILIBRIUM_DISTANCE));

        int r, g, b;
        if (normalized < 0) {
            // Compressed: blue to green
            float t = Math.max(-1.0f, normalized) + 1.0f; // 0 to 1
            r = 0;
            g = (int) (255 * t);
            b = (int) (255 * (1.0f - t));
        } else {
            // Stretched: green to red
            float t = Math.min(1.0f, normalized); // 0 to 1
            r = (int) (255 * t);
            g = (int) (255 * (1.0f - t));
            b = 0;
        }

        return new int[]{r, g, b};
    }

    /**
     * Map EM field amplitude to color.
     * Uses phase (real vs imaginary) to determine hue.
     */
    private int[] amplitudeToColor(float normalized, float real, float imag) {
        // Use HSV-like coloring: hue from phase, value from amplitude
        float phase = (float) Math.atan2(imag, real); // -π to π
        float hue = (phase + (float) Math.PI) / (2.0f * (float) Math.PI); // 0 to 1

        // Convert HSV to RGB
        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = 0;
        float q = normalized * (1 - f);
        float t = normalized * f;

        float r, g, b;
        switch (h % 6) {
            case 0: r = normalized; g = t; b = p; break;
            case 1: r = q; g = normalized; b = p; break;
            case 2: r = p; g = normalized; b = t; break;
            case 3: r = p; g = q; b = normalized; break;
            case 4: r = t; g = p; b = normalized; break;
            default: r = normalized; g = p; b = q; break;
        }

        return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }

    /**
     * Print statistics about the current simulation state.
     */
    public void printStatistics(int timestep, double elapsedTime) {
        float avgSpacing = lattice.getAverageSpacing();
        float totalEnergy = lattice.getTotalEnergy();
        float maxEMAmplitude = lattice.getMaxEMAmplitude();

        // Calculate min/max spacing
        float minSpacing = Float.MAX_VALUE;
        float maxSpacing = Float.MIN_VALUE;

        for (int y = 0; y < lattice.gridHeight; y++) {
            for (int x = 0; x < lattice.gridWidth; x++) {
                int index = lattice.getIndex(x, y);
                float px = lattice.posX[index];
                float py = lattice.posY[index];

                if (x < lattice.gridWidth - 1) {
                    int rightIndex = lattice.getIndex(x + 1, y);
                    float dx = lattice.posX[rightIndex] - px;
                    float dy = lattice.posY[rightIndex] - py;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);
                    minSpacing = Math.min(minSpacing, dist);
                    maxSpacing = Math.max(maxSpacing, dist);
                }
            }
        }

        System.out.println("=== Timestep " + timestep + " ===");
        System.out.printf("Elapsed time: %.3f ms%n", elapsedTime);
        System.out.printf("Average spacing: %.6f ℓ_p%n", avgSpacing);
        System.out.printf("Min spacing: %.6f ℓ_p (%.1f%% compression)%n",
                          minSpacing, (1.0f - minSpacing) * 100);
        System.out.printf("Max spacing: %.6f ℓ_p (%.1f%% stretch)%n",
                          maxSpacing, (maxSpacing - 1.0f) * 100);
        System.out.printf("Total energy: %.6f%n", totalEnergy);
        System.out.printf("Max EM amplitude: %.6f%n", maxEMAmplitude);
        System.out.println();
    }

    /**
     * Save statistics to a CSV file for later analysis.
     */
    public void appendStatisticsToCSV(String filename, int timestep, double elapsedTime) throws IOException {
        boolean fileExists = new java.io.File(filename).exists();

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
            // Write header if new file
            if (!fileExists) {
                writer.println("timestep,elapsed_ms,avg_spacing,total_energy,max_em_amplitude");
            }

            float avgSpacing = lattice.getAverageSpacing();
            float totalEnergy = lattice.getTotalEnergy();
            float maxEMAmplitude = lattice.getMaxEMAmplitude();

            writer.printf("%d,%.3f,%.6f,%.6f,%.6f%n",
                          timestep, elapsedTime, avgSpacing, totalEnergy, maxEMAmplitude);
        }
    }

    /**
     * Generate a PPM image showing annealing activity.
     * Red = actively annealing, temperature shown as intensity.
     */
    public void generateAnnealingActivityImage(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Find max temperature for normalization
        float maxTemp = 0.0001f;
        for (int i = 0; i < lattice.totalSpheres; i++) {
            if (lattice.isAnnealing[i]) {
                maxTemp = Math.max(maxTemp, lattice.annealingTemperature[i]);
            }
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);

                    if (lattice.isAnnealing[index]) {
                        // Red intensity based on temperature
                        float norm = lattice.annealingTemperature[index] / maxTemp;
                        int intensity = (int) (norm * 255);
                        writer.println(intensity + " 0 0");
                    } else {
                        // Black for inactive
                        writer.println("0 0 0");
                    }
                }
            }
        }
    }

    /**
     * Generate a PPM image showing particle boundaries.
     * Each particle gets a unique color.
     */
    public void generateParticleImage(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Create a mapping from sphere index to particle ID
        int[] particleMap = new int[lattice.totalSpheres];
        java.util.Arrays.fill(particleMap, -1);

        for (ParticlePattern particle : lattice.particles) {
            for (int sphereIdx : particle.getParticipatingSpheres()) {
                particleMap[sphereIdx] = particle.getId();
            }
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);
                    int particleId = particleMap[index];

                    if (particleId >= 0) {
                        // Assign unique color based on particle ID
                        int[] rgb = particleIdToColor(particleId);
                        writer.println(rgb[0] + " " + rgb[1] + " " + rgb[2]);
                    } else {
                        // Gray for non-particle spheres
                        writer.println("64 64 64");
                    }
                }
            }
        }
    }

    /**
     * Generate a PPM image showing stability (how long spheres have been in stable configuration).
     */
    public void generateStabilityHeatmap(String filename) throws IOException {
        int width = lattice.gridWidth;
        int height = lattice.gridHeight;

        // Find max stability for normalization
        int maxStability = 1;
        for (int i = 0; i < lattice.totalSpheres; i++) {
            maxStability = Math.max(maxStability, lattice.stabilityHistory[i]);
        }

        // Write PPM file
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("P3");
            writer.println(width + " " + height);
            writer.println("255");

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = lattice.getIndex(x, y);
                    float norm = (float) lattice.stabilityHistory[index] / maxStability;

                    // Green = stable, darker = less stable
                    int intensity = (int) (norm * 255);
                    writer.println("0 " + intensity + " 0");
                }
            }
        }
    }

    /**
     * Convert particle ID to a unique RGB color.
     */
    private int[] particleIdToColor(int id) {
        // Use golden ratio for nice color spread
        float hue = (id * 0.618033988749895f) % 1.0f;
        return hsvToRgb(hue, 0.8f, 0.9f);
    }

    /**
     * Convert HSV to RGB.
     */
    private int[] hsvToRgb(float h, float s, float v) {
        int hi = (int) (h * 6) % 6;
        float f = h * 6 - hi;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);

        float r, g, b;
        switch (hi) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }

        return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }
}
