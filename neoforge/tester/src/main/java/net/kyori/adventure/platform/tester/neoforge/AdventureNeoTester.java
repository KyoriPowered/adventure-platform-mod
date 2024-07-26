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
package net.kyori.adventure.platform.tester.neoforge;

import com.google.common.base.Strings;
import com.mojang.brigadier.Command;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
import net.kyori.adventure.platform.modcommon.ComponentArgumentType;
import net.kyori.adventure.platform.modcommon.KeyArgumentType;
import net.kyori.adventure.platform.modcommon.MinecraftAudiences;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.impl.ComponentArgumentTypeSerializer;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.modcommon.ComponentArgumentType.component;
import static net.kyori.adventure.platform.modcommon.ComponentArgumentType.miniMessage;
import static net.kyori.adventure.platform.modcommon.KeyArgumentType.key;
import static net.kyori.adventure.sound.Sound.sound;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.TextColor.color;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

@Mod("adventure_platform_neoforge_tester")
public class AdventureNeoTester {
  public static final ComponentLogger LOGGER = ComponentLogger.logger();
  private static final Key FONT_MEOW = advKey("meow");
  private static final Key FONT_IOSEVKA = advKey("iosevka");

  private static final List<TextColor> LINE_COLOURS = IntStream.of(0x9400D3, 0x4B0082, 0x0000FF, 0x00FF00, 0xFFFF00, 0xFF7F00, 0xFF0000,
      0x55CDFC, 0xF7A8B8, 0xFFFFFF, 0xF7A8B8, 0x55CDFC)
    .mapToObj(TextColor::color).toList();
  private static final Component LINE = text(Strings.repeat("█", 10));

  private static final String ARG_TEXT = "text";
  private static final String ARG_SECONDS = "seconds";
  private static final String ARG_TARGET = "target";
  private static final String ARG_TARGETS = "targets";
  private static final String ARG_SOUND = "sound";
  private static final TextColor COLOR_RESPONSE = color(0x22EE99);

  private final Map<UUID, BossBar> greetingBars = new HashMap<>();
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private @Nullable MinecraftServerAudiences platform;

  private static final ChatType ADVENTURE_BROADCAST = net.kyori.adventure.chat.ChatType.chatType(advKey("broadcast"));

  private static Key advKey(final String location) {
    return MinecraftAudiences.asAdventure(ResourceLocation.fromNamespaceAndPath(Adventure.NAMESPACE, location));
  }

  public @NotNull MinecraftServerAudiences adventure() {
    return requireNonNull(this.platform, "Tried to access Fabric platform without a running server");
  }

  // TODO - add registration helper for neo
  private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, "adventure");
  private static final Holder<ArgumentTypeInfo<?, ?>> COMPONENT_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register("component", () -> ArgumentTypeInfos.registerByClass(
    ComponentArgumentType.class,
    ComponentArgumentTypeSerializer.INSTANCE
  ));
  private static final Holder<ArgumentTypeInfo<?, ?>> KEY_ARGUMENT_TYPE = COMMAND_ARGUMENT_TYPES.register("key", () -> ArgumentTypeInfos.registerByClass(
    KeyArgumentType.class,
    SingletonArgumentInfo.contextFree(KeyArgumentType::key)
  ));

  public AdventureNeoTester(final IEventBus modBus) {
    COMMAND_ARGUMENT_TYPES.register(modBus);

    // Register localizations
    final TranslationRegistry testmodRegistry = TranslationRegistry.create(advKey("testmod_localizations"));
    for (final var lang : List.of(Locale.ENGLISH, Locale.GERMAN)) {
      testmodRegistry.registerAll(lang, ResourceBundle.getBundle("net.kyori.adventure.platform.test.fabric.messages", lang), false);
    }
    GlobalTranslator.translator().addSource(testmodRegistry);

    LOGGER.info(Component.text("Setting up mod! {} is a cool mode"), Component.translatable("gameMode.adventure", NamedTextColor.BLUE));

    // Set up platform
    NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> this.platform = MinecraftServerAudiences.of(e.getServer()));
    NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
      this.platform = null;
      this.greetingBars.clear();
    });

    NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> {
      e.getDispatcher().register(literal("adventure")
        .then(literal("about").executes(ctx -> {
          // Interface injection, this lets us access the default platform instance
          this.platform.audience(ctx.getSource()).sendMessage(translatable("adventure.test.welcome", COLOR_RESPONSE, this.platform.asAdventure(ctx.getSource().getDisplayName())));
          // Or the old-fashioned way, for
          this.adventure().audience(ctx.getSource()).sendMessage(translatable("adventure.test.description", color(0xc022cc)));
          return 1;
        }))
        .then(literal("echo").then(argument(ARG_TEXT, miniMessage()).executes(ctx -> {
          final Component result = component(ctx, ARG_TEXT);
          this.platform.audience(ctx.getSource()).sendMessage(result, ChatType.SAY_COMMAND.bind(this.platform.asAdventure(ctx.getSource().getDisplayName())));
          this.platform.audience(ctx.getSource()).sendMessage(text("And a second time!", NamedTextColor.DARK_PURPLE));
          return 1;
        })))
        .then(literal("eval").then(argument(ARG_TEXT, miniMessage()).executes(ctx -> {
          final Component result = component(ctx, ARG_TEXT);
          this.platform.audience(ctx.getSource()).sendMessage(this.platform.asAdventure(ComponentUtils.updateForEntity(ctx.getSource(), this.platform.toNative(result), ctx.getSource().getEntity(), 0)));
          return Command.SINGLE_SUCCESS;
        })))
        .then(literal("countdown").then(argument(ARG_SECONDS, integer()).executes(ctx -> { // multiple boss bars!
          final Audience audience = this.adventure().audience(ctx.getSource());
          this.beginCountdown(ctx.getSource().getServer(), text("Countdown"), getInteger(ctx, ARG_SECONDS), audience, BossBar.Color.RED, complete -> {
            complete.sendActionBar(text("Countdown complete!", COLOR_RESPONSE));
          });
          this.beginCountdown(ctx.getSource().getServer(), text("Faster Countdown"), getInteger(ctx, ARG_SECONDS) / 2, audience, BossBar.Color.PURPLE, complete -> {
            complete.sendActionBar(text().content("Faster Countdown complete! ").color(COLOR_RESPONSE)
              .append(text('\uE042', Style.style().font(FONT_MEOW).build()))); // private use kitten in font
          });
          return 1;
        })))
        .then(literal("tellall").then(argument(ARG_TARGETS, players()).then(argument(ARG_TEXT, miniMessage()).executes(ctx -> {
          final Collection<ServerPlayer> targets = getPlayers(ctx, ARG_TARGETS);
          final AdventureCommandSourceStack source = this.adventure().audience(ctx.getSource());
          final Component message = component(ctx, ARG_TEXT);
          final Audience destination = Audience.audience(((Collection) targets));

          destination.sendMessage(message, ADVENTURE_BROADCAST.bind(this.platform.asAdventure(ctx.getSource().getDisplayName())));
          source.sendMessage(text(b -> {
            b.content("You have sent \"");
            b.append(message).append(text("\" to ")).append(this.listPlayers(targets));
            b.color(COLOR_RESPONSE);
          }));
          return 1;
        }))))
        .then(literal("sound").then(argument(ARG_SOUND, key()).suggests(SuggestionProviders.AVAILABLE_SOUNDS).executes(ctx -> {
          final Audience viewer = this.adventure().audience(ctx.getSource());
          final Key sound = key(ctx, ARG_SOUND);
          viewer.sendMessage(text(b -> b.content("Playing sound ").append(represent(sound)).color(COLOR_RESPONSE)));
          viewer.playSound(sound(sound, Sound.Source.MASTER, 1f, 1f));
          return 1;
        })))
        .then(literal("book_callback").executes(ctx -> {
          final ClickEvent callback = ClickEvent.callback(
            aud -> aud.openBook(Book.builder()
              .title(text("My book", NamedTextColor.RED))
              .author(text("The adventure team", COLOR_RESPONSE))
              .addPage(text("Welcome to our rules page"))
              .addPage(text("Let's do a thing!"))
              .build()),
            opts -> opts.uses(1)
          );
          LOGGER.info("{}", callback);

          this.platform.audience(ctx.getSource()).sendMessage(Component.textOfChildren(text("Click here").clickEvent(callback), text(" to see important information!")));
          return 1;
        }))
        .then(literal("rename").then(argument(ARG_TARGET, EntityArgument.entity()).then(argument(ARG_TEXT, miniMessage()).executes(ctx -> {
          final Entity target = EntityArgument.getEntity(ctx, ARG_TARGET);
          final Component title = component(ctx, ARG_TEXT);
          final var oldDisplayName = target.getDisplayName();
          target.setCustomName(this.adventure().toNative(title));
          this.platform.audience(ctx.getSource()).sendSuccess(text()
            .color(COLOR_RESPONSE)
            .content("Successfully set entity ")
            .append(this.platform.asAdventure(oldDisplayName))
            .append(text("'s display name to "))
            .append(title)
            .build(), false);
          return Command.SINGLE_SUCCESS;
        }))))
        .then(literal("rgb").executes(ctx -> {
          for (final TextColor color : LINE_COLOURS) {
            this.platform.audience(ctx.getSource()).sendMessage(LINE.color(color));
          }
          return Command.SINGLE_SUCCESS;
        }))
        .then(literal("baron").executes(ctx -> {
          final ServerPlayer player = ctx.getSource().getPlayerOrException();
          final BossBar greeting = this.greetingBars.computeIfAbsent(player.getUUID(), id -> {
            return BossBar.bossBar(translatable("adventure.test.greeting", NamedTextColor.GOLD, this.platform.asAdventure(player.getDisplayName())),
              1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
          });

          this.adventure().audience(ctx.getSource()).showBossBar(greeting);
          return Command.SINGLE_SUCCESS;
        }))
        .then(literal("baroff").executes(ctx -> {
          final BossBar existing = this.greetingBars.remove(ctx.getSource().getPlayerOrException().getUUID());
          if (existing != null) {
            this.adventure().audience(ctx.getSource()).hideBossBar(existing);
          }
          return Command.SINGLE_SUCCESS;
        }))
        .then(literal("tablist").executes(ctx -> {
          final Audience target = this.adventure().audience(ctx.getSource());
          target.sendPlayerListHeader(text("Adventure", COLOR_NAMESPACE));
          target.sendPlayerListFooter(text("test platform!", COLOR_PATH));
          return Command.SINGLE_SUCCESS;
        }))
        .then(literal("plain").then(argument(ARG_TEXT, miniMessage()).executes(ctx -> {
          final Component text = component(ctx, ARG_TEXT);
          ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(PlainTextComponentSerializer.plainText().serialize(text)), false);
          return Command.SINGLE_SUCCESS;
        }))));
    });
  }

  private static final Component COLON = text(":", NamedTextColor.GRAY);
  private static final TextColor COLOR_PATH = color(0x18A4C2);
  private static final TextColor COLOR_NAMESPACE = color(0x0D6679);
  private static final TextColor COLOR_NAMESPACE_VANILLA = color(0x4A656B);

  private static Component represent(final @NotNull Key ident) {
    final TextColor namespaceColor;
    if (ident.namespace().equals("minecraft")) { // de-emphasize
      namespaceColor = COLOR_NAMESPACE_VANILLA;
    } else {
      namespaceColor = COLOR_NAMESPACE;
    }

    return text()
      .content(ident.namespace())
      .color(namespaceColor)
      .append(COLON)
      .append(text(ident.value(), COLOR_PATH))
      .build();

  }

  private Component listPlayers(final Collection<? extends ServerPlayer> players) {
    final HoverEvent<Component> hover = HoverEvent.showText(Component.text(b -> {
      boolean first = true;
      for (final ServerPlayer player : players) {
        if (!first) {
          b.append(newline());
        }
        first = false;
        ((Pointered) player).get(Identity.DISPLAY_NAME).ifPresent(b::append);
      }
    }));
    return text().content(players.size() + " players")
      .decoration(TextDecoration.UNDERLINED, true)
      .hoverEvent(hover).build();
  }

  /**
   * Begin a countdown shown on a boss bar, completing with the specified action.
   *
   * @param server           The server to schedule operations on
   * @param title            Boss bar title
   * @param timeSeconds      seconds boss bar will last
   * @param targets          viewers of the action
   * @param color            the color of the boss bar
   * @param completionAction callback to execute when countdown is complete
   */
  private void beginCountdown(final MinecraftServer server, final Component title, final int timeSeconds, final Audience targets, final BossBar.Color color, final Consumer<Audience> completionAction) {
    final BossBar bar = BossBar.bossBar(title.style(builder -> builder.colorIfAbsent(textColor(color)).font(FONT_IOSEVKA)), 1, color, BossBar.Overlay.PROGRESS, Collections.singleton(BossBar.Flag.PLAY_BOSS_MUSIC));

    final int timeMs = timeSeconds * 1000; // total time ms
    final long[] times = new long[]{timeMs, System.currentTimeMillis()}; // remaining time in ms, last update time
    final AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

    task.set(this.executor.scheduleAtFixedRate(() -> {
      final long now = System.currentTimeMillis();
      final long dt = now - times[1];
      times[0] -= dt;
      times[1] = now;

      if (times[0] <= 0) { // we are complete
        final ScheduledFuture<?> future = task.getAndSet(null);
        if (future != null) {
          future.cancel(false);
        }
        targets.hideBossBar(bar);
        completionAction.accept(targets);
        return;
      }

      final float newFraction = bar.progress() - (dt / (float) timeMs);
      assert newFraction > 0;

      server.execute(() -> bar.progress(newFraction));
    }, 1, 100, TimeUnit.MILLISECONDS)); // no delay, 100ms tick rate (every 2 ticks)
    targets.showBossBar(bar);
  }

  static TextColor textColor(final BossBar.Color barColor) {
    return switch (barColor) {
      case PINK -> NamedTextColor.LIGHT_PURPLE;
      case BLUE -> NamedTextColor.BLUE;
      case RED -> NamedTextColor.RED;
      case GREEN -> NamedTextColor.GREEN;
      case YELLOW -> NamedTextColor.YELLOW;
      case PURPLE -> NamedTextColor.DARK_PURPLE;
      case WHITE -> NamedTextColor.WHITE;
    };
  }
}
