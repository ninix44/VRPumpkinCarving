package org.vmstudio.pumpkincarving.forge;

import org.vmstudio.visor.api.ModLoader;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.pumpkincarving.core.client.PumpkinCarvingAddonClient;
import org.vmstudio.pumpkincarving.core.common.VisorPumpkinCarving;
import org.vmstudio.pumpkincarving.core.server.PumpkinCarvingAddonServer;
import net.minecraftforge.fml.common.Mod;

@Mod(VisorPumpkinCarving.MOD_ID)
public class ExampleMod {
    public ExampleMod(){
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
