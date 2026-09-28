package dev.averageanime.createmetalwork.neoforge.datagen;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @param fluidOwners the other mods that ship this metal's molten fluid. A bare mod id means the fluid
 *                    is that mod's {@code molten_<name>}; an entry containing a colon names the fluid
 *                    outright, for a mod that does not follow the convention -- {@code
 *                    tfmg:liquid_silicon}.
 */
public record Metal(String name, Heat heat, List<String> fluidOwners, List<Source> sources,
                    boolean standardForms) {

    public enum Heat {
        HEATED("heated"),
        SUPERHEATED("superheated");

        public final String json;

        Heat(String json) {
            this.json = json;
        }
    }

    public record Source(@Nullable String modId, @Nullable String ingot,
                         @Nullable String nugget, @Nullable String block) {

        public static Source always(@Nullable String ingot, @Nullable String nugget,
                                    @Nullable String block) {
            return new Source(null, ingot, nugget, block);
        }

        public static Source from(String modId, String ingot, @Nullable String nugget, @Nullable String block) {
            return new Source(modId, ingot, nugget, block);
        }

        public static Source standard(String modId, String metal) {
            return new Source(modId, modId + ":" + metal + "_ingot",
                    modId + ":" + metal + "_nugget", modId + ":" + metal + "_block");
        }
    }

    /** The fluid a {@link #fluidOwners()} entry names: its own id, or the owner's {@code molten_<metal>}. */
    public static String fluidOf(String owner, String metal) {
        return owner.indexOf(':') < 0 ? owner + ":molten_" + metal : owner;
    }

    /** The mod id a {@link #fluidOwners()} entry belongs to. */
    public static String ownerOf(String owner) {
        int colon = owner.indexOf(':');
        return colon < 0 ? owner : owner.substring(0, colon);
    }

    public List<Source> reachableSources() {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).modId() == null) return sources.subList(0, i + 1);
        }
        return sources;
    }

    public boolean standardForms() {
        return standardForms;
    }

    public boolean fluidCollides() {
        return !fluidOwners.isEmpty();
    }
}
