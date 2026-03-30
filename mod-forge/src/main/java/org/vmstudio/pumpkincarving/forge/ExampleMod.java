package org.vmstudio.pumpkincarving.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import org.vmstudio.pumpkincarving.core.client.PumpkinCarvingAddonClient;
import org.vmstudio.pumpkincarving.core.common.AddonNetworking;
import org.vmstudio.pumpkincarving.core.common.VisorPumpkinCarving;
import org.vmstudio.pumpkincarving.core.network.NetworkHelper;
import org.vmstudio.pumpkincarving.core.server.PumpkinCarvingAddonServer;
import org.vmstudio.pumpkincarving.forge.network.ForgeNetworkChannel;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;

@Mod(VisorPumpkinCarving.MOD_ID)
public class ExampleMod {
    public ExampleMod() {
        NetworkHelper.setChannel(new ForgeNetworkChannel(new ResourceLocation(VisorPumpkinCarving.MOD_ID, "network")));
        AddonNetworking.initCommon();

        if(ModLoader.get().isDedicatedServer()){
            VisorAPI.registerAddon(
                    new PumpkinCarvingAddonServer()
            );
        }else{
            VisorAPI.registerAddon(
                    new PumpkinCarvingAddonClient()
            );
        }
    }
}
