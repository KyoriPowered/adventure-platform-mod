package net.kyori.adventure.platform.neoforge.impl.client;

import net.kyori.adventure.platform.modcommon.impl.client.ClientProxy;
import net.kyori.adventure.platform.neoforge.impl.AdventureNeoforgeCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = "adventure_platform_neoforge", dist = Dist.CLIENT)
public class AdventureNeoforgeClient {
  static {
    AdventureNeoforgeCommon.SIDE_PROXY = new ClientProxy();
  }
}
