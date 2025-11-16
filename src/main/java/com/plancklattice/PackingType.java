package com.plancklattice;

/**
 * Enumeration of lattice packing configurations.
 * Different coordination numbers lead to different stable patterns.
 */
public enum PackingType {
    /**
     * Simple cubic packing - 6 neighbors (up, down, left, right, front, back in 3D).
     * In 2D: 4 neighbors.
     */
    SIMPLE_CUBIC(6, 0.0f),

    /**
     * Face-centered cubic (FCC) - 12 neighbors, densest sphere packing in 3D.
     * Very stable configuration.
     */
    FCC(12, -1.0f),

    /**
     * Hexagonal close packing (HCP) - 12 neighbors, equally dense as FCC.
     * Different stacking sequence than FCC.
     */
    HCP(12, -1.0f),

    /**
     * Body-centered cubic - 8 neighbors, moderately dense.
     */
    BODY_CENTERED(8, -0.5f),

    /**
     * Hexagonal (2D) - 6 neighbors in hexagonal arrangement.
     * Optimal 2D packing.
     */
    HEXAGONAL_2D(6, -0.8f),

    /**
     * Square (2D) - 4 neighbors in square grid.
     */
    SQUARE_2D(4, 0.0f),

    /**
     * Custom or unclassified configuration.
     */
    CUSTOM(0, 0.5f);

    private final int idealCoordination;
    private final float energyBonus;  // Negative = more stable

    PackingType(int idealCoordination, float energyBonus) {
        this.idealCoordination = idealCoordination;
        this.energyBonus = energyBonus;
    }

    /**
     * Get the ideal number of neighbors for this packing type.
     */
    public int getIdealCoordination() {
        return idealCoordination;
    }

    /**
     * Get the energy bonus/penalty for this packing type.
     * Negative values indicate more stable configurations.
     */
    public float getEnergyBonus() {
        return energyBonus;
    }

    /**
     * Classify packing type based on coordination number.
     */
    public static PackingType fromCoordination(int coordination) {
        return switch (coordination) {
            case 4 -> SQUARE_2D;
            case 6 -> HEXAGONAL_2D;  // Could be SIMPLE_CUBIC in 3D
            case 8 -> BODY_CENTERED;
            case 12 -> FCC;  // Could also be HCP
            default -> CUSTOM;
        };
    }
}
