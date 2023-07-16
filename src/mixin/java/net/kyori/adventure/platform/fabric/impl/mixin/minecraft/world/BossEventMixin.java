/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2023 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.world;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.platform.fabric.impl.AbstractBossBarListener;
import net.kyori.adventure.platform.fabric.impl.BossEventBridge;
import net.minecraft.world.BossEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BossEvent.class)
public abstract class BossEventMixin implements BossEventBridge {
  private AbstractBossBarListener<?, ?> controller;
  private BossBar advBar;

  @Override
  public @Nullable AbstractBossBarListener<?, ?> adventure$bridge$controller() {
    return this.controller;
  }

  @Override
  public @Nullable BossBar adventure$bridge$bar() {
    return this.advBar;
  }

  @Override
  public @Nullable AbstractBossBarListener<?, ?> adventure$bridge$link(@Nullable AbstractBossBarListener<?, ?> controller, BossBar bar) {
    final var oldController = this.controller;
    this.controller = controller;
    this.advBar = bar;
    return oldController;
  }

  @Override
  public @Nullable AbstractBossBarListener<?, ?> adventure$bridge$unlink() {
    final var oldController = this.controller;
    this.controller = null;
    this.advBar = null;
    return oldController;
  }
}
