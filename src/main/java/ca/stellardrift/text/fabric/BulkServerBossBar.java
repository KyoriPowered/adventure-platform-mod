package ca.stellardrift.text.fabric;

import java.util.Collection;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * An interface for performing bulk adds and removes on a {@link ServerBossBar}
 */
public interface BulkServerBossBar {
  void addAll(Collection<ServerPlayerEntity> players);
  void removeAll(Collection<ServerPlayerEntity> players);
}
