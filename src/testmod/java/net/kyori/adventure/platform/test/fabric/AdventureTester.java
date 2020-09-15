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

package net.kyori.adventure.platform.test.fabric;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudienceProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.fabric.KeyArgumentType;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.TranslationRegistry;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.platform.fabric.ComponentArgumentType.component;
import static net.kyori.adventure.platform.fabric.KeyArgumentType.key;
import static net.kyori.adventure.text.TextComponent.newline;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.getPlayers;
import static net.minecraft.commands.arguments.EntityArgument.players;

public class AdventureTester implements ModInitializer {
  private static final Key FONT_MEOW = Key.of("adventure", "meow");
  private static final Key FONT_IOSEVKA = Key.of("adventure", "iosevka");

  private static final List<TextColor> LINE_COLOURS = IntStream.of(0x9400D3, 0x4B0082, 0x0000FF, 0x00FF00, 0xFFFF00, 0xFF7F00, 0xFF0000,
                                                                  0x55CDFC, 0xF7A8B8, 0xFFFFFF, 0xF7A8B8, 0x55CDFC)
                                                                  .mapToObj(TextColor::of).collect(Collectors.toList());
  private static final Component LINE = TextComponent.of(Strings.repeat("█", 10));

  private static final String ARG_TEXT = "text";
  private static final String ARG_SECONDS = "seconds";
  private static final String ARG_TARGETS = "targets";
  private static final String ARG_SOUND = "sound";
  private static final TextColor COLOR_RESPONSE = TextColor.of(0x22EE99);

  private final Map<UUID, BossBar> greetingBars = new HashMap<>();
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  private @Nullable FabricServerAudienceProvider platform;

  public FabricServerAudienceProvider adventure() {
    return requireNonNull(this.platform, "Tried to access Fabric platform without a running server");
  }

  @Override
  public void onInitialize() {
    // Register localizations
    Stream.of(Locale.ENGLISH, Locale.GERMAN).forEach(lang -> {
      TranslationRegistry.get().registerAll(lang, "net.kyori.adventure.platform.test.fabric.messages", false);
    });
    // Set up platform
    ServerLifecycleEvents.SERVER_STARTING.register(server -> this.platform = FabricServerAudienceProvider.of(server));
    ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
      this.platform = null;
      this.greetingBars.clear();
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
      dispatcher.register(literal("adventure")
        .then(literal("about").executes(ctx -> {
          final Audience audience = this.adventure().audience(ctx.getSource());
          audience.sendMessage(TranslatableComponent.of("adventure.test.welcome", COLOR_RESPONSE, this.adventure().toAdventure(ctx.getSource().getDisplayName())));
          audience.sendMessage(TranslatableComponent.of("adventure.test.description", TextColor.of(0xc022cc)));
          return 1;
        }))
        .then(literal("echo").then(argument(ARG_TEXT, component()).executes(ctx -> {
          final Audience audience = this.adventure().audience(ctx.getSource());
          final Component result = component(ctx, ARG_TEXT);
          audience.sendMessage(result);
          return 1;
        })))
        .then(literal("countdown").then(argument(ARG_SECONDS, integer()).executes(ctx -> { // multiple boss bars!
          final Audience audience = this.adventure().audience(ctx.getSource());
          this.beginCountdown(TextComponent.of("Countdown"), getInteger(ctx, ARG_SECONDS), audience, BossBar.Color.RED, complete -> {
            complete.sendActionBar(TextComponent.of("Countdown complete!", COLOR_RESPONSE));
          });
          this.beginCountdown(TextComponent.of("Faster Countdown"), getInteger(ctx, ARG_SECONDS) / 2, audience, BossBar.Color.PURPLE, complete -> {
            complete.sendActionBar(TextComponent.builder("Faster Countdown complete! ", COLOR_RESPONSE)
              .append(TextComponent.of('\uE042', Style.builder().font(FONT_MEOW).build())).build()); // private use kitten in font
          });
          return 1;
        })))
      .then(literal("tellall").then(argument(ARG_TARGETS, players()).then(argument(ARG_TEXT, component()).executes(ctx -> {
        final Collection<ServerPlayer> targets = getPlayers(ctx, ARG_TARGETS);
        final Audience source = this.adventure().audience(ctx.getSource());
        final Component message = component(ctx, ARG_TEXT);
        final Audience destination = this.adventure().audience(targets);

        destination.sendMessage(message);
        source.sendMessage(TextComponent.make("You have sent \"", b -> {
          b.append(message).append("\" to ").append(this.listPlayers(targets));
          b.color(COLOR_RESPONSE);
        }));
        return 1;
      }))))
      .then(literal("sound").then(argument(ARG_SOUND, key()).suggests(SuggestionProviders.AVAILABLE_SOUNDS).executes(ctx -> {
        final Audience viewer = this.adventure().audience(ctx.getSource());
        final Key sound = KeyArgumentType.key(ctx, ARG_SOUND);
        viewer.sendMessage(TextComponent.make("Playing sound ", b -> b.append(represent(sound)).color(COLOR_RESPONSE)));
        viewer.playSound(Sound.of(sound, Sound.Source.MASTER, 1f, 1f));
        return 1;
      })))
      .then(literal("book").executes(ctx -> {
        final Audience viewer = this.adventure().audience(ctx.getSource());
        viewer.openBook(Book.builder()
        .title(TextComponent.of("My book", NamedTextColor.RED))
        .author(TextComponent.of("The adventure team", COLOR_RESPONSE))
        .addPage(TextComponent.of("Welcome to our rules page"))
        .addPage(TextComponent.of("Let's do a thing!"))
        .build());
        return 1;
      }))
      .then(literal("rgb").executes(ctx -> {
        final Audience viewer = this.adventure().audience(ctx.getSource());
        for(final TextColor color : LINE_COLOURS) {
          viewer.sendMessage(LINE.color(color));
        }
        return 1;
      }))
      .then(literal("baron").executes(ctx -> {
        final ServerPlayer player = ctx.getSource().getPlayerOrException();
        final BossBar greeting = this.greetingBars.computeIfAbsent(player.getUUID(), id -> {
          return BossBar.of(TranslatableComponent.of("adventure.test.greeting", NamedTextColor.GOLD, this.adventure().toAdventure(player.getDisplayName())),
          1, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        });

        this.adventure().audience(ctx.getSource()).showBossBar(greeting);
        return 1;
      }))
      .then(literal("baroff").executes(ctx -> {
        final BossBar existing = this.greetingBars.remove(ctx.getSource().getPlayerOrException().getUUID());
        if(existing != null) {
          this.adventure().audience(ctx.getSource()).hideBossBar(existing);
        }
        return 1;
      })));
    });
  }

  private static final Component COLON = TextComponent.of(":", NamedTextColor.GRAY);
  private static final TextColor COLOR_PATH = TextColor.of(0x18A4C2);
  private static final TextColor COLOR_NAMESPACE = TextColor.of(0x0D6679);
  private static final TextColor COLOR_NAMESPACE_VANILLA = TextColor.of(0x4A656B);

  private static Component represent(final @NonNull Key ident) {
    final TextColor namespaceColor;
    if(ident.namespace().equals("minecraft")) { // de-emphasize
      namespaceColor = COLOR_NAMESPACE_VANILLA;
    } else {
      namespaceColor = COLOR_NAMESPACE;
    }

    return TextComponent.builder(ident.namespace(), namespaceColor)
      .append(COLON)
      .append(TextComponent.of(ident.value(), COLOR_PATH))
      .build();

  }

  private Component listPlayers(final Collection<? extends ServerPlayer> players) {
    final HoverEvent<Component> hover = HoverEvent.showText(TextComponent.make(b -> {
      boolean first = true;
      for(final ServerPlayer player : players) {
        if(!first) {
          b.append(newline());
        }
        first = false;
        b.append(this.adventure().toAdventure(player.getDisplayName()));
      }
    }));
    return TextComponent.builder(players.size() + " players")
      .decoration(TextDecoration.UNDERLINED, true)
      .hoverEvent(hover).build();
  }

  /**
   * Begin a countdown shown on a boss bar, completing with the specified action
   *
   * @param title Boss bar title
   * @param timeSeconds seconds boss bar will last
   * @param targets viewers of the action
   * @param completionAction callback to execute when countdown is complete
   */
  private void beginCountdown(final Component title, final int timeSeconds, final Audience targets, final BossBar.Color color, final Consumer<Audience> completionAction) {
    final BossBar bar = BossBar.of(title.style(builder -> builder.colorIfAbsent(textColor(color)).font(FONT_IOSEVKA)), 1, color, BossBar.Overlay.PROGRESS, Collections.singleton(BossBar.Flag.PLAY_BOSS_MUSIC));

    final int timeMs = timeSeconds * 1000; // total time ms
    final long[] times = new long[] {timeMs, System.currentTimeMillis()}; // remaining time in ms, last update time
    final AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

    task.set(this.executor.scheduleAtFixedRate(() -> {
      final long now = System.currentTimeMillis();
      final long dt = now - times[1];
      times[0] -= dt;
      times[1] = now;

      if(times[0] <= 0) { // we are complete
        final ScheduledFuture<?> future = task.getAndSet(null);
        if(future != null) {
          future.cancel(false);
        }
        targets.hideBossBar(bar);
        completionAction.accept(targets);
        return;
      }

      final float newFraction = bar.percent() - (dt / (float) timeMs);
      assert newFraction > 0;
      bar.percent(newFraction);
    }, 1, 10, TimeUnit.MILLISECONDS)); // no delay, 10ms tick rate
    targets.showBossBar(bar);
  }

  static TextColor textColor(final BossBar.Color barColor) {
    switch(barColor) {
      case PINK: return NamedTextColor.LIGHT_PURPLE;
      case BLUE: return NamedTextColor.BLUE;
      case RED: return NamedTextColor.RED;
      case GREEN: return NamedTextColor.GREEN;
      case YELLOW: return NamedTextColor.YELLOW;
      case PURPLE: return NamedTextColor.DARK_PURPLE;
      case WHITE: return NamedTextColor.WHITE;
      default: throw new IllegalArgumentException("Unknown color " + barColor);
    }
  }
}
