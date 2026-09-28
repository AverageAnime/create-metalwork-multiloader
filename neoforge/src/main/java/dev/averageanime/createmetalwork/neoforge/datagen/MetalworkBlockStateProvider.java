package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.fluid.FluidBlock;
import dev.averageanime.createmetalwork.neoforge.registry.FluidRegistration;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Map;

/** One trivial variant per molten fluid block, pointing at the model {@link MetalworkBlockModelProvider} writes. */
public class MetalworkBlockStateProvider
        extends net.neoforged.neoforge.client.model.generators.BlockStateProvider {

    public MetalworkBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateMetalworkCommon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (Map.Entry<String, FluidBlock.FluidType> entry : FluidRegistration.BY_ID.entrySet()) {
            // The sibling model provider writes this in the same run, so it cannot be resolved yet.
            simpleBlock(entry.getValue().BLOCK.get(), unchecked("block/" + entry.getKey() + "_block"));
        }
    }

    private ModelFile unchecked(String path) {
        return new ModelFile.UncheckedModelFile(
                ResourceLocation.fromNamespaceAndPath(CreateMetalworkCommon.MOD_ID, path));
    }
}
