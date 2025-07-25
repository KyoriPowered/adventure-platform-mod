package net.kyori.adventure.platform.modcommon;

import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowItem;
import net.kyori.adventure.text.event.HoverEventSource;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

/**
 * Marker interface for item hover event sources.
 * Used by interface injection on {@link net.minecraft.world.item.ItemStack}.
 */
public interface ItemHoverEventSource extends HoverEventSource<ShowItem> {

  @Override
  default HoverEvent<ShowItem> asHoverEvent(@NotNull UnaryOperator<ShowItem> op) {
    throw new UnsupportedOperationException("Method must be overridden by Mixin");
  }
  
}
