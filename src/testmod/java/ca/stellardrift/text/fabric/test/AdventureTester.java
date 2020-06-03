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

package ca.stellardrift.text.fabric.test;

import ca.stellardrift.text.fabric.AdventureCommandSource;
import ca.stellardrift.text.fabric.Audiences;
import ca.stellardrift.text.fabric.TextAdapter;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.command.arguments.MessageArgumentType;
import net.minecraft.command.arguments.TextArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import static ca.stellardrift.text.fabric.ComponentArgumentType.component;
import static ca.stellardrift.text.fabric.ComponentArgumentType.getComponent;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.kyori.adventure.text.TextComponent.newline;
import static net.minecraft.command.arguments.EntityArgumentType.getPlayers;
import static net.minecraft.command.arguments.EntityArgumentType.players;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AdventureTester implements ModInitializer {
  private static final String ARG_TEXT = "text";
  private static final String ARG_SECONDS = "seconds";
  private static final String ARG_TARGETS = "targets";
  private static final TextColor COLOR_RESPONSE = TextColor.of(0x22EE99);
  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Override
  public void onInitialize() {

    ServerStartCallback.EVENT.register(server -> { // TODO: workaround for broken command registration event
      final CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
      dispatcher.register(literal("adventure")
        .then(literal("echo").then(argument(ARG_TEXT, component()).executes(ctx -> {
          final Audience audience = AdventureCommandSource.of(ctx.getSource());
          final Component result = getComponent(ctx, ARG_TEXT);
          audience.sendMessage(result);
          return 1;
        })))
        .then(literal("countdown").then(argument(ARG_SECONDS, integer()).executes(ctx -> { // multiple boss bars!
          final Audience audience = AdventureCommandSource.of(ctx.getSource());
          beginCountdown(TextComponent.of("Countdown"), getInteger(ctx, ARG_SECONDS), audience, BossBar.Color.RED, complete -> {
            complete.sendActionBar(TextComponent.of("Countdown complete!", COLOR_RESPONSE));
          });
          beginCountdown(TextComponent.of("Faster Countdown"), getInteger(ctx, ARG_SECONDS) / 2, audience, BossBar.Color.PURPLE, complete -> {
            complete.showTitle(Title.of(TextComponent.of("Faster Countdown"), TextComponent.of("Complete"), Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofSeconds(5)));
            complete.sendActionBar(TextComponent.of("Faster Countdown complete!", COLOR_RESPONSE));
          });
          return 1;
        })))
      .then(literal("tellall").then(argument(ARG_TARGETS, players()).then(argument(ARG_TEXT, component()).executes(ctx -> {
        final Collection<ServerPlayerEntity> targets = getPlayers(ctx, ARG_TARGETS);
        final Audience source = AdventureCommandSource.of(ctx.getSource());
        final Component message = getComponent(ctx, ARG_TEXT);
        final Audience destination = Audiences.of(targets);

        destination.sendMessage(message);
        source.sendMessage(TextComponent.make("You have sent \"", b -> {
          b.append(message).append("\" to ").append(listPlayers(targets));
          b.color(COLOR_RESPONSE);
        }));
        return 1;
      })))));
    });
  }

  private static Component listPlayers(Collection<? extends ServerPlayerEntity> players) {
    final HoverEvent<Component> hover = HoverEvent.showText(TextComponent.make(b -> {
      boolean first = true;
      for (ServerPlayerEntity player : players) {
        if (!first) {
          b.append(newline());
        }
        first = false;
        Component component = TextAdapter.adapt(player.getDisplayName());
        Audiences.of(player).sendMessage(TextComponent.builder("You are ", COLOR_RESPONSE).append(component).build());
        b.append(component);
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
    final BossBar bar = BossBar.of(title.colorIfAbsent(textColor(color)), 1, color, BossBar.Overlay.PROGRESS, Collections.singleton(BossBar.Flag.PLAY_BOSS_MUSIC));

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
