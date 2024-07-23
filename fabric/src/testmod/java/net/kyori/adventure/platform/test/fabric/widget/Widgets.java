/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
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
package net.kyori.adventure.platform.test.fabric.widget;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import org.jetbrains.annotations.Nullable;

/**
 * An action that can be performed.
 *
 * <p>Each action is placed in line in a bar above the player chat UI.</p>
 *
 * <p>This creates vanilla widgets with initialized width and height, but
 * uninitialized positions. Containers should lay out their children.</p>
 */
public final class Widgets {
  public static final int BUTTON_SIZE = 20;
  public static final int IN_GROUP_SPACING = 2;
  public static final int BETWEEN_GROUP_SPACING = 4;

  public static Button button(final Component label, final Button.OnPress clickAction) {
    return button(label, clickAction, null);
  }

  public static Button button(final Component label, final Button.OnPress clickAction, final @Nullable Tooltip tooltip) {
    final net.minecraft.network.chat.Component mcComponent = MinecraftClientAudiences.of().toNative(label);
    final int textWidth = Minecraft.getInstance().font.width(mcComponent);
    // x, y, width, height, label, clickAction, tooltipAction
    return Button.builder(mcComponent, clickAction)
      .bounds(0, 0, IN_GROUP_SPACING * 2 + textWidth, BUTTON_SIZE)
      .tooltip(tooltip)
      .build();
  }

  /**
   * Create a checkbox with the given label, starting out unchecked,
   * and with a consumer taking the current pressed state of the button.
   *
   * @param label label
   * @param whenPressed action to perform when the checkbox is pressed
   * @return a new checkbox
   */
  public static Checkbox checkbox(final Component label, final BooleanConsumer whenPressed) {
    return checkbox(label, false, whenPressed);
  }

  /**
   * Create a checkbox with the given label, the initial state,
   * and with a consumer taking the current pressed state of the button.
   *
   * @param label label
   * @param initialState the checked state to start out with
   * @param whenPressed action to perform when the checkbox is pressed
   * @return a new checkbox
   */
  public static Checkbox checkbox(final Component label, final boolean initialState, final BooleanConsumer whenPressed) {
    final net.minecraft.network.chat.Component mcComponent = MinecraftClientAudiences.of().toNative(label);
    return Checkbox.builder(mcComponent, Minecraft.getInstance().font)
      .selected(initialState)
      .onValueChange((checkbox, bl) -> whenPressed.accept(bl))
      .build();
  }

  private Widgets() {
  }

}
