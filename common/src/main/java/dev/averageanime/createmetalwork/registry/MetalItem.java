package dev.averageanime.createmetalwork.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.config.RegistryCondition;

import org.jetbrains.annotations.Nullable;

public record MetalItem(String id, int maxStack, RegistryCondition condition) {

    private static final int DEFAULT_MAX_STACK = 64;

    public static @Nullable MetalItem parse(String entry) {
        RegistryCondition.Split split = RegistryCondition.split(entry, CreateMetalworkCommon.LOGGER);
        if (split == null) return null;

        String[] p = split.payload().split("\\|");
        if (p.length != 1 && p.length != 2) {
            CreateMetalworkCommon.LOGGER.warn(
                    "Skipping item entry: expected name or name|maxStack, got: {}", entry);
            return null;
        }
        if (p[0].isBlank()) {
            CreateMetalworkCommon.LOGGER.warn("Skipping item entry with a blank name: {}", entry);
            return null;
        }
        int maxStack = DEFAULT_MAX_STACK;
        if (p.length == 2) {
            try {
                maxStack = Integer.parseInt(p[1]);
            } catch (NumberFormatException e) {
                CreateMetalworkCommon.LOGGER.warn("Skipping item entry with a non-numeric stack size: {}", entry);
                return null;
            }
            if (maxStack < 1 || maxStack > 64) {
                CreateMetalworkCommon.LOGGER.warn("Skipping item entry with an out-of-range stack size: {}", entry);
                return null;
            }
        }
        return new MetalItem(p[0], maxStack, split.condition());
    }
}
