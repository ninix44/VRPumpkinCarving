package org.vmstudio.pumpkincarving.fabric;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.pumpkincarving.core.client.PumpkinCarvingAddonClient;
import org.vmstudio.pumpkincarving.core.server.PumpkinCarvingAddonServer;
import net.fabricmc.api.ModInitializer;

public class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
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
