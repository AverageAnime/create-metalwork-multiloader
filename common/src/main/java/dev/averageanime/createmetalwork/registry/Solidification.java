package dev.averageanime.createmetalwork.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** What a molten fluid becomes when it meets water, resolved from {@code c:storage_blocks/<metal>} with an id scan as fallback. */
public final class Solidification {

    public static final String FLOWING_RESULT = "minecraft:stone";

    private static final String MOLTEN_PREFIX = "molten_";

    /** Metals whose block a tag cannot name. */
    private static final Map<String, String> OVERRIDES = new LinkedHashMap<>();

    static {
        OVERRIDES.put("andesite", "minecraft:andesite");
        OVERRIDES.put("andesite_alloy", "create:andesite_alloy_block");
        // TFMG ships no magnetic_alloy_block and no c:storage_blocks/magnetic_alloy, so neither the tag
        // nor the id scan finds anything. Its laminated block is nine sheets, so nine ingots, which is
        // what a storage block would have been worth anyway.
        OVERRIDES.put("magnetic_alloy", "tfmg:laminated_magnetic_alloy_block");
    }

    private static final List<String> PREFERRED_NAMESPACES = List.of(
            "minecraft",
            "create",
            "alltheores",
            "createmetallurgy",
            "create_ironworks",
            "createbigcannons",
            "northstar",
            "mythicmetals",
            "tfmg");

    private static final Comparator<Block> BY_PREFERENCE = Comparator
            .comparingInt((Block block) -> {
                int rank = PREFERRED_NAMESPACES.indexOf(namespaceOf(block));
                return rank < 0 ? PREFERRED_NAMESPACES.size() : rank;
            })
            .thenComparing(Solidification::namespaceOf);

    private Solidification() {}

    public record Target(Fluid fluid, Block block) {}

    /** @param unresolved metal names that had a fluid but no block anywhere, named so the log can say which */
    public record Targets(List<Target> resolved, List<String> unresolved) {}

    /** Every molten source fluid paired with what it leaves behind. Must run after block tags are bound. */
    public static Targets resolveAll() {
        List<Target> resolved = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        Map<String, Block> cache = new LinkedHashMap<>();

        for (Map.Entry<ResourceKey<Fluid>, Fluid> entry : BuiltInRegistries.FLUID.entrySet()) {
            String path = entry.getKey().location().getPath();
            if (!path.startsWith(MOLTEN_PREFIX)) continue;

            Fluid fluid = entry.getValue();
            if (fluid == Fluids.EMPTY) continue;
            // A flowing variant named molten_iron_flowing would otherwise register a second interaction.
            if (!fluid.isSource(fluid.defaultFluidState())) continue;

            String metal = path.substring(MOLTEN_PREFIX.length());
            if (metal.isEmpty()) continue;

            // Misses are cached too, or two mods registering the same molten_<metal> each pay for a full scan.
            if (!cache.containsKey(metal)) cache.put(metal, resolveFor(metal));
            Block block = cache.get(metal);
            if (block == null) {
                if (!unresolved.contains(metal)) unresolved.add(metal);
                continue;
            }
            resolved.add(new Target(fluid, block));
        }
        return new Targets(List.copyOf(resolved), List.copyOf(unresolved));
    }

    public static @Nullable Block resolveFor(String metal) {
        String override = OVERRIDES.get(metal);
        if (override != null) return resolveBlock(override);

        Block tagged = fromStorageBlockTag(metal);
        return tagged != null ? tagged : fromIdScan(metal);
    }

    /** The best {@code c:storage_blocks/<metal>} member. */
    private static @Nullable Block fromStorageBlockTag(String metal) {
        ResourceLocation id = ResourceLocation.tryBuild("c", "storage_blocks/" + metal);
        if (id == null) return null;

        Optional<HolderSet.Named<Block>> tag =
                BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, id));
        return tag.flatMap(holders -> holders.stream()
                .map(Holder::value)
                .filter(block -> block != Blocks.AIR)
                .min(BY_PREFERENCE)).orElse(null);

    }

    /** For mods that ship the block but not the tag: the best block whose path is {@code <metal>_block}. */
    private static @Nullable Block fromIdScan(String metal) {
        String wanted = metal + "_block";
        Block best = null;
        for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            if (!entry.getKey().location().getPath().equals(wanted)) continue;
            Block block = entry.getValue();
            if (block == Blocks.AIR) continue;
            if (best == null || BY_PREFERENCE.compare(block, best) < 0) best = block;
        }
        return best;
    }

    private static String namespaceOf(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace();
    }

    public static @Nullable Block resolveBlock(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        return BuiltInRegistries.BLOCK.containsKey(key) ? BuiltInRegistries.BLOCK.get(key) : null;
    }

    public static Block flowingResult() {
        Block block = resolveBlock(FLOWING_RESULT);
        return block != null ? block : Blocks.STONE;
    }
}
