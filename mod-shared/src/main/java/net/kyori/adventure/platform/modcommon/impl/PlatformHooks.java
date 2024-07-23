package net.kyori.adventure.platform.modcommon.impl;

import java.util.Locale;
import java.util.function.Function;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service interface for platform-specific things.
 */
public interface PlatformHooks extends SidedProxy {
  SidedProxy sidedProxy();

  @Override
  default void contributeFlattenerElements(final ComponentFlattener.@NotNull Builder flattenerBuilder) {
    this.sidedProxy().contributeFlattenerElements(flattenerBuilder);
  }

  @Override
  default @NotNull WrappedComponent createWrappedComponent(
    final net.kyori.adventure.text.@NotNull Component wrapped,
    final @Nullable Function<Pointered, ?> partition,
    final @Nullable ComponentRenderer<Pointered> renderer,
    final @Nullable NonWrappingComponentSerializer nonWrappingSerializer
  ) {
    return this.sidedProxy().createWrappedComponent(wrapped, partition, renderer, nonWrappingSerializer);
  }

  void updateTabList(final ServerPlayer player, final @Nullable Component header, final @Nullable Component footer);

  default void collectPointers(Pointered pointered, Pointers.Builder builder) {
  }

  default void onLocaleChange(ServerPlayer player, Locale newLocale) {
  }
}
