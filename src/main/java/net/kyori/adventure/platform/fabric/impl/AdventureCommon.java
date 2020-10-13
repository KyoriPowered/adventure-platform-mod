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

import ca.stellardrift.colonel.api.ServerArgumentType;
import io.netty.channel.Channel;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.ComponentArgumentType;
import net.kyori.adventure.platform.fabric.KeyArgumentType;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.accessor.ConnectionAccess;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudienceProviderImpl;
import net.kyori.adventure.platform.fabric.impl.server.FriendlyByteBufBridge;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AdventureCommon implements ModInitializer {

  public static final PlainComponentSerializer PLAIN;
  public static final GsonComponentSerializer GSON = GsonComponentSerializer.builder().legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE).build();

  static {
    final Function<KeybindComponent, String> keybindNamer;

    if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      keybindNamer = keybind -> KeyMapping.createNameSupplier(keybind.keybind()).get().getContents();
    } else {
      keybindNamer = KeybindComponent::keybind;
    }
    PLAIN = new PlainComponentSerializer(keybindNamer, trans -> new WrappedComponent(trans, GlobalTranslator.renderer()).getContents());
  }

  static ResourceLocation res(final @NonNull String value) {
    return new ResourceLocation("adventure", value);
  }

  @Override
  public void onInitialize() {
    // Register custom argument types
    if(FabricLoader.getInstance().isModLoaded("colonel")) { // we can do server-only arg types
      ServerArgumentType.<ComponentArgumentType>builder(res("component"))
        .type(ComponentArgumentType.class)
        .serializer(new ComponentArgumentTypeSerializer())
        .fallbackProvider(arg -> ComponentArgument.textComponent())
        .fallbackSuggestions(null) // client text parsing is fine
        .register();
      ServerArgumentType.<KeyArgumentType>builder(res("key"))
        .type(KeyArgumentType.class)
        .serializer(new EmptyArgumentSerializer<>(KeyArgumentType::key))
        .fallbackProvider(arg -> ResourceLocationArgument.id())
        .fallbackSuggestions(null)
        .register();
    } else {
      ArgumentTypes.register("adventure:component", ComponentArgumentType.class, new ComponentArgumentTypeSerializer());
      ArgumentTypes.register("adventure:key", KeyArgumentType.class, new EmptyArgumentSerializer<>(KeyArgumentType::key));
    }

    PlayerLocales.CHANGED_EVENT.register((player, locale) -> {
      final Channel channel = ((ConnectionAccess) player.connection.getConnection()).getChannel();
      channel.attr(FriendlyByteBufBridge.CHANNEL_LOCALE).set(locale);
      FabricServerAudienceProviderImpl.forEachInstance(instance -> {
        instance.bossBars().refreshTitles(player);
      });
    });
  }
}
