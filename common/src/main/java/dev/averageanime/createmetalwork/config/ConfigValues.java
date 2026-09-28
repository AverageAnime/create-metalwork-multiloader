package dev.averageanime.createmetalwork.config;

import java.util.function.Supplier;

/** Read through suppliers so a config reload takes effect. */
public final class ConfigValues {

    static Supplier<Boolean> CREATE_EXPANDED_BASIN_FLUIDS = unbound();

    private ConfigValues() {}

    private static <T> Supplier<T> unbound() {
        return () -> { throw new IllegalStateException("Config not built yet"); };
    }

    /** Falls back to the schema default, so an early read behaves as the option's default rather than off. */
    public static boolean isExpandedBasinFluidsEnabled() { try { return CREATE_EXPANDED_BASIN_FLUIDS.get(); } catch (IllegalStateException e) { return true; } }
}
