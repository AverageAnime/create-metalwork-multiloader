package dev.averageanime.createmetalwork.config;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.config.RegistryCondition;
import dev.averageanime.createmetalwork.lib.config.TomlListReader;
import dev.averageanime.createmetalwork.platform.Services;

import java.util.List;

public final class ConfigBootstrap {

    public static final String FLUIDS = "blocks.fluid";
    public static final String ITEMS = "items.item";

    // AddonGate.KEY is read from the same file, and the reader caches it on first load, so it has to be
    // named here or it reads as absent for the rest of the launch.
    private static final List<String> KEYS = List.of(FLUIDS, ITEMS, AddonGate.KEY);

    private static volatile TomlListReader reader;

    private ConfigBootstrap() {}

    public static List<String> read(String key, List<String> defaults) {
        if (Services.PLATFORM.isRunningDataGen()) return defaults;
        return RegistryCondition.inherit(reader().read(key, defaults), defaults,
                ConfigBootstrap::idOf, CreateMetalworkCommon.LOGGER);
    }

    /** A stored list with no condition handling; {@code @} clauses are meaningless here. */
    public static List<String> readRaw(String key, List<String> defaults) {
        return reader().read(key, defaults);
    }

    /** The name at the front of a line, before any {@code |} tuning fields. */
    private static String idOf(String payload) {
        int bar = payload.indexOf('|');
        return bar < 0 ? payload : payload.substring(0, bar);
    }

    private static TomlListReader reader() {
        TomlListReader local = reader;
        if (local != null) return local;
        synchronized (ConfigBootstrap.class) {
            if (reader == null) {
                reader = new TomlListReader(
                        Services.PLATFORM.getConfigDir()
                                .resolve(CreateMetalworkCommon.MOD_ID + "-common.toml"),
                        KEYS, CreateMetalworkCommon.LOGGER);
            }
            return reader;
        }
    }
}
