package dev.averageanime.createmetalwork.registry;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Answers the {@code createmetalwork:enabled} recipe condition: does this bare name still exist?
 *
 * <p>Both fluids and items are registered from an editable config list, so a name that ships in the
 * defaults can be absent at runtime -- the user removed the line, or a {@code @} clause deferred it to
 * another mod. Recipes naming such a thing have to drop out with it.
 */
public final class Registered {

    private Registered() {}

    /** @param fluids and {@code items} live key views of each loader's registration map */
    public static Predicate<String> names(Set<String> fluids, Set<String> items) {
        return id -> items.contains(id) || fluids.contains(id) || derivedFluid(fluids, id);
    }

    /** One fluid entry also registers the flowing form, the block and the bucket. */
    private static boolean derivedFluid(Set<String> fluids, String id) {
        if (id.startsWith("flowing_")) return fluids.contains(id.substring("flowing_".length()));
        if (id.endsWith("_block")) return fluids.contains(id.substring(0, id.length() - "_block".length()));
        if (id.endsWith("_bucket")) return fluids.contains(id.substring(0, id.length() - "_bucket".length()));
        return false;
    }
}
