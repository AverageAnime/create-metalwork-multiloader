package dev.averageanime.createmetalwork.fabric;

import dev.averageanime.createmetalwork.fabric.registry.FluidRegistration;
import net.fabricmc.api.ClientModInitializer;

public class CreateMetalworkClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FluidRegistration.initClient();
    }
}
