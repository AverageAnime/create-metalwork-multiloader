package dev.averageanime.createmetalwork.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.config.RegistryCondition;

import org.jetbrains.annotations.Nullable;

/** One parsed line of the fluid config: {@code name|slope|level|density|viscosity|tickRate}, all but the name optional, with an optional trailing {@code @} clause. */
public record MoltenFluid(String id, int slopeFindDistance, int levelDecreasePerBlock,
                          int density, int viscosity, int tickRate, RegistryCondition condition) {

    public static final int TEMPERATURE = 1300;

    public static final int LIGHT_LEVEL = 15;

    public static final float FOG_START = 0.0f;
    public static final float FOG_END = 1f / 48f;

    private static final int DEFAULT_SLOPE = 4;
    private static final int DEFAULT_LEVEL = 2;
    private static final int DEFAULT_DENSITY = 8000;
    private static final int DEFAULT_VISCOSITY = 70;
    private static final int DEFAULT_TICK_RATE = 15;

    /** @return the parsed fluid, or null if the line is malformed (already logged) */
    public static @Nullable MoltenFluid parse(String entry) {
        RegistryCondition.Split split = RegistryCondition.split(entry, CreateMetalworkCommon.LOGGER);
        if (split == null) return null;

        String[] p = split.payload().split("\\|");
        if (p.length != 1 && p.length != 3 && p.length != 6) {
            CreateMetalworkCommon.LOGGER.warn(
                    "Skipping fluid entry: expected name, name|slope|level, or "
                            + "name|slope|level|density|viscosity|tickRate, got: {}", entry);
            return null;
        }
        try {
            String id = p[0];
            if (id.isBlank()) throw new NumberFormatException("blank id");
            int slope = p.length >= 3 ? Integer.parseInt(p[1]) : DEFAULT_SLOPE;
            int level = p.length >= 3 ? Integer.parseInt(p[2]) : DEFAULT_LEVEL;
            int density = p.length == 6 ? Integer.parseInt(p[3]) : DEFAULT_DENSITY;
            int viscosity = p.length == 6 ? Integer.parseInt(p[4]) : DEFAULT_VISCOSITY;
            int tickRate = p.length == 6 ? Integer.parseInt(p[5]) : DEFAULT_TICK_RATE;
            return new MoltenFluid(id, slope, level, density, viscosity, tickRate, split.condition());
        } catch (NumberFormatException e) {
            CreateMetalworkCommon.LOGGER.warn("Skipping fluid entry with a non-numeric field: {}", entry);
            return null;
        }
    }
}
