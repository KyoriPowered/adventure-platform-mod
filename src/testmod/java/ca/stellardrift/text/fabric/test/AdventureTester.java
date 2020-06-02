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
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Collections;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.server.command.ServerCommandSource;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AdventureTester implements ModInitializer {
  @Override
  public void onInitialize() {

    ServerStartCallback.EVENT.register(server -> { // TODO: workaround for broken command registration event
      final CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();
      dispatcher.register(literal("adventure")
        .then(literal("echo").then(argument("text", StringArgumentType.greedyString()).executes(ctx -> {
          final Audience audience = AdventureCommandSource.of(ctx.getSource());
          final String arg = StringArgumentType.getString(ctx, "text");
          Component result;
          try {
            result = GsonComponentSerializer.INSTANCE.deserialize(arg);
          } catch(JsonSyntaxException ex) {
            audience.sendMessage(TextComponent.builder("Unable to parse provided text as JSON: ", NamedTextColor.RED)
              .append(TextComponent.of(ex.getMessage(), NamedTextColor.DARK_RED)).build());
            result = TextComponent.of(arg);
          }
          audience.sendMessage(result);
          return 1;
        })))
        .then(literal("countdown").then(argument("seconds", integer()).executes(ctx -> {
          final Audience audience = AdventureCommandSource.of(ctx.getSource());
          BossBar bar = BossBar.of(TextComponent.of("countdown"), 1f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS, Collections.singleton(BossBar.Flag.PLAY_BOSS_MUSIC));
          audience.showBossBar(bar);
          return 1;
        }))));
    });
  }
}
