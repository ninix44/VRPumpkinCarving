package org.vmstudio.pumpkincarving.core.client;

import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.common.addon.VisorAddon;
import org.vmstudio.pumpkincarving.core.client.overlays.VROverlayExample;
import org.vmstudio.pumpkincarving.core.common.VisorPumpkinCarving;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PumpkinCarvingAddonClient implements VisorAddon {
    @Override
    public void onAddonLoad() {
        VisorAPI.addonManager().getRegistries()
                .overlays()
                .registerComponents(
                        List.of(
                                new VROverlayExample(
                                        this,
                                        VROverlayExample.ID
                                )
                        )
                );
    }

    @Override
    public @Nullable String getAddonPackagePath() {
        return "org.vmstudio.pumpkincarving.core.client";
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
