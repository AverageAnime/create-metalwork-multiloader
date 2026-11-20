package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.lib.datagen.DedupedTagAppender;
import dev.averageanime.createmetalwork.registry.MoltenFluid;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MetalworkTagProvider extends IntrinsicHolderTagsProvider<Item> {

    // Metals with a full dust chain: a clean dust and the dirty dust it is washed from. Must match
    // MetalworkRecipeProvider.DUSTS: its recipes read these tags, so a metal missing here has a dust
    // that nothing can smelt, melt or wash.
    private static final List<String> DUSTS = List.of(
            "adamantite", "andesite", "aquarium", "banglum", "carmot",
            "cincinnasite", "kyber", "lead", "lithium", "manganese",
            "midas_gold", "mythril", "netherite_scrap", "nickel", "orichalcum",
            "palladium", "prometheum", "quadrillum", "runite", "starrite",
            "stormyx", "thallasium", "tin", "titanium", "tungsten",
            "copper", "gold", "iron", "zinc", "aluminum", "osmium", "platinum", "silver", "uranium");

    private static final List<String> MYTHIC_DUSTS = List.of(
            "adamantite", "aquarium", "banglum", "bronze", "carmot", "celestium", "copper", "durasteel",
            "gold", "hallowed", "kyber", "manganese", "metallurgium", "midas_gold", "mythril",
            "orichalcum", "osmium", "palladium", "platinum", "prometheum", "quadrillum", "runite",
            "silver", "star_platinum", "steel", "stormyx", "tin", "unobtainium");

    /**
     * Mythic Metals metals with a raw ore, which it does not publish a conventional tag for. Osmium,
     * platinum, silver and tin matter as much as the rest: Create crushes c:raw_materials/<metal>, so
     * without them those four raw ores could not be crushed at all in a Mythic Metals pack.
     */
    private static final List<String> MYTHIC_RAW = List.of(
            "adamantite", "aquarium", "banglum", "carmot", "kyber", "manganese", "midas_gold",
            "mythril", "orichalcum", "osmium", "palladium", "platinum", "prometheum", "quadrillum",
            "runite", "silver", "stormyx", "tin");

    private record ForeignIngot(String metal, String item) {}

    private static final List<ForeignIngot> FOREIGN_INGOTS = List.of(
            new ForeignIngot("aeternium", "betterend:aeternium_ingot"),
            new ForeignIngot("terminite", "betterend:terminite_ingot"),
            new ForeignIngot("thallasium", "betterend:thallasium_ingot"),
            new ForeignIngot("cincinnasite", "betternether:cincinnasite_ingot"),
            new ForeignIngot("star_platinum", "mythicmetals:star_platinum"),
            new ForeignIngot("starrite", "mythicmetals:starrite"),
            new ForeignIngot("morkite", "mythicmetals:morkite"),
            new ForeignIngot("unobtainium", "mythicmetals:unobtainium"),
            // TFMG tags none of its magnetic alloy, so without this every recipe keyed on the metal is
            // gated off by an empty tag and the fluid is unreachable. Its silicon ingot it does tag.
            new ForeignIngot("magnetic_alloy", "tfmg:magnetic_alloy_ingot"));

    public MetalworkTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.ITEM, registries,
                item -> item.builtInRegistryHolder().key(), CreateMetalworkCommon.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Create: Metalwork item tags";
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        DedupedTagAppender<Item> appender = new DedupedTagAppender<>(this::tag);

        for (String metal : DUSTS) {
            appender.tag(conventional("dusts/" + metal)).addOptional(own(metal + "_dust"));
            appender.tag(conventional("dirty_dusts/" + metal)).addOptional(own("dirty_" + metal + "_dust"));
        }

        // Unobtainium is crushed-only and has no dirty dust, so it gets the clean tag by hand rather
        // than through the loop above.
        appender.tag(conventional("dusts/unobtainium")).addOptional(own("unobtainium_dust"));

        for (String metal : MYTHIC_DUSTS) {
            appender.tag(conventional("dusts/" + metal))
                    .addOptional(ResourceLocation.fromNamespaceAndPath("mythicmetals", metal + "_dust"));
        }
        for (ForeignIngot ingot : FOREIGN_INGOTS) {
            appender.tag(conventional("ingots/" + ingot.metal()))
                    .addOptional(ResourceLocation.parse(ingot.item()));
        }

        // The bucket half of each molten fluid, from the same list MetalworkFluidTagProvider reads.
        for (String entry : AddonDefaults.customFluids()) {
            MoltenFluid fluid = MoltenFluid.parse(entry);
            if (fluid == null) continue;
            appender.tag(conventional("buckets/" + fluid.id())).addOptional(own(fluid.id() + "_bucket"));
        }

        // The two crushed intermediates that have recipes on both sides.
        appender.tag(conventional("crushed_raw_materials/andesite")).addOptional(own("crushed_andesite"));
        appender.tag(conventional("crushed_raw_materials/netherite_scrap")).addOptional(own("crushed_netherite_scrap"));
        // Metallurgy and Northstar each ship a crushed tungsten and tag it only under the flat
        // c:crushed_raw_materials, so without this every crushed-tungsten recipe keyed on the metal
        // missed Metallurgy's, the one most packs have.
        appender.tag(conventional("crushed_raw_materials/tungsten"))
                .addOptional(ResourceLocation.parse("createmetallurgy:crushed_raw_tungsten"))
                .addOptional(ResourceLocation.parse("northstar:crushed_raw_tungsten"));

        // c:crushed_raw_materials/<metal> is hand-authored under common/src/main/resources for all
        // thirty-three metals that have one, so only morkite needs adding -- its file was simply missing,
        // leaving molten_morkite_from_crushed_raw reading a tag nothing defined, so it never loaded.
        appender.tag(conventional("crushed_raw_materials/morkite")).addOptional(own("crushed_morkite"));

        // Without this the raw side of the chain does not exist for these metals. Mythic Metals
        // publishes c:raw_materials only for gold and iron -- its own are under
        // mythicmetals:raw_materials/<metal> -- so every recipe gated on the conventional tag was
        // false and silently absent, which is also why molten_<metal>_from_raw never loaded for them.
        // Nesting their tag rather than naming items means a rename on their side cannot break it.
        for (String metal : MYTHIC_RAW) {
            appender.tag(conventional("raw_materials/" + metal))
                    .addOptionalTag(TagKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath("mythicmetals",
                                    "raw_materials/" + metal)));
        }
        // BetterEnd publishes no raw_materials tag either, and spells the item suffix-first.
        appender.tag(conventional("raw_materials/thallasium"))
                .addOptional(ResourceLocation.parse("betterend:thallasium_raw"));
        // BetterNether's raw form is the shard named after the metal: the ore drops one to three of it,
        // Fortune-boosted, and each smelts to a single ingot. Nothing crafts it back, so it is safe to crush.
        appender.tag(conventional("raw_materials/cincinnasite"))
                .addOptional(ResourceLocation.parse("betternether:cincinnasite"));

        // TFMG's, Metallurgy's and Simple Ore Doubling's solid slag, for the recipes
        // MetalworkRecipeProvider.slagSolids restates on these tags. All three are put on one rate there,
        // a block being nine slag, so any of them stands in for any other.
        for (String mod : new String[] {"tfmg", "createmetallurgy", "create_simple_ore_doubling"}) {
            appender.tag(conventional("slag")).addOptional(ResourceLocation.fromNamespaceAndPath(mod, "slag"));
            appender.tag(conventional("storage_blocks/slag"))
                    .addOptional(ResourceLocation.fromNamespaceAndPath(mod, "slag_block"));
        }

        CreateMetalworkCommon.LOGGER.info("Generated {} item tag entries", appender.emittedCount());
    }

    private static TagKey<Item> conventional(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static ResourceLocation own(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateMetalworkCommon.MOD_ID, path);
    }
}
