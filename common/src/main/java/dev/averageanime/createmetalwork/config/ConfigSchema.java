package dev.averageanime.createmetalwork.config;

import dev.averageanime.createmetalwork.lib.config.ConfigSpecBuilder;
import dev.averageanime.createmetalwork.lib.config.ConfigSpecOverlay;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ConfigSchema {

    private static final Predicate<Object> STRING = obj -> obj instanceof String;

    private ConfigSchema() {}

    /**
     * Each builder is wrapped so an addon can supply the default an option is declared with, keyed by its
     * dotted path. Every {@code define} below still names this mod's own default; the wrapper is what
     * merges anything an addon adds to it.
     */
    public static void build(ConfigSpecBuilder client, ConfigSpecBuilder common, ConfigSpecBuilder server) {
        ConfigSpecBuilder wrappedClient = ConfigSpecOverlay.wrap(client, AddonDefaults.forFile(AddonDefaults.CLIENT));
        ConfigSpecBuilder wrappedCommon = ConfigSpecOverlay.wrap(common, AddonDefaults.forFile(AddonDefaults.COMMON));
        ConfigSpecBuilder wrappedServer = ConfigSpecOverlay.wrap(server, AddonDefaults.forFile(AddonDefaults.SERVER));
        buildCommon(wrappedCommon);
        buildServer(wrappedServer);
        ConfigSpecOverlay.reportDeclared(wrappedClient, wrappedCommon, wrappedServer);
    }

    private static void buildCommon(ConfigSpecBuilder common) {
        common.push("addons");
        common.defineList("addons_enabled", AddonGate.defaults(),
                () -> "addon_id", STRING, true);
        common.pop();

        common.push("blocks");
        common.defineList("fluid", ConfigDefaults.CUSTOM_FLUID_DEFAULT,
                () -> "name  OR  name|slopeFindDistance|levelDecreasePerBlock",
                STRING, true);
        common.pop();

        common.push("items");
        common.defineList("item", ConfigDefaults.CUSTOM_ITEM_DEFAULT,
                () -> "name  OR  name|maxStack",
                STRING, true);
        common.pop();
    }

    private static void buildServer(ConfigSpecBuilder server) {
        server.push("compat");
        server.push("create");
        ConfigValues.CREATE_EXPANDED_BASIN_FLUIDS = server.defineBool("expanded_basin_fluids", true);
        server.pop();
        server.pop();
    }
}
