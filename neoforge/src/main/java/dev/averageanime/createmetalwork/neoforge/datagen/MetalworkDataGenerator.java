package dev.averageanime.createmetalwork.neoforge.datagen;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = CreateMetalworkCommon.MOD_ID)
public final class MetalworkDataGenerator {

    private MetalworkDataGenerator() {}

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new MetalworkBlockStateProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new MetalworkBlockModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new MetalworkItemModelProvider(output, existingFiles));

        generator.addProvider(event.includeServer(), new MetalworkRecipeProvider(output));
        generator.addProvider(event.includeServer(), new MetalworkTagProvider(
                output, event.getLookupProvider(), existingFiles));
        generator.addProvider(event.includeServer(), new MetalworkFluidTagProvider(
                output, event.getLookupProvider(), existingFiles));
    }
}
