package dev.averageanime.createmetalwork.fabric.registry;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;
import dev.averageanime.createmetalwork.config.AddonDefaults;
import dev.averageanime.createmetalwork.config.ConfigBootstrap;
import dev.averageanime.createmetalwork.platform.Services;
import dev.averageanime.createmetalwork.registry.MetalItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

/** The same list NeoForge reads. Fabric registers eagerly. */
public final class ItemRegistration {

    public static final Map<String, Item> BY_ID = new LinkedHashMap<>();

    private ItemRegistration() {}

    public static void init() {
        int skipped = 0;
        for (String entry : ConfigBootstrap.read(ConfigBootstrap.ITEMS, AddonDefaults.customItems())) {
            MetalItem item = MetalItem.parse(entry);
            if (item == null) continue;
            if (!item.condition().isSatisfied(Services.PLATFORM)) {
                CreateMetalworkCommon.LOGGER.info("Not registering {}: {}", item.id(), item.condition().reason(Services.PLATFORM));
                skipped++;
                continue;
            }
            if (BY_ID.containsKey(item.id())) {
                CreateMetalworkCommon.LOGGER.warn("Skipping duplicate item entry: {}", item.id());
                continue;
            }
            BY_ID.put(item.id(), Registry.register(BuiltInRegistries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(CreateMetalworkCommon.MOD_ID, item.id()),
                    new Item(new Item.Properties().stacksTo(item.maxStack()))));
        }
        CreateMetalworkCommon.LOGGER.info("Registering {} items ({} deferred to another mod)",
                BY_ID.size(), skipped);
    }
}
