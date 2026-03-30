package org.vmstudio.pumpkincarving.fabric;

import net.fabricmc.api.ModInitializer;
import org.vmstudio.pumpkincarving.core.common.AddonNetworking;
import org.vmstudio.pumpkincarving.core.network.NetworkHelper;
import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.pumpkincarving.core.client.PumpkinCarvingAddonClient;
import org.vmstudio.pumpkincarving.core.server.PumpkinCarvingAddonServer;
import org.vmstudio.pumpkincarving.fabric.network.FabricNetworkChannel;

public class PumpkinCarvingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetworkHelper.setChannel(new FabricNetworkChannel());
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
