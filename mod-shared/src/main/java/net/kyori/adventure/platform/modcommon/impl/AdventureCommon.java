package net.kyori.adventure.platform.modcommon.impl;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class AdventureCommon {
  public static final Supplier<ComponentFlattener> FLATTENER;
  public static final PlatformHooks HOOKS;
  public static final ScheduledExecutorService SCHEDULER;
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");

  static {
    // Daemon thread executor for scheduled tasks
    SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
      .setNameFormat("adventure-platform-fabric-scheduler-%d")
      .setDaemon(true)
      .setUncaughtExceptionHandler((thread, ex) -> LOGGER.error("An uncaught exception occurred in scheduler thread '{}':", thread.getName(), ex))
      .build());
    final var platformHooks = discoverHooks();
    HOOKS = platformHooks;
    FLATTENER = Suppliers.memoize(() -> createFlattener(platformHooks));
  }

  public static Pointers pointers(final Player player) {
    return player instanceof PointerProviderBridge ? ((PointerProviderBridge) player).adventure$pointers() : Pointers.empty();
  }

  public static Pointered pointered(final Player player) {
    return player instanceof Pointered ? (Pointered) player : Audience.empty();
  }

  public static ResourceLocation res(final @NotNull String value) {
    return ResourceLocation.fromNamespaceAndPath(Adventure.NAMESPACE, value);
  }

  static PlatformHooks discoverHooks() {
    return ServiceLoader.load(PlatformHooks.class)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No platform-specific hooks found for adventure platform"));
  }

  private static ComponentFlattener createFlattener(final SidedProxy proxy) {
    final ComponentFlattener.Builder flattenerBuilder = ComponentFlattener.basic().toBuilder();

    proxy.contributeFlattenerElements(flattenerBuilder);

    flattenerBuilder.complexMapper(TranslatableComponent.class, (translatable, consumer) -> {
      final String key = translatable.key();
      for (final Translator registry : GlobalTranslator.translator().sources()) {
        if (registry instanceof TranslationRegistry tr && tr.contains(key)) {
          consumer.accept(GlobalTranslator.render(translatable, Locale.getDefault()));
          return;
        }
      }

      final @Nullable String translated = Objects.requireNonNullElse(
        Language.getInstance().getOrDefault(key, translatable.fallback()),
        key
      );

      final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
      final List<TranslationArgument> args = translatable.arguments();
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
              consumer.accept(args.get(idx).asComponent());
            }
          } catch (final NumberFormatException ex) {
            // ignore, drop the format placeholder
          }
        } else {
          final int idx = argPosition++;
          if (idx < args.size()) {
            consumer.accept(args.get(idx).asComponent());
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

  public static Function<Pointered, Locale> localePartition() {
    return ptr -> ptr.getOrDefault(Identity.LOCALE, Locale.US);
  }

  public static Pointered pointered(final FPointered pointers) {
    return pointers;
  }

  public static void scheduleClickCallbackCleanup() {
    // Perform scheduled cleanup
    SCHEDULER.scheduleWithFixedDelay(
      ClickCallbackRegistry.INSTANCE::cleanUp,
      ClickCallbackRegistry.CLEAN_UP_RATE,
      ClickCallbackRegistry.CLEAN_UP_RATE,
      TimeUnit.SECONDS
    );
  }

  public static net.minecraft.network.chat.ChatType.@NotNull Bound chatTypeToNative(
    final ChatType.@NotNull Bound bound,
    final MinecraftAudiencesInternal rendererProvider
  ) {
    final Holder<net.minecraft.network.chat.ChatType> type = rendererProvider.registryAccess()
      .registryOrThrow(Registries.CHAT_TYPE)
      .getHolder(MinecraftAudiences.toNative(bound.type().key()))
      .orElseThrow(() -> new IllegalArgumentException("Could not resolve chat type for key " + bound.type().key()));

    return new net.minecraft.network.chat.ChatType.Bound(
      type,
      rendererProvider.toNative(bound.name()),
      Optional.ofNullable(bound.target()).map(rendererProvider::toNative)
    );
  }

  @FunctionalInterface
  interface FPointered extends Pointered {
    @Override
    @NotNull
    Pointers pointers();
  }
}
