package net.kyori.adventure.platform.tester.neoforge;

import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod("adventure_platform_neoforge_tester")
public class AdventureNeoTester {
  public AdventureNeoTester() {
    NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
      e.getEntity().sendSystemMessage(
        MinecraftServerAudiences.of(e.getEntity().getServer()).toNative(
          Component.text("hello", NamedTextColor.RED)
        )
      );
    });
  }
}
