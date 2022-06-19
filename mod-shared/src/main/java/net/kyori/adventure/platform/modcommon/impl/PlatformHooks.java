package net.kyori.adventure.platform.modcommon.impl;

import com.google.gson.Gson;
import net.kyori.adventure.pointer.Pointers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Service interface for platform-specific things.
 */
public interface PlatformHooks extends SidedProxy {
  Gson componentSerializerGson();

  void updateTabList(final ServerPlayer player, final @Nullable Component header, final @Nullable Component footer);

  Pointers pointers(final Player player);

  void replaceBossBarSubscriber(ServerBossEvent bar, ServerPlayer old, ServerPlayer newPlayer);
}
