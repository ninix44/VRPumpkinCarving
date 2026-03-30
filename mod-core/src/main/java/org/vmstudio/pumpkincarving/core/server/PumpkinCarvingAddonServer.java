package org.vmstudio.pumpkincarving.core.server;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.pumpkincarving.core.common.AddonNetworking;
import org.vmstudio.pumpkincarving.core.common.VisorPumpkinCarving;
import org.vmstudio.visor.api.common.addon.VisorAddon;

public class PumpkinCarvingAddonServer implements VisorAddon {
    @Override
    public void onAddonLoad() {
        AddonNetworking.initCommon();
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.pumpkincarving.core.server";
    }

    @Override
    public @NotNull String getAddonId() {
        return VisorPumpkinCarving.MOD_ID;
    }

    @Override
    public @NotNull Component getAddonName() {
        return Component.literal(VisorPumpkinCarving.MOD_NAME);
    }

    @Override
    public String getModId() {
        return VisorPumpkinCarving.MOD_ID;
    }
}
