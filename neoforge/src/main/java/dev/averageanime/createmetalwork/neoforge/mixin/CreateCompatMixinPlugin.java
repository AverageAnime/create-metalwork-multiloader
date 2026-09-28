package dev.averageanime.createmetalwork.neoforge.mixin;

import dev.averageanime.createmetalwork.lib.mixin.ModPresenceMixinPlugin;

public class CreateCompatMixinPlugin extends ModPresenceMixinPlugin {

    @Override
    protected String requiredModId() {
        return "create";
    }
}
