package dev.averageanime.createmetalwork.fabric.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.config.ConfigBootstrap;
import dev.averageanime.createmetalwork.platform.Services;
import dev.averageanime.createmetalwork.lib.fluid.FluidBlock;
import dev.averageanime.createmetalwork.registry.MoltenFluid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fabric registers eagerly, and its builder has no density or temperature options. */
public final class FluidRegistration {

    private static final List<FluidBlock> ALL = new ArrayList<>();
    public static final Map<String, FluidBlock> BY_ID = new LinkedHashMap<>();

    private FluidRegistration() {}

    public static void init() {
        // Must precede the first declaration.
        FluidBlock.configure(CreateMetalworkCommon.MOD_ID);

        int skipped = 0;
        for (String entry : ConfigBootstrap.read(ConfigBootstrap.FLUIDS, AddonDefaults.customFluids())) {
            MoltenFluid fluid = MoltenFluid.parse(entry);
            if (fluid == null) continue;
            if (!fluid.condition().isSatisfied(Services.PLATFORM)) {
                CreateMetalworkCommon.LOGGER.info("Not registering {}: {}", fluid.id(), fluid.condition().reason(Services.PLATFORM));
                skipped++;
                continue;
            }
            if (BY_ID.containsKey(fluid.id())) {
                CreateMetalworkCommon.LOGGER.warn("Skipping duplicate fluid entry: {}", fluid.id());
                continue;
            }
            FluidBlock built = new FluidBlock(fluid.id())
                    .burnsEntities()
                    .fog(MoltenFluid.FOG_START, MoltenFluid.FOG_END)
                    .flow(fluid.slopeFindDistance(), fluid.levelDecreasePerBlock())
                    .build();
            ALL.add(built);
            BY_ID.put(fluid.id(), built);
        }
        CreateMetalworkCommon.LOGGER.info("Registering {} molten fluids ({} deferred to another mod)",
                BY_ID.size(), skipped);
    }

    public static void initClient() {
        for (FluidBlock fluid : ALL) fluid.registerClientRendering();
    }
}
