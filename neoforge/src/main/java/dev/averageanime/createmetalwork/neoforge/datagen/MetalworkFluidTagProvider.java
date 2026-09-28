package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.lib.datagen.DatagenHelpers;
import dev.averageanime.createmetalwork.lib.datagen.DedupedTagAppender;
import dev.averageanime.createmetalwork.registry.MoltenFluid;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * One {@code c:<fluid>} tag per molten fluid this mod knows of, holding the source and its flowing
 * variant.
 *
 * <p>Two passes, because the two lists cover different ground. The config list is every fluid this mod
 * registers itself; {@link Metals#ALL} additionally names the fluids it defers to other mods, and covers
 * metals with no config entry at all -- nethersteel has recipes but no fluid of ours, so only the second
 * pass reaches it.
 *
 * <p>The config list rather than {@link
 * dev.averageanime.createmetalwork.neoforge.registry.FluidRegistration#BY_ID}: that map is filtered by
 * each fluid's {@code RegistryCondition}, and datagen runs with no addons on the classpath, so every
 * fluid gated on another mod would be missing from it.
 *
 * <p>The second pass does name foreign namespaces, deliberately. A mod owning a fluid is no guarantee it
 * tags it -- TFMG ships {@code tfmg:liquid_silicon} with no {@code c:} tag of any kind, so nothing could
 * reach it by tag until this named it. Everything is added optional, so a namespace whose mod is absent
 * leaves an empty tag rather than a broken one, and the appender drops the duplicates that arise where a
 * mod does tag its own.
 */
public class MetalworkFluidTagProvider extends TagsProvider<Fluid> {

    public MetalworkFluidTagProvider(PackOutput output,
                                     CompletableFuture<HolderLookup.Provider> registries,
                                     ExistingFileHelper existingFileHelper) {
        super(output, Registries.FLUID, registries, CreateMetalworkCommon.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Create: Metalwork fluid tags";
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        DedupedTagAppender<Fluid> appender = new DedupedTagAppender<>(this::tag);
        BiConsumer<TagKey<Fluid>, ResourceLocation> add = (key, id) -> appender.tag(key).addOptional(id);

        for (String entry : AddonDefaults.customFluids()) {
            MoltenFluid fluid = MoltenFluid.parse(entry);
            if (fluid == null) continue;
            DatagenHelpers.conventionTag(add,
                    CreateMetalworkCommon.MOD_ID, fluid.id(), "flowing_" + fluid.id());
        }

        for (Metal metal : Metals.ALL) {
            String convention = "molten_" + metal.name();
            for (String owner : metal.fluidOwners()) {
                ResourceLocation fluid = ResourceLocation.parse(Metal.fluidOf(owner, metal.name()));
                String path = fluid.getPath();
                DatagenHelpers.conventionTag(add, convention,
                        fluid.getNamespace(), path, "flowing_" + path);
                // A fluid the owner named its own way earns its own tag too, so anything looking for it
                // by that name finds it -- TFMG never emits c:liquid_silicon itself.
                if (!path.equals(convention)) {
                    DatagenHelpers.conventionTag(add, fluid.getNamespace(), path, "flowing_" + path);
                }
            }
        }

        CreateMetalworkCommon.LOGGER.info("Generated {} fluid tag entries", appender.emittedCount());
    }
}
