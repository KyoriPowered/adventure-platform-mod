package net.kyori.adventure.platform.fabric.impl.server;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.modcommon.impl.server.MinecraftServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.server.ServerPlayerAudience;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class FabricServerPlayerAudience extends ServerPlayerAudience {
  private final MinecraftServerAudiencesImpl controller;

  public FabricServerPlayerAudience(final ServerPlayer player, final MinecraftServerAudiencesImpl controller) {
    super(player, controller);
    this.controller = controller;
  }

  @Override
  public void showBossBar(final @NotNull BossBar bar) {
    MinecraftServerAudiencesImpl.forEachInstance(controller -> {
      if (controller != this.controller) {
        controller.bossBars().unsubscribe(this.player, bar);
      }
    });
    this.controller.bossBars().subscribe(this.player, bar);
  }

  @Override
  public void hideBossBar(final @NotNull BossBar bar) {
    MinecraftServerAudiencesImpl.forEachInstance(controller -> controller.bossBars().unsubscribe(this.player, bar));
  }
}
