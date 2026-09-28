package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.datagen.DatagenHelpers;
import dev.averageanime.createmetalwork.neoforge.registry.FluidRegistration;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** The inset cube every placed molten fluid uses; the shape itself lives in the shared library. */
public class MetalworkBlockModelProvider
        extends net.neoforged.neoforge.client.model.generators.BlockModelProvider {

    public MetalworkBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateMetalworkCommon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (String fluidId : FluidRegistration.BY_ID.keySet()) {
            DatagenHelpers.insetBlockModel(this, CreateMetalworkCommon.MOD_ID, fluidId);
        }
    }
}
