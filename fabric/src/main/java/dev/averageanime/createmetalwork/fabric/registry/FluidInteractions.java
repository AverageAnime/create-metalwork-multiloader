package dev.averageanime.createmetalwork.fabric.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.registry.Solidification;
import io.github.fabricators_of_create.porting_lib.fluids.FluidInteractionRegistry;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import io.github.fabricators_of_create.porting_lib.fluids.extensions.FluidExtension;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/** Runs after block tags are bound, or {@link Solidification} resolves nothing. */
public final class FluidInteractions {

    private FluidInteractions() {}

    public static void register() {
        int registered = 0;
        BlockState flowingState = Solidification.flowingResult().defaultBlockState();
        Set<FluidType> seen = new HashSet<>();

        Solidification.Targets targets = Solidification.resolveAll();
        for (Solidification.Target target : targets.resolved()) {
            FluidType type = ((FluidExtension) target.fluid()).getFluidType();
            if (type == PortingLibFluids.EMPTY_TYPE || !seen.add(type)) continue;

            BlockState solidState = target.block().defaultBlockState();
            FluidInteractionRegistry.addInteraction(type,
                    new FluidInteractionRegistry.InteractionInformation(
                            PortingLibFluids.WATER_TYPE,
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
