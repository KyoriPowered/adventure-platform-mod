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

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.CommandSyntaxExceptionWrapper;
import net.kyori.adventure.platform.modcommon.impl.MinecraftAudiencesInternal;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.PlayerChatMessageBridge;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.platform.modcommon.impl.nbt.ModDataComponentValue;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.util.ComponentMessageThrowable;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Common operations in both the client and server environments.
 *
 * <p>See {@link MinecraftServerAudiences} for logical server-specific operations,
 * and {@code MinecraftClientAudiences} for logical client-specific operations</p>
 *
 * <p>In development environments with interface injection, many of the utility methods
 * in this class are redundant.</p>
 *
 * @since 6.0.0
 */
public interface MinecraftAudiences {

  /**
   * Given an existing native component, convert it into an Adventure component for working with.
   *
   * @param input source component
   * @param modifier operator to transform the component
   * @return new component
   * @since 6.0.0
   */
  default net.minecraft.network.chat.@NotNull Component update(final net.minecraft.network.chat.@NotNull Component input, final UnaryOperator<Component> modifier) {
    final Component modified;
    if (input instanceof WrappedComponent) {
      modified = requireNonNull(modifier).apply(((WrappedComponent) input).wrapped());
      final @Nullable Function<Pointered, ?> partition = ((WrappedComponent) input).partition();
      final @Nullable ComponentRenderer<Pointered> renderer = ((WrappedComponent) input).renderer();
      return AdventureCommon.HOOKS.createWrappedComponent(
        modified,
        partition,
        renderer,
        new NonWrappingComponentSerializer(((MinecraftAudiencesInternal) this)::registryAccess)
      );
    } else {
      final Component original = this.asAdventure(input);
      modified = modifier.apply(original);
      return this.asNative(modified);
    }
  }

  /**
   * Convert a MC {@link ResourceLocation} instance to a text Key.
   *
   * <p>{@link ResourceLocation} implements {@link Key} at runtime, so this is effectively a cast.</p>
   *
   * @param loc The Identifier to convert
   * @return The equivalent data as a Key
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  @SuppressWarnings("DataFlowIssue")
  static Key asAdventure(final ResourceLocation loc) {
    if (loc == null) {
      return null;
    }
    return (Key) (Object) loc;
  }

  /**
   * Convert a Kyori {@link Key} instance to a MC ResourceLocation.
   *
   * @param key The Key to convert
   * @return The equivalent data as a resource location
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  @SuppressWarnings("ConstantValue")
  static ResourceLocation asNative(final Key key) {
    if (key == null) {
      return null;
    }

    if ((Object) key instanceof ResourceLocation loc) {
      return loc;
    }
    return ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
  }

  /**
   * Get an {@link Entity}'s representation as an {@link net.kyori.adventure.sound.Sound.Emitter} of sounds.
   *
   * @param entity the entity to convert
   * @return the entity as a sound emitter
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  static Sound.Emitter asEmitter(final Entity entity) {
    return (Sound.Emitter) entity;
  }

  /**
   * Expose a Brigadier CommandSyntaxException's message using the adventure-provided interface for rich-message exceptions.
   *
   * @param ex the exception to cast
   * @return a converted command exception
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  default ComponentMessageThrowable asComponentThrowable(final CommandSyntaxException ex) {
    if (ex == null) {
      return null;
    }
    return new CommandSyntaxExceptionWrapper(ex, this);
  }

  /**
   * Return a TextSerializer instance that will do deep conversions between
   * Adventure {@link Component Components} and Minecraft {@link net.minecraft.network.chat.Component Components}.
   *
   * <p>This serializer will never wrap text, and can provide {@link net.minecraft.network.chat.MutableComponent}
   * instances suitable for passing around the game.</p>
   *
   * @return a serializer instance
   * @since 6.0.0
   */
  @NotNull ComponentSerializer<Component, Component, net.minecraft.network.chat.Component> nonWrappingSerializer();

  /**
   * Get a {@link Player} identified by their profile's {@link java.util.UUID}.
   *
   * @param player the player to identify
   * @return an identified representation of the player
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  static Identified identified(final Player player) {
    return (Identified) player;
  }

  /**
   * Get an {@link Identity} representation of a {@link GameProfile}.
   *
   * @param profile the profile to represent
   * @return an identity of the game profile
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  static Identity identity(final GameProfile profile) {
    if (profile == null) {
      return null;
    }
    if (profile instanceof Identity identity) {
      return identity;
    }
    return Identity.identity(profile.getId());
  }

  /**
   * Returns a {@link HoverEvent} that displays the provided {@link Entity}.
   *
   * @param entity the entity
   * @return hover event
   * @since 6.0.0
   */
  @SuppressWarnings("unchecked")
  static @NotNull HoverEvent<HoverEvent.ShowEntity> asHoverEvent(final @NotNull Entity entity) {
    return ((HoverEventSource<HoverEvent.ShowEntity>) entity).asHoverEvent();
  }

  /**
   * Returns a {@link HoverEvent} that displays the provided {@link ItemStack}.
   *
   * @param stack the item stack
   * @return hover event
   * @since 6.0.0
   */
  @SuppressWarnings("unchecked")
  static @NotNull HoverEvent<HoverEvent.ShowItem> asHoverEvent(final @NotNull ItemStack stack) {
    return ((HoverEventSource<HoverEvent.ShowItem>) (Object) stack).asHoverEvent();
  }

  /**
   * Returns an adventure {@link Sound.Type} for the provided {@link SoundEvent}.
   *
   * @param soundEvent sound event
   * @return sound type
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  static Sound.Type asSoundType(final SoundEvent soundEvent) {
    return (Sound.Type) (Object) soundEvent;
  }

  /**
   * Returns the Kyori {@link Key} representation of the {@link ResourceKey}'s {@link ResourceKey#location() location}.
   *
   * @param resourceKey resource key
   * @return key
   * @since 6.0.0
   */
  static @NotNull Key key(final @NotNull ResourceKey<?> resourceKey) {
    return ((Keyed) resourceKey).key();
  }

  /**
   * Returns an adventure view of the provided {@link MessageSignature}.
   *
   * @param signature message signature
   * @return adventure message signature
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  @SuppressWarnings("DataFlowIssue")
  static SignedMessage.Signature asAdventure(final MessageSignature signature) {
    return (SignedMessage.Signature) (Object) signature;
  }

  /**
   * Returns a native view of the provided {@link SignedMessage.Signature}.
   *
   * @param signature message signature
   * @return native message signature
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  @SuppressWarnings("ConstantValue")
  static MessageSignature asNative(final SignedMessage.Signature signature) {
    if (signature == null) {
      return null;
    }
    if ((Object) signature instanceof MessageSignature nativeSig) {
      return nativeSig;
    }
    return new MessageSignature(signature.bytes());
  }

  /**
   * Returns an adventure {@link SignedMessage} view of the provided {@link PlayerChatMessage}.
   *
   * @param message player chat message
   * @return signed message
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  @SuppressWarnings("DataFlowIssue")
  default SignedMessage asAdventure(final PlayerChatMessage message) {
    if (message == null) {
      return null;
    }
    ((PlayerChatMessageBridge) (Object) message).adventure$controller((MinecraftAudiencesInternal) this);
    return (SignedMessage) (Object) message;
  }

  /**
   * Get a wrapped value for a certain data component.
   *
   * <p>The data component value must not be a {@link DataComponentType#isTransient() transient} one.</p>
   *
   * @param type the component type
   * @param value the value to wrap
   * @param <T> the value type
   * @return the wrapped value
   * @since 6.0.0
   */
  static @NotNull <T> DataComponentValue dataComponentValue(final @NotNull DataComponentType<T> type, final @NotNull T value) {
    return new ModDataComponentValue.Present<>(value, type.codecOrThrow());
  }

  /**
   * Return a component flattener that can use game data to resolve extra information about components.
   *
   * @return the flattener
   * @since 6.0.0
   */
  @NotNull ComponentFlattener flattener();

  /**
   * Active renderer to render components.
   *
   * @return Shared renderer
   * @since 6.0.0
   */
  @NotNull ComponentRenderer<Pointered> renderer();

  /**
   * Get a native {@link net.minecraft.network.chat.Component} from an adventure {@link Component}.
   *
   * <p>The specific type of the returned component is undefined. For example, it may be a wrapper object.</p>
   *
   * @param adventure adventure input
   * @return native representation
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  net.minecraft.network.chat.Component asNative(Component adventure);

  /**
   * Get an adventure {@link Component} from a native {@link net.minecraft.network.chat.Component}.
   *
   * @param vanilla the native component
   * @return adventure component
   * @since 6.0.0
   */
  @Contract("null -> null; !null -> !null")
  default Component asAdventure(final net.minecraft.network.chat.Component vanilla) {
    return this.nonWrappingSerializer().deserializeOrNull(vanilla);
  }
}
