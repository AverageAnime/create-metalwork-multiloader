package dev.averageanime.createmetalwork.neoforge.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.config.ConfigBootstrap;
import dev.averageanime.createmetalwork.platform.Services;
import dev.averageanime.createmetalwork.registry.MetalItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ItemRegistration {

    public static final Map<String, DeferredItem<Item>> BY_ID = new LinkedHashMap<>();

    private ItemRegistration() {}

    public static void register(IEventBus modEventBus) {
        int skipped = 0;
        for (String entry : ConfigBootstrap.read(ConfigBootstrap.ITEMS, AddonDefaults.customItems())) {
            MetalItem item = MetalItem.parse(entry);
            if (item == null) continue;
            if (!item.condition().isSatisfied(Services.PLATFORM)) {
                skipped++;
                continue;
            }
            if (BY_ID.containsKey(item.id())) {
                CreateMetalworkCommon.LOGGER.warn("Skipping duplicate item entry: {}", item.id());
                continue;
            }
            BY_ID.put(item.id(), FluidRegistration.ITEMS.registerItem(item.id(),
                    properties -> new Item(properties.stacksTo(item.maxStack()))));
        }
        CreateMetalworkCommon.LOGGER.info("Registering {} items ({} deferred to another mod)",
                BY_ID.size(), skipped);
    }
}
