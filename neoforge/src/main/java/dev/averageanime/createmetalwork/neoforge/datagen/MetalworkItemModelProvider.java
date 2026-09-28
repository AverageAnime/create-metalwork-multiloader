package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.neoforge.registry.FluidRegistration;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Every Metalwork item is a flat sprite. Buckets and dusts share one register, so one pass covers both. */
public class MetalworkItemModelProvider
        extends net.neoforged.neoforge.client.model.generators.ItemModelProvider {

    public MetalworkItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CreateMetalworkCommon.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        FluidRegistration.ITEMS.getEntries().forEach(holder -> {
            String id = holder.getId().getPath();
            withExistingParent(id, "item/generated")
                    .texture("layer0", ResourceLocation.fromNamespaceAndPath(
                            CreateMetalworkCommon.MOD_ID, "item/" + id));
        });
    }
}
