package dev.averageanime.createmetalwork.neoforge;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.ConfigValues;
import dev.averageanime.createmetalwork.lib.compat.BasinFluidCapacity;
import dev.averageanime.createmetalwork.lib.config.RecipeConditions;
import dev.averageanime.createmetalwork.lib.recipe.RecipeSubjects;
import dev.averageanime.createmetalwork.neoforge.config.ConfigRegistration;
import dev.averageanime.createmetalwork.neoforge.registry.FluidInteractions;
import dev.averageanime.createmetalwork.neoforge.registry.FluidRegistration;
import dev.averageanime.createmetalwork.neoforge.registry.ItemRegistration;
import dev.averageanime.createmetalwork.neoforge.registry.TabRegistration;
import dev.averageanime.createmetalwork.registry.Registered;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@Mod(CreateMetalworkCommon.MOD_ID)
public class CreateMetalwork {

    private static boolean interactionsRegistered;

    public CreateMetalwork(IEventBus modEventBus, ModContainer modContainer) {
        // Read lazily: the mixins run long after this.
        BasinFluidCapacity.gate(ConfigValues::isExpandedBasinFluidsEnabled);

        modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigRegistration.CLIENT_SPEC,
                CreateMetalworkCommon.MOD_ID + "-client.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigRegistration.COMMON_SPEC,
                CreateMetalworkCommon.MOD_ID + "-common.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ConfigRegistration.SERVER_SPEC,
                CreateMetalworkCommon.MOD_ID + "-server.toml");

        // Deferred to client setup: the screen classes do not exist on a dedicated server.
        modEventBus.addListener((FMLClientSetupEvent event) -> {
            IConfigScreenFactory screen = new ConfigRegistration.ConfigScreen();
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, screen);
        });

        // Items first: both share one item register, and FluidRegistration is what attaches it.
        ItemRegistration.register(modEventBus);
        FluidRegistration.register(modEventBus);
        TabRegistration.register(modEventBus);

        // After registration: the predicate reads the maps those calls fill, and recipes are not
        // read until datapack load anyway.
        RecipeSubjects.gate(Registered.names(
                FluidRegistration.BY_ID.keySet(), ItemRegistration.BY_ID.keySet()));
        RecipeConditions.register(modEventBus, CreateMetalworkCommon.MOD_ID);

        // Tags being loaded is the signal, on the game bus. Once per launch: FluidInteractionRegistry has no way to withdraw an entry.
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
            if (interactionsRegistered) return;
            interactionsRegistered = true;
            FluidInteractions.register();
        });

        CreateMetalworkCommon.LOGGER.info("{} loaded", CreateMetalworkCommon.MOD_NAME);
    }
}
