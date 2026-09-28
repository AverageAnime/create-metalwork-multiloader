package dev.averageanime.createmetalwork.fabric.platform;

import dev.averageanime.createmetalwork.lib.platform.AddonSource;
import dev.averageanime.createmetalwork.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FabricPlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /** {@code getAllMods()} is populated before entrypoints run, so this is safe from {@code onInitialize} onward. */
    @Override
    public List<AddonSource> findModResources(String path) {
        List<AddonSource> found = new ArrayList<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                mod.findPath(path)
                        .filter(Files::exists)
                        .ifPresent(p -> found.add(new AddonSource(mod.getMetadata().getId(), p)));
            } catch (Exception ignored) {
                // A mod container that cannot be probed contributes nothing.
            }
        }
        return found;
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public boolean isFabric() {
        return true;
    }

    @Override
    public boolean isNeoforge() {
        return false;
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
