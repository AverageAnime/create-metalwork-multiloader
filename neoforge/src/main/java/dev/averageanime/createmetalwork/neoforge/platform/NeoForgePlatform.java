package dev.averageanime.createmetalwork.neoforge.platform;

import dev.averageanime.createmetalwork.lib.platform.AddonSource;
import dev.averageanime.createmetalwork.platform.Platform;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforge.data.loading.DatagenModLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NeoForgePlatform implements Platform {

    @Override
    public boolean isModLoaded(String modId) {
        LoadingModList loading = LoadingModList.get();
        if (loading != null) return loading.getModFileById(modId) != null;
        ModList list = ModList.get();
        return list != null && list.isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }

    @Override
    public boolean isRunningDataGen() {
        return DatagenModLoader.isRunningDataGen();
    }

    @Override
    public List<AddonSource> findModResources(String path) {
        String[] parts = path.split("/");
        List<AddonSource> found = new ArrayList<>();
        for (ModFileInfo info : LoadingModList.get().getModFiles()) {
            if (info == null || info.getMods().isEmpty()) continue;
            try {
                Path resource = info.getFile().findResource(parts);
                if (resource != null && Files.exists(resource)) {
                    found.add(new AddonSource(info.getMods().getFirst().getModId(), resource));
                }
            } catch (Exception ignored) {
                // A mod file that cannot be probed contributes nothing.
            }
        }
        return found;
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getDist().isClient();
    }

    @Override
    public boolean isServer() {
        return FMLLoader.getDist().isDedicatedServer();
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public boolean isNeoforge() {
        return true;
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
