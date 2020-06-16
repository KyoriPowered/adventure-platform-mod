/*
 * Copyright © 2020 zml [at] stellardrift [.] ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.kyori.adventure.platform.test.fabric;

import net.kyori.adventure.platform.fabric.ComponentArgumentType;
import net.kyori.adventure.platform.fabric.FabricPlatform;
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
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static java.util.Objects.requireNonNull;
import static net.kyori.adventure.text.TextComponent.newline;
import static net.minecraft.command.arguments.EntityArgumentType.getPlayers;
import static net.minecraft.command.arguments.EntityArgumentType.players;
import static net.minecraft.command.arguments.IdentifierArgumentType.getIdentifier;
import static net.minecraft.command.arguments.IdentifierArgumentType.identifier;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AdventureTester implements ModInitializer {
  private static final Key FONT_MEOW = Key.of("adventure", "meow");
  private static final Key FONT_IOSEVKA = Key.of("adventure", "iosevka");

  private static final String ARG_TEXT = "text";
  private static final String ARG_SECONDS = "seconds";
  private static final String ARG_TARGETS = "targets";
  private static final String ARG_SOUND = "sound";
  private static final TextColor COLOR_RESPONSE = TextColor.of(0x22EE99);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  private @Nullable FabricPlatform platform;

  public FabricPlatform adventure() {
    return requireNonNull(this.platform, "Tried to access Fabric platform without a running server");
  }

  @Override
  public void onInitialize() {
    // Set up platform
    ServerStartCallback.EVENT.register(server -> this.platform = FabricPlatform.of(server));
    ServerStopCallback.EVENT.register(server -> this.platform = null);

    CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
      dispatcher.register(literal("adventure")
        .then(literal("echo").then(argument(ARG_TEXT, ComponentArgumentType.component()).executes(ctx -> {
          final Audience audience = adventure().audience(ctx.getSource());
          final Component result = ComponentArgumentType.getComponent(ctx, ARG_TEXT);
          audience.sendMessage(result);
          return 1;
        })))
        .then(literal("countdown").then(argument(ARG_SECONDS, integer()).executes(ctx -> { // multiple boss bars!
          final Audience audience = adventure().audience(ctx.getSource());
          beginCountdown(TextComponent.of("Countdown"), getInteger(ctx, ARG_SECONDS), audience, BossBar.Color.RED, complete -> {
            complete.sendActionBar(TextComponent.of("Countdown complete!", COLOR_RESPONSE));
          });
          beginCountdown(TextComponent.of("Faster Countdown"), getInteger(ctx, ARG_SECONDS) / 2, audience, BossBar.Color.PURPLE, complete -> {
            complete.sendActionBar(TextComponent.builder("Faster Countdown complete! ", COLOR_RESPONSE).append(TextComponent.of('\uE042', Style.builder().font(FONT_MEOW).build())).build());
          });
          return 1;
        })))
      .then(literal("tellall").then(argument(ARG_TARGETS, players()).then(argument(ARG_TEXT, ComponentArgumentType.component()).executes(ctx -> {
        final Collection<ServerPlayerEntity> targets = getPlayers(ctx, ARG_TARGETS);
        final Audience source = adventure().audience(ctx.getSource());
        final Component message = ComponentArgumentType.getComponent(ctx, ARG_TEXT);
        final Audience destination = adventure().audience(targets);

        destination.sendMessage(message);
        source.sendMessage(TextComponent.make("You have sent \"", b -> {
          b.append(message).append("\" to ").append(listPlayers(targets));
          b.color(COLOR_RESPONSE);
        }));
        return 1;
      }))))
      .then(literal("sound").then(argument(ARG_SOUND, identifier()).executes(ctx -> {
        final Audience viewer = adventure().audience(ctx.getSource());
        final Key sound = FabricPlatform.adapt(getIdentifier(ctx, ARG_SOUND));
        viewer.sendMessage(TextComponent.make("Playing sound ", b -> b.append(represent(sound)).color(COLOR_RESPONSE)));
        viewer.playSound(Sound.of(sound, Sound.Source.MASTER, 1f, 1f));
        return 1;
      })))
      .then(literal("book").executes(ctx -> {
        final Audience viewer = adventure().audience(ctx.getSource());
        viewer.openBook(Book.builder()
        .title(TextComponent.of("My book", NamedTextColor.RED))
        .author(TextComponent.of("The adventure team", COLOR_RESPONSE))
        .page(TextComponent.of("Welcome to our rules page"))
        .page(TextComponent.of("Let's do a thing!"))
        .build());
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

  private static Component listPlayers(Collection<? extends ServerPlayerEntity> players) {
    final HoverEvent<Component> hover = HoverEvent.showText(TextComponent.make(b -> {
      boolean first = true;
      for (ServerPlayerEntity player : players) {
        if (!first) {
          b.append(newline());
        }
        first = false;
        b.append(FabricPlatform.adapt(player.getDisplayName()));
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
  private void beginCountdown(Component title, final int timeSeconds, Audience targets, BossBar.Color color, Consumer<Audience> completionAction) {
    final BossBar bar = BossBar.of(title.style(builder -> builder.colorIfAbsent(textColor(color)).font(FONT_IOSEVKA)), 1, color, BossBar.Overlay.PROGRESS, Collections.singleton(BossBar.Flag.PLAY_BOSS_MUSIC));

    final int timeMs = timeSeconds * 1000; // total time ms
    final long[] times = new long[] {timeMs, System.currentTimeMillis()}; // remaining time in ms, last update time
    final AtomicReference<ScheduledFuture<?>> task = new AtomicReference<>();

    task.set(executor.scheduleAtFixedRate(() -> {
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
