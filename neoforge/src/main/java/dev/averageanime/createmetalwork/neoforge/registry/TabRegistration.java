package dev.averageanime.createmetalwork.neoforge.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.fluid.FluidBlock;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public final class TabRegistration {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateMetalworkCommon.MOD_ID);

    public static final Holder<CreativeModeTab> BASE = TABS.register("base", TabRegistration::base);

    private TabRegistration() {}

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }

    private static CreativeModeTab base(ResourceLocation id) {
        return CreativeModeTab.builder()
                .title(Component.translatable("tab." + CreateMetalworkCommon.MOD_ID))
                .icon(TabRegistration::icon)
                .displayItems((parameters, output) -> {
                    for (DeferredItem<?> item : ItemRegistration.BY_ID.values()) {
                        output.accept(item.get());
                    }
                    for (FluidBlock.FluidType fluid : FluidRegistration.BY_ID.values()) {
                        output.accept(fluid.BUCKET.get());
                    }
                })
                .build();
    }

    private static ItemStack icon() {
        for (Map.Entry<String, DeferredItem<net.minecraft.world.item.Item>> entry : ItemRegistration.BY_ID.entrySet()) {
            return new ItemStack(entry.getValue().get());
        }
        for (FluidBlock.FluidType fluid : FluidRegistration.BY_ID.values()) {
            return new ItemStack(fluid.BUCKET.get());
        }
        return ItemStack.EMPTY;
    }
}
