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
package net.kyori.adventure.platform.test.fabric.mixin.client;

import net.kyori.adventure.platform.test.fabric.widget.AdventureTestButtons;
import net.kyori.adventure.platform.test.fabric.widget.Widgets;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
  // @formatter:off
  @Shadow protected EditBox input;
  // @formatter:on

  protected ChatScreenMixin(final Component label) {
    super(label);
  }

  @Inject(method = "init()V", at = @At("TAIL"))
  private void testmod$addCustomWidgets(final CallbackInfo ci) {
    // Instantiate and add all children
    int x = Widgets.BETWEEN_GROUP_SPACING; // padding used for chat screen edit box
    final int y = this.height - ( // starting from bottom of the screen
      this.minecraft.gui.getChat().getHeight() // height of the chat box
        + this.input.getHeight() // height of the edit bar
        + (int) Math.floor(24 * this.minecraft.gui.getChat().getScale())
        + Widgets.BETWEEN_GROUP_SPACING * 2); // paddings

    for (final AbstractWidget widget : AdventureTestButtons.testItems()) {
      widget.x = x;
      widget.y = y - widget.getHeight(); // the y above takes us to the bottom of a widget, since we want to align those

      if (widget.getHeight() > Widgets.BUTTON_SIZE) {
        widget.y -= widget.getHeight() - Widgets.BUTTON_SIZE;
      }

      x += widget.getWidth() + Widgets.IN_GROUP_SPACING;

      this.addRenderableWidget(widget);
    }
  }
}
