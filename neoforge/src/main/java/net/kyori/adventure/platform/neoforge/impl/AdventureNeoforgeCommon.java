package net.kyori.adventure.platform.neoforge.impl;

import com.google.auto.service.AutoService;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod("adventure_platform_neoforge")
public class AdventureNeoforgeCommon {
  public static SidedProxy SIDE_PROXY;

  @AutoService(PlatformHooks.class)
  public static final class ForgeHooks implements PlatformHooks {

    @Override
    public SidedProxy sidedProxy() {
      return AdventureNeoforgeCommon.SIDE_PROXY;
    }

    @Override
    public void updateTabList(
      final ServerPlayer player,
      final net.minecraft.network.chat.@Nullable Component header,
      final net.minecraft.network.chat.@Nullable Component footer
    ) {
      player.setTabListHeaderFooter(
        header == null ? player.getTabListHeader() : header,
        footer == null ? player.getTabListFooter() : footer
      );
    }
  }
}
