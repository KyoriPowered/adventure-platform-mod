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
package net.kyori.adventure.platform.fabric.impl;

import com.google.auto.service.AutoService;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.CollectPointersCallback;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerBridge;
import net.kyori.adventure.platform.modcommon.ComponentArgumentType;
import net.kyori.adventure.platform.modcommon.KeyArgumentType;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ClickCallbackRegistry;
import net.kyori.adventure.platform.modcommon.impl.ComponentArgumentTypeSerializer;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class AdventureFabricCommon implements ModInitializer {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final SidedProxy SIDE_PROXY;

  public static final String MOD_FAPI_NETWORKING = "fabric-networking-api-v1";

  static {
    final var sidedProxy = chooseSidedProxy();
    SIDE_PROXY = sidedProxy;
  }

  private static SidedProxy chooseSidedProxy() {
    final EnvType environment = FabricLoader.getInstance().getEnvironmentType();
    final var sidedProxyContainers = FabricLoader.getInstance().getEntrypointContainers(
      "adventure-internal:sidedproxy/" + environment.name().toLowerCase(Locale.ROOT),
      SidedProxy.class
    );

    return switch (sidedProxyContainers.size()) {
      case 0 -> throw new IllegalStateException("No sided proxies were available for adventure-platform-fabric in environment " + environment);
      case 1 -> {
        final var proxy = sidedProxyContainers.getFirst();
        LOGGER.debug("Selected sided proxy {} from {}", proxy.getEntrypoint(), proxy.getProvider().getMetadata().getId());
        yield proxy.getEntrypoint();
      }
      default -> {
        final var proxy = sidedProxyContainers.getFirst();
        LOGGER.warn("Multiple applicable proxies were applicable, choosing first available: {} from {}", proxy.getEntrypoint(), proxy.getProvider().getMetadata().getId());
        yield proxy.getEntrypoint();
      }
    };
  }

  @Override
  public void onInitialize() {
    this.setupCustomArgumentTypes();

    CommandRegistrationCallback.EVENT.register((dispatcher, registries, env) -> {
      ClickCallbackRegistry.INSTANCE.registerToDispatcher(dispatcher);
    });

    AdventureCommon.scheduleClickCallbackCleanup();

    // If we are in development mode, shut down immediately
    if (Boolean.getBoolean("adventure.testMode")) {
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
          MixinEnvironment.getCurrentEnvironment().audit();
          server.execute(() -> {
            try {
              Files.writeString(Path.of("adventure-test-success.out"), "true");
            } catch (final IOException ex) {
              System.exit(1);
            }
            server.halt(false);
          });
        });
      }
    }
  }

  private void setupCustomArgumentTypes() {
    // sync is optional, so fapi is not required
    if (FabricLoader.getInstance().isModLoaded(MOD_FAPI_NETWORKING)) {
      ClientboundArgumentTypeMappingsPacket.register();
      ServerboundRegisteredArgumentTypesPacket.register();
    }

    ServerArgumentTypes.register(
      new ServerArgumentType<>(
        AdventureCommon.res("component"),
        ComponentArgumentType.class,
        ComponentArgumentTypeSerializer.INSTANCE,
        (arg, ctx) -> switch (arg.format()) {
          case JSON -> ComponentArgument.textComponent(ctx);
          case MINIMESSAGE -> StringArgumentType.greedyString();
        },
        null
      )
    );
    ServerArgumentTypes.register(
      new ServerArgumentType<>(
        AdventureCommon.res("key"),
        KeyArgumentType.class,
        SingletonArgumentInfo.contextFree(KeyArgumentType::key),
        (arg, ctx) -> ResourceLocationArgument.id(),
        null
      )
    );
  }

  @AutoService(PlatformHooks.class)
  public static final class FabricHooks implements PlatformHooks {

    @Override
    public SidedProxy sidedProxy() {
      return AdventureFabricCommon.SIDE_PROXY;
    }

    @Override
    public void updateTabList(
      final ServerPlayer player,
      final net.minecraft.network.chat.@Nullable Component header,
      final net.minecraft.network.chat.@Nullable Component footer
    ) {
      ((ServerPlayerBridge) player).bridge$updateTabList(header, footer);
    }

    @Override
    public void collectPointers(final Pointered pointered, final Pointers.Builder builder) {
      PlatformHooks.super.collectPointers(pointered, builder);
      CollectPointersCallback.EVENT.invoker().registerPointers(pointered, builder);
    }

    @Override
    public void onLocaleChange(final ServerPlayer player, final Locale newLocale) {
      PlatformHooks.super.onLocaleChange(player, newLocale);
      PlayerLocales.CHANGED_EVENT.invoker().onLocaleChanged(player, newLocale);
    }
  }
}
