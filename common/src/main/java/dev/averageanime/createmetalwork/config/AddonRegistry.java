package dev.averageanime.createmetalwork.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.config.AddonSpecLoader;
import dev.averageanime.createmetalwork.platform.Services;

import java.util.List;

/** Maps each section of {@code data/createmetalwork/addon.json} to the pipe-delimited form the config uses. */
public final class AddonRegistry {

    /** Probed in every mod file. */
    public static final String ADDON_PATH = "data/" + CreateMetalworkCommon.MOD_ID + "/addon.json";

    /** Extra roots to scan, separated by the platform path separator. */
    public static final String ADDON_ROOTS_PROPERTY = CreateMetalworkCommon.MOD_ID + ".addonRoots";

    private static final int SUPPORTED_SPEC_VERSION = 1;

    private static final java.util.function.Predicate<Object> STRING = obj -> obj instanceof String;

    private static final String FLUIDS = "fluids";
    private static final String ITEMS = "items";

    /**
     * No {@code builtin} specs: this mod ships none of its own, so the probed {@link #ADDON_PATH} is the
     * only source besides the dev roots.
     */
    private static final AddonSpecLoader LOADER =
            new AddonSpecLoader(ADDON_PATH, ADDON_ROOTS_PROPERTY, SUPPORTED_SPEC_VERSION,
                    CreateMetalworkCommon.LOGGER, path -> Services.PLATFORM.findModResources(path))
                    .gate(AddonGate::isEnabled)
                    .section(FLUIDS, STRING, AddonRegistry::fluid)
                    .section(ITEMS, STRING, AddonRegistry::item);

    private AddonRegistry() {}

    public static List<String> fluids() { return LOADER.get(FLUIDS); }
    public static List<String> items()  { return LOADER.get(ITEMS); }

    /** Every loaded addon, whatever the gate says. */
    public static List<AddonSpecLoader.Addon> addons() { return LOADER.addons(); }

    /** A section's entries from every enabled addon, in load order. */
    public static List<String> from(String section) { return LOADER.get(section); }

    public static List<AddonSpecLoader.ConfigContribution> configContributions() {
        return LOADER.configContributions();
    }

    /**
     * {@code name}, {@code name|slope|level}, or all six fields. The physical trio is only honoured as
     * part of a complete six-field line -- {@link dev.averageanime.createmetalwork.registry.MoltenFluid}
     * reads them on {@code p.length == 6} alone -- so a partial line is written out in full here rather
     * than left to be silently ignored.
     */
    private static void fluid(JsonObject o, AddonSpecLoader.Sink sink) {
        String id = str(o, "id", null);
        if (id == null) return;
        boolean physical = o.has("density") || o.has("viscosity") || o.has("tick_rate");
        String entry;
        if (physical || o.has("slope") || o.has("level")) {
            entry = id + "|" + num(o, "slope", "4") + "|" + num(o, "level", "2");
            if (physical) {
                entry += "|" + num(o, "density", "8000")
                        + "|" + num(o, "viscosity", "70")
                        + "|" + num(o, "tick_rate", "15");
            }
        } else {
            entry = id;
        }
        sink.add(FLUIDS, withCondition(entry, o));
    }

    private static void item(JsonObject o, AddonSpecLoader.Sink sink) {
        String id = str(o, "id", null);
        if (id == null) return;
        String entry = o.has("max_stack") ? id + "|" + num(o, "max_stack", "64") : id;
        sink.add(ITEMS, withCondition(entry, o));
    }

    /** Appends the entry's optional {@code condition}, with or without the leading {@code @}. */
    private static String withCondition(String entry, JsonObject o) {
        String condition = str(o, "condition", null);
        if (condition == null || condition.isBlank()) return entry;
        return entry + (condition.startsWith("@") ? condition : "@" + condition);
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static String num(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }
}
