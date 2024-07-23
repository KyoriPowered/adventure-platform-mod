package net.kyori.adventure.platform.neoforge.impl;

import com.google.auto.service.AutoService;
import java.util.function.Function;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.platform.modcommon.impl.client.ClientWrappedComponent;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod("adventure_platform_neoforge")
public class AdventureMod {
  @AutoService(PlatformHooks.class)
  public static final class ForgeHooks implements PlatformHooks {

    @Override
    public void contributeFlattenerElements(final ComponentFlattener.@NotNull Builder flattenerBuilder) {
      // TODO AdventureFabricCommon.SIDE_PROXY.contributeFlattenerElements(flattenerBuilder);
    }

    @Override
    public @NotNull WrappedComponent createWrappedComponent(
      @NotNull final Component wrapped,
      @Nullable final Function<Pointered, ?> partition,
      @Nullable final ComponentRenderer<Pointered> renderer,
      final @Nullable NonWrappingComponentSerializer nonWrappingComponentSerializer
    ) {
      return new ClientWrappedComponent(wrapped, partition, renderer);
      // TODO return AdventureFabricCommon.SIDE_PROXY.createWrappedComponent(wrapped, partition, renderer, nonWrappingComponentSerializer);
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

    @Override
    public Pointers pointers(final Player player) {
      return Pointers.empty();
      // TODO return player instanceof PointerProviderBridge ? ((PointerProviderBridge) player).adventure$pointers() : Pointers.empty();
    }

    @Override
    public Pointered pointered(final Player player) {
      return player instanceof Pointered ? (Pointered) player : Audience.empty();
    }

    @Override
    public void replaceBossBarSubscriber(final ServerBossEvent bar, final ServerPlayer old, final ServerPlayer newPlayer) {
      // TODO ((ServerBossEventBridge) bar).adventure$replaceSubscriber(old, newPlayer);
    }
  }
}
