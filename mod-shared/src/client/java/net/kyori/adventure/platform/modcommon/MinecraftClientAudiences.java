/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.modcommon;

import java.util.function.Function;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.impl.client.MinecraftClientAudiencesImpl;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import org.jetbrains.annotations.NotNull;

/**
 * Access the client's player as an {@link net.kyori.adventure.audience.Audience}.
 *
 * @since 6.0.0
 */
public interface MinecraftClientAudiences extends MinecraftAudiences {

  /**
   * Get the common instance, that will render using the global translation registry.
   *
   * @return the audience provider
   * @since 6.0.0
   */
  static @NotNull MinecraftClientAudiences of() {
    return MinecraftClientAudiencesImpl.INSTANCE;
  }

  /**
   * Create a builder for an audience provider that might use custom locales.
   *
   * @return the builder
   * @since 6.0.0
   */
  static MinecraftClientAudiences.@NotNull Builder builder() {
    return new MinecraftClientAudiencesImpl.Builder();
  }

  /**
   * Get an audience for the client's player.
   *
   * <p>When not in-game, most operations will no-op</p>
   *
   * @return player audience
   * @since 6.0.0
   */
  @NotNull Audience audience();

  /**
   * Build a {@link MinecraftClientAudiences} instance.
   *
   * @since 6.0.0
   */
  interface Builder {
    /**
     * Sets the component renderer for the provider.
     *
     * @param componentRenderer a component renderer
     * @return this builder
     * @see #componentRenderer(Function, ComponentRenderer)
     * @since 6.0.0
     */
    @NotNull Builder componentRenderer(final @NotNull ComponentRenderer<Pointered> componentRenderer);

    /**
     * Set the partition function for the provider.
     *
     * <p>The output of the function must have {@link Object#equals(Object)} and {@link Object#hashCode()}
     * methods overridden to ensure efficient operation.</p>
     *
     * <p>The output of the partition function must also be something suitable for use as a map key and
     * as such, for long-term storage. This excludes objects that may hold live game state
     * like {@code Entity} or {@code Level}.</p>
     *
     * <p>The configured {@link #componentRenderer(ComponentRenderer) component renderer} must produce
     * the same result for two {@link Pointered} instances where this partition function provides the
     * same output. If this condition is violated, caching issues are likely to occur, producing
     * incorrect output for at least one of the inputs.</p>
     *
     * <p>A local {@code record} is a good way to produce a compound output value for this function.</p>
     *
     * @param partitionFunction the partition function to apply
     * @return this builder
     * @see #componentRenderer(Function, ComponentRenderer)
     * @since 6.0.0
     */
    @NotNull Builder partition(final @NotNull Function<Pointered, ?> partitionFunction);

    /**
     * Sets the component renderer and partition function for the provider.
     *
     * <p>This variant validates that the component renderer only depends on information included in the partition.</p>
     *
     * @param partition the partition function to use on this provider
     * @param componentRenderer a component renderer
     * @param <T> the type used in the partition function
     * @return this builder
     * @since 6.0.0
     */
    default <T> @NotNull Builder componentRenderer(final @NotNull Function<Pointered, T> partition, final @NotNull ComponentRenderer<T> componentRenderer) {
      return this.partition(partition)
        .componentRenderer(componentRenderer.mapContext(partition));
    }

    /**
     * Builds the provider.
     *
     * @return the built provider
     * @since 6.0.0
     */
    @NotNull MinecraftClientAudiences build();
  }
}
