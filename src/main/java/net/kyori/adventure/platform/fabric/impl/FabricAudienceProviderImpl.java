/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2020 KyoriPowered
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

package net.kyori.adventure.platform.fabric.impl;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack;
import net.kyori.adventure.platform.fabric.CommandSourceAudience;
import net.kyori.adventure.platform.fabric.FabricAudienceProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * The entry point for accessing Adventure.
 */
public final class FabricAudienceProviderImpl implements FabricAudienceProvider {
  public static final PlainComponentSerializer PLAIN;
  public static final ComponentSerializer<Component, Component, net.minecraft.network.chat.Component> TEXT_NON_WRAPPING =
          new NonWrappingComponentSerializer();
  public static final GsonComponentSerializer GSON = GsonComponentSerializer.builder().legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE).build();

  static {
    final Function<KeybindComponent, String> keybindNamer;

    if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      keybindNamer = keybind -> KeyMapping.createNameSupplier(keybind.keybind()).get().getContents();
    } else {
      keybindNamer = KeybindComponent::keybind;
    }
    PLAIN = new PlainComponentSerializer(keybindNamer, trans -> FabricAudienceProvider.adapt(trans).getContents());
  }

  private final MinecraftServer server;
  private final ComponentRenderer<Locale> renderer;

  public FabricAudienceProviderImpl(final MinecraftServer server, final ComponentRenderer<Locale> renderer) {
    this.server = server;
    this.renderer = renderer;
  }

  @Override
  public @NonNull Audience all() {
    return Audience.of(this.console(), this.players());
  }

  @Override
  public @NonNull Audience console() {
    return (Audience) this.server;
  }

  @Override
  public @NonNull Audience players() {
    return Audience.of(this.audiences(this.server.getPlayerList().getPlayers()));
  }

  @Override
  public @NonNull Audience player(final @NonNull UUID playerId) {
    final /* @Nullable */ ServerPlayer player = this.server.getPlayerList().getPlayer(playerId);
    return player != null ? (Audience) player : Audience.empty();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Iterable<Audience> audiences(final Iterable<? extends ServerPlayer> players) {
    return (Iterable) players;
  }

  @Override
  public @NonNull Audience permission(final @NonNull String permission) {
    return Audience.of(); // TODO: permissions api
  }

  @Override public AdventureCommandSourceStack audience(final @NonNull CommandSourceStack source) {
    if(!(source instanceof AdventureCommandSourceStack)) {
      throw new IllegalArgumentException("The AdventureCommandSource mixin failed!");
    }

    return (AdventureCommandSourceStack) source;
  }

  @Override public Audience audience(final @NonNull CommandSource output) {
    return CommandSourceAudience.of(output);
  }

  @Override public Audience audience(final @NonNull Iterable<ServerPlayer> players) {
    return Audience.of(this.audiences(players));
  }

  @Override
  public @NonNull Audience world(final @NonNull Key worldId) {
    final /* @Nullable */ ServerLevel level = this.server.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY,
            FabricAudienceProvider.adapt(requireNonNull(worldId, "worldId"))));
    if(level != null) {
      return this.audience(level.players());
    }
    return Audience.of();
  }

  @Override
  public @NonNull Audience server(final @NonNull String serverName) {
    return this.all();
  }

  @Override
  public @NonNull ComponentRenderer<Locale> localeRenderer() {
    return this.renderer;
  }

  @Override
  public @NonNull GsonComponentSerializer gsonSerializer() {
    return GSON;
  }

  @Override
  public void close() {
  }
}
