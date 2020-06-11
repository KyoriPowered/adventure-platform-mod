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

package ca.stellardrift.text.fabric;

import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.MessageType;
import net.minecraft.server.command.CommandOutput;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;

/**
 * Audience implementation that can wrap a CommandOutput
 */
public final class CommandOutputAudience implements FabricAudience {
  private final CommandOutput output;

  CommandOutputAudience(final CommandOutput output) {
    this.output = output;
  }

  public static FabricAudience of(final CommandOutput output) {
    if (output instanceof FabricAudience) {
      return (FabricAudience) output;
    } else {
      return new CommandOutputAudience(requireNonNull(output, "output"));
    }
  }

  @Override
  public void sendMessage(final MessageType type, final Component text, final UUID source) {
    this.output.sendSystemMessage(TextAdapter.adapt(text), source);
  }

  @Override
  public void showBossBar(@NonNull final BossBar bar) { }

  @Override
  public void hideBossBar(@NonNull final BossBar bar) { }

  @Override
  public void playSound(@NonNull final Sound sound) { }

  @Override
  public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
  }

  @Override
  public void stopSound(@NonNull final SoundStop stop) { }

  @Override
  public void showTitle(@NonNull final Title title) { }

  @Override
  public void clearTitle() { }

  @Override
  public void resetTitle() { }
}
