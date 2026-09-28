package dev.averageanime.createmetalwork.platform;

import dev.averageanime.createmetalwork.CreateMetalworkCommon;

public final class Services {

    public static final Platform PLATFORM = load(Platform.class);

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        final T service = dev.averageanime.createmetalwork.lib.platform.Services.load(clazz);
        CreateMetalworkCommon.LOGGER.debug("Loaded {} for service {}", service, clazz);
        return service;
    }
}
