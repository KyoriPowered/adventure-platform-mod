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

package ca.stellardrift.adventure.fabric.mixin;

import ca.stellardrift.adventure.fabric.FabricAudience;
import ca.stellardrift.adventure.fabric.FabricPlatform;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.MessageType;
import net.minecraft.server.dedicated.ServerCommandOutput;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerCommandOutput.class)
public abstract class MixinServerCommandOutput implements FabricAudience {
    @Shadow @Final private StringBuffer buffer;

    @Override
    public void sendMessage(final MessageType type, final Component text, final UUID source) {
        this.buffer.append(FabricPlatform.plain().serialize(text));
    }

    @Override
    public void showBossBar(@NonNull final BossBar bar) { }

    @Override
    public void hideBossBar(@NonNull final BossBar bar) { }

    @Override
    public void playSound(@NonNull final Sound sound) { }

    @Override
    public void stopSound(@NonNull final SoundStop stop) { }

    @Override
    public void showTitle(@NonNull final Title title) { }

    @Override
    public void clearTitle() { }

    @Override
    public void resetTitle() { }
}
