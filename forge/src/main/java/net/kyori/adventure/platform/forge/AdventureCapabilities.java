package net.kyori.adventure.platform.forge;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.pointer.Pointered;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class AdventureCapabilities {
  /**
   * A capabilitiy provided on all entities.
   *
   * @since 5.4.0
   */
  public static final Capability<Pointered> POINTERED = CapabilityManager.get(new CapabilityToken<Pointered>() {});

  /**
   * A capabilitiy provided on players.
   *
   * @since 5.4.0
   */
  public static final Capability<Audience> AUDIENCE = CapabilityManager.get(new CapabilityToken<Audience>() {});
}
