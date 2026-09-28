package dev.averageanime.createmetalwork.neoforge.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.config.ConfigBootstrap;
import dev.averageanime.createmetalwork.platform.Services;
import dev.averageanime.createmetalwork.lib.fluid.FluidBlock;
import dev.averageanime.createmetalwork.registry.MoltenFluid;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FluidRegistration {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CreateMetalworkCommon.MOD_ID);
    public static final DeferredRegister<net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CreateMetalworkCommon.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CreateMetalworkCommon.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(CreateMetalworkCommon.MOD_ID);

    public static final Map<String, FluidBlock.FluidType> BY_ID = new LinkedHashMap<>();

    static {
        // Must precede the first declaration.
        FluidBlock.configure(new FluidBlock.Registrars(
                CreateMetalworkCommon.MOD_ID, FLUIDS, FLUID_TYPES, BLOCKS, ITEMS));
    }

    private FluidRegistration() {}

    public static void register(IEventBus modEventBus) {
        int skipped = 0;
        for (String entry : ConfigBootstrap.read(ConfigBootstrap.FLUIDS, AddonDefaults.customFluids())) {
            MoltenFluid fluid = MoltenFluid.parse(entry);
            if (fluid == null) continue;
            if (!fluid.condition().isSatisfied(Services.PLATFORM)) {
                skipped++;
                continue;
            }
            if (BY_ID.containsKey(fluid.id())) {
                CreateMetalworkCommon.LOGGER.warn("Skipping duplicate fluid entry: {}", fluid.id());
                continue;
            }
            BY_ID.put(fluid.id(), molten(fluid).build());
        }
        CreateMetalworkCommon.LOGGER.info("Registering {} molten fluids ({} deferred to another mod)",
                BY_ID.size(), skipped);

        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static FluidBlock molten(MoltenFluid fluid) {
        return new FluidBlock(fluid.id())
                .lightLevel(MoltenFluid.LIGHT_LEVEL)
                .temperature(MoltenFluid.TEMPERATURE)
                .bucketSounds(SoundEvents.BUCKET_FILL_LAVA, SoundEvents.BUCKET_EMPTY_LAVA)
                .burnsEntities()
                .fog(MoltenFluid.FOG_START, MoltenFluid.FOG_END)
                .physics(fluid.density(), fluid.viscosity())
                .flow(fluid.slopeFindDistance(), fluid.levelDecreasePerBlock())
                .tickRate(fluid.tickRate());
    }
}
