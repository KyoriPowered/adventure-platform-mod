package net.kyori.adventure.platform.fabric;

import java.util.function.UnaryOperator;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.ShowEntity;
import net.kyori.adventure.text.event.HoverEventSource;
import org.jetbrains.annotations.NotNull;

/**
 * An interface providing default implementations of HoverEventSource methods.
 *
 * @since 5.4.0
 */
public interface EntityHoverEventSource extends HoverEventSource<ShowEntity> {

  @Override
  default @NotNull HoverEvent<ShowEntity> asHoverEvent(final @NotNull UnaryOperator<ShowEntity> op) {
    throw new UnsupportedOperationException("Method must be overridden by Mixin");
  }

}
