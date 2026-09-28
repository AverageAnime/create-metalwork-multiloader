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

    // Metals with a full dust chain: a clean dust and the dirty dust it is washed from.
    private static final List<String> DUSTS = List.of(
            "adamantite", "andesite", "aquarium", "banglum", "carmot",
            "cincinnasite", "kyber", "lead", "lithium", "manganese",
            "midas_gold", "mythril", "netherite_scrap", "nickel", "orichalcum",
            "palladium", "prometheum", "quadrillum", "runite", "starrite",
            "stormyx", "thallasium", "tin", "tungsten");

    private static final List<String> MYTHIC_DUSTS = List.of(
            "adamantite", "aquarium", "banglum", "bronze", "carmot", "celestium", "copper", "durasteel",
            "gold", "hallowed", "kyber", "manganese", "metallurgium", "midas_gold", "mythril",
            "orichalcum", "osmium", "palladium", "platinum", "prometheum", "quadrillum", "runite",
            "silver", "star_platinum", "steel", "stormyx", "tin", "unobtainium");

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
        // Both sides of this one: ours registers only when Northstar is absent, theirs when it is present.
        appender.tag(conventional("crushed_raw_materials/tungsten"))
                .addOptional(own("crushed_raw_tungsten"))
                .addOptional(ResourceLocation.parse("northstar:crushed_raw_tungsten"));

        CreateMetalworkCommon.LOGGER.info("Generated {} item tag entries", appender.emittedCount());
    }

    private static TagKey<Item> conventional(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
    }

    private static ResourceLocation own(String path) {
        return ResourceLocation.fromNamespaceAndPath(CreateMetalworkCommon.MOD_ID, path);
    }
}
