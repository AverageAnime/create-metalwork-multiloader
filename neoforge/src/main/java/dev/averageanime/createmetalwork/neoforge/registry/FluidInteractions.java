package dev.averageanime.createmetalwork.neoforge.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.registry.Solidification;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.HashSet;
import java.util.Set;

public final class FluidInteractions {

    private FluidInteractions() {}

    public static void register() {
        int registered = 0;
        BlockState flowingState = Solidification.flowingResult().defaultBlockState();
        Set<FluidType> seen = new HashSet<>();

        Solidification.Targets targets = Solidification.resolveAll();
        for (Solidification.Target target : targets.resolved()) {
            FluidType type = target.fluid().getFluidType();
            if (!seen.add(type)) continue;

            BlockState solidState = target.block().defaultBlockState();
            FluidInteractionRegistry.addInteraction(type,
                    new FluidInteractionRegistry.InteractionInformation(
                            NeoForgeMod.WATER_TYPE.value(),
                            state -> state.isSource() ? solidState : flowingState));
            registered++;
        }

        if (targets.unresolved().isEmpty()) {
            CreateMetalworkCommon.LOGGER.info("Registered {} fluid interactions", registered);
        } else {
            CreateMetalworkCommon.LOGGER.info(
                    "Registered {} fluid interactions ({} metals have no block installed: {})",
                    registered, targets.unresolved().size(), String.join(", ", targets.unresolved()));
        }
    }
}
