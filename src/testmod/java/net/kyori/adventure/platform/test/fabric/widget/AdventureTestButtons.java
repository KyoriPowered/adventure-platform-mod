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
package net.kyori.adventure.platform.test.fabric.widget;

import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.fabric.FabricClientAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;

import static net.kyori.adventure.platform.test.fabric.widget.Widgets.button;
import static net.kyori.adventure.platform.test.fabric.widget.Widgets.checkbox;
import static net.kyori.adventure.text.Component.text;

public final class AdventureTestButtons {

  private AdventureTestButtons() {
  }

  public static List<AbstractWidget> testItems() {
    final BossBar testBar = BossBar.bossBar(text("Your current world is called: ").append(text(Minecraft.getInstance().level.dimension().location().toString(), NamedTextColor.AQUA)), 1f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    return List.of(
      button(text("I am a test!"), b -> {
        clientAudience().sendMessage(text("I'm from the client!", NamedTextColor.DARK_PURPLE));
      }),
      checkbox(text("boss bar?"), b -> {
        if (b) {
          clientAudience().showBossBar(testBar);
        } else {
          clientAudience().hideBossBar(testBar);
        }
      })
    );
  }

  private static Audience clientAudience() {
    return FabricClientAudiences.of().audience();
  }
}
