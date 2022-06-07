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

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.fabric.ComponentArgumentType;
import net.kyori.adventure.platform.fabric.KeyArgumentType;
import net.kyori.adventure.platform.fabric.PlayerLocales;
import net.kyori.adventure.platform.fabric.impl.accessor.ArgumentTypeInfosAccess;
import net.kyori.adventure.platform.fabric.impl.server.FabricServerAudiencesImpl;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class AdventureCommon implements ModInitializer {

  private static final Logger LOGGER = LogUtils.getLogger();

  public static final SidedProxy SIDE_PROXY;

  public static final ComponentFlattener FLATTENER;
  private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");

  static {
    final var sidedProxy = chooseSidedProxy();
    SIDE_PROXY = sidedProxy;
    FLATTENER = createFlattener(sidedProxy);
  }

  private static SidedProxy chooseSidedProxy() {
    final EnvType environment = FabricLoader.getInstance().getEnvironmentType();
    final List<SidedProxy> proxies = ServiceLoader.load(SidedProxy.class).stream()
      .filter(provider -> {
        try {
          final SidedProxy proxy = provider.get();
          if (proxy.isApplicable(environment)) {
            return true;
          } else {
            LOGGER.debug("Skipping provider {} because it was not applicable to {}", provider.type(), environment);
          }
        } catch (final ServiceConfigurationError ex) {
          LOGGER.debug("Skipping provider {} due to an error while instantiating", provider.type(), ex);
        }
        return false;
      })
      .map(ServiceLoader.Provider::get)
      .toList();

    return switch (proxies.size()) {
      case 0 -> throw new IllegalStateException("No sided proxies were available for adventure-platform-fabric");
      case 1 -> {
        final var proxy = proxies.get(0);
        LOGGER.debug("Selected sided proxy {}", proxy);
        yield proxy;
      }
      default -> {
        LOGGER.warn("Multiple applicable proxies were applicable, choosing first: {}", proxies);
        yield proxies.get(0);
      }
    };
  }

  private static ComponentFlattener createFlattener(final SidedProxy proxy) {
    final ComponentFlattener.Builder flattenerBuilder = ComponentFlattener.basic().toBuilder();

    proxy.contributeFlattenerElements(flattenerBuilder);

    flattenerBuilder.complexMapper(TranslatableComponent.class, (translatable, consumer) -> {
      final String key = translatable.key();
      for (final Translator registry : GlobalTranslator.translator().sources()) {
        if (registry instanceof TranslationRegistry && ((TranslationRegistry) registry).contains(key)) {
          consumer.accept(GlobalTranslator.render(translatable, Locale.getDefault()));
          return;
        }
      }

      final @NotNull String translated = Language.getInstance().getOrDefault(key);
      final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
      final List<Component> args = translatable.args();
      int argPosition = 0;
      int lastIdx = 0;
      while (matcher.find()) {
        // append prior
        if (lastIdx < matcher.start()) consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
        lastIdx = matcher.end();

        final @Nullable String argIdx = matcher.group(1);
        // calculate argument position
        if (argIdx != null) {
          try {
            final int idx = Integer.parseInt(argIdx) - 1;
            if (idx < args.size()) {
              consumer.accept(args.get(idx));
            }
          } catch (final NumberFormatException ex) {
            // ignore, drop the format placeholder
          }
        } else {
          final int idx = argPosition++;
          if (idx < args.size()) {
            consumer.accept(args.get(idx));
          }
        }
      }

      // append tail
      if (lastIdx < translated.length()) {
        consumer.accept(Component.text(translated.substring(lastIdx)));
      }
    });

    return flattenerBuilder.build();
  }

  static ResourceLocation res(final @NotNull String value) {
    return new ResourceLocation(Adventure.NAMESPACE, value);
  }

  @Override
  public void onInitialize() {
    // Register custom argument types
    if (FabricLoader.getInstance().isModLoaded("colonel")) { // we can do server-only arg types
      /*ServerArgumentType.<ComponentArgumentType>builder(res("component"))
        .type(ComponentArgumentType.class)
        .serializer(new ComponentArgumentTypeSerializer())
        .fallbackProvider(arg -> {
          return switch (arg.format()) {
            case JSON -> ComponentArgument.textComponent();
            case MINIMESSAGE -> StringArgumentType.greedyString();
          };
        })
        .fallbackSuggestions(null) // client text parsing is fine
        .register();
      ServerArgumentType.<KeyArgumentType>builder(res("key"))
        .type(KeyArgumentType.class)
        .serializer(new EmptyArgumentSerializer<>(KeyArgumentType::key))
        .fallbackProvider(arg -> ResourceLocationArgument.id())
        .fallbackSuggestions(null)
        .register();*/
    } else {
      ArgumentTypeInfosAccess.adventure$invoke$register(
        Registry.COMMAND_ARGUMENT_TYPE,
        "adventure:component",
        ComponentArgumentType.class,
        new ComponentArgumentTypeSerializer()
      );
      ArgumentTypeInfosAccess.adventure$invoke$register(
        Registry.COMMAND_ARGUMENT_TYPE,
        "adventure:key",
        KeyArgumentType.class,
        SingletonArgumentInfo.contextFree(KeyArgumentType::key)
      );
    }

    PlayerLocales.CHANGED_EVENT.register((player, locale) -> {
      FabricServerAudiencesImpl.forEachInstance(instance -> {
        instance.bossBars().refreshTitles(player);
      });
    });

    // If we are in development mode, shut down immediately
    if (Boolean.getBoolean("adventure.testMode")) {
      if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
          MixinEnvironment.getCurrentEnvironment().audit();
          server.execute(() -> server.halt(false));
        });
      }
    }
  }

  public static Function<Pointered, Locale> localePartition() {
    return ptr -> ptr.getOrDefault(Identity.LOCALE, Locale.US);
  }

  public static Pointered pointered(final FPointered pointers) {
    return pointers;
  }

  @FunctionalInterface
  interface FPointered extends Pointered {
    @Override
    @NotNull Pointers pointers();
  }
}
