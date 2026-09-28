package dev.averageanime.createmetalwork.fabric;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.fabric.config.ConfigRegistration;
import dev.averageanime.createmetalwork.fabric.registry.FluidInteractions;
import dev.averageanime.createmetalwork.fabric.registry.FluidRegistration;
import dev.averageanime.createmetalwork.fabric.registry.ItemRegistration;
import dev.averageanime.createmetalwork.fabric.registry.TabRegistration;
import dev.averageanime.createmetalwork.lib.config.RecipeConditions;
import dev.averageanime.createmetalwork.lib.recipe.RecipeSubjects;
import dev.averageanime.createmetalwork.registry.Registered;
import io.github.fabricators_of_create.porting_lib.config.ConfigRegistry;
import io.github.fabricators_of_create.porting_lib.config.ModConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.api.ModInitializer;

public class CreateMetalwork implements ModInitializer {

    private static boolean interactionsRegistered;

    @Override
    public void onInitialize() {
        ConfigRegistry.registerConfig(CreateMetalworkCommon.MOD_ID, ModConfig.Type.CLIENT,
                ConfigRegistration.CLIENT_SPEC);
        ConfigRegistry.registerConfig(CreateMetalworkCommon.MOD_ID, ModConfig.Type.COMMON,
                ConfigRegistration.COMMON_SPEC);
        ConfigRegistry.registerConfig(CreateMetalworkCommon.MOD_ID, ModConfig.Type.SERVER,
                ConfigRegistration.SERVER_SPEC);

        ItemRegistration.init();
        FluidRegistration.init();
        // Last: the tab reads both registration maps to pick its icon.
        TabRegistration.init();

        // After registration: the predicate reads the maps those calls fill, and recipes are not
        // read until datapack load anyway.
        RecipeSubjects.gate(Registered.names(
                FluidRegistration.BY_ID.keySet(), ItemRegistration.BY_ID.keySet()));
        RecipeConditions.register(CreateMetalworkCommon.MOD_ID);

        // Not from here: block tags are not bound until server start.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (interactionsRegistered) return;
            interactionsRegistered = true;
            FluidInteractions.register();
        });

        CreateMetalworkCommon.LOGGER.info("{} loaded", CreateMetalworkCommon.MOD_NAME);
    }
}
