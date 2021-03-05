/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2021 KyoriPowered
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.ComponentArgumentType;
import net.kyori.adventure.platform.fabric.KeyArgumentType;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.accessor.ConnectionAccess;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudiencesImpl;
import net.kyori.adventure.platform.fabric.impl.server.FriendlyByteBufBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.ComponentFlattener;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.EmptyArgumentSerializer;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AdventureCommon implements ModInitializer {

  public static final ComponentFlattener FLATTENER;
  public static final PlainComponentSerializer PLAIN;
  public static final GsonComponentSerializer GSON = GsonComponentSerializer.builder().legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.INSTANCE).build();
  private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");

  static {
    final ComponentFlattener.Builder flattenerBuilder = ComponentFlattener.basic().toBuilder();

    if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
      flattenerBuilder.type(KeybindComponent.class, keybind -> KeyMapping.createNameSupplier(keybind.keybind()).get().getContents());
    }

    flattenerBuilder.nestedType(TranslatableComponent.class, (translatable, consumer) -> {
      final @Nullable String translated = Language.getInstance().has(translatable.key()) ? Language.getInstance().getOrDefault(translatable.key()) : null;
      if(translated == null) {
        consumer.accept(Component.text(translatable.key()));
        return;
      }

      final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
      final List<Component> args = translatable.args();
      int argPosition = 0;
      int lastIdx = 0;
      while(matcher.find()) {
        // append prior
        if(lastIdx < matcher.start()) consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
        lastIdx = matcher.end();

        final @Nullable String argIdx = matcher.group(1);
        // calculate argument position
        if(argIdx != null) {
          try {
            final int idx = Integer.parseInt(argIdx);
            if(idx < args.size()) {
              consumer.accept(args.get(idx));
            }
          } catch(final NumberFormatException ex) {
            // ignore, drop the format placeholder
          }
        } else {
          final int idx = argPosition++;
          if(idx < args.size()) {
            consumer.accept(args.get(idx));
          }
        }
      }

      // append tail
      if(lastIdx < translated.length()) {
        consumer.accept(Component.text(translated.substring(lastIdx)));
      }
    });

    FLATTENER = flattenerBuilder.build();
    PLAIN = PlainComponentSerializer.builder().flattener(FLATTENER).build();
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
      FabricServerAudiencesImpl.forEachInstance(instance -> {
        instance.bossBars().refreshTitles(player);
      });
    });
  }
}
