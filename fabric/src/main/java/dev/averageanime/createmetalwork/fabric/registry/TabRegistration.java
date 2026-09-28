package dev.averageanime.createmetalwork.fabric.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.lib.fluid.FluidBlock;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Contents are read off the registration maps, so config-enabled content turns up without a second edit. */
public final class TabRegistration {

    private TabRegistration() {}

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(CreateMetalworkCommon.MOD_ID, "base"),
                FabricItemGroup.builder()
                        .title(Component.translatable("tab." + CreateMetalworkCommon.MOD_ID))
                        .icon(TabRegistration::icon)
                        .displayItems((parameters, output) -> {
                            for (Item item : ItemRegistration.BY_ID.values()) output.accept(item);
                            for (FluidBlock fluid : FluidRegistration.BY_ID.values()) output.accept(fluid.BUCKET);
                        })
                        .build());
    }

    /** Empty only when nothing at all registered. */
    private static ItemStack icon() {
        for (Item item : ItemRegistration.BY_ID.values()) return new ItemStack(item);
        for (FluidBlock fluid : FluidRegistration.BY_ID.values()) return new ItemStack(fluid.BUCKET);
        return ItemStack.EMPTY;
    }
}
