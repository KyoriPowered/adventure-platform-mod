package net.kyori.adventure.platform.neoforge.impl.server;

import net.kyori.adventure.platform.modcommon.impl.server.DedicatedServerProxy;
import net.kyori.adventure.platform.neoforge.impl.AdventureNeoforgeCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = "adventure_platform_neoforge", dist = Dist.DEDICATED_SERVER)
public class AdventureNeoforgeServer {
  static {
    AdventureNeoforgeCommon.SIDE_PROXY = new DedicatedServerProxy();
  }
}
