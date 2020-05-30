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

package ca.stellardrift.text.fabric.mixin;

import ca.stellardrift.text.fabric.FabricAudience;
import ca.stellardrift.text.fabric.PlainAudience;
import ca.stellardrift.text.fabric.TextAdapter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Implement ComponentCommandOutput for output to the server console
 */
@Mixin(value = MinecraftServer.class)
public abstract class MixinMinecraftServer implements FabricAudience {
    @Shadow @Final
    private static Logger LOGGER;

    /**
     * Send a message to this receiver as a component
     *
     * @param text The text to send
     */
    @Override
    public void sendMessage(MessageType type, Component text) {
        LOGGER.info(TextAdapter.plain().serialize(text)); // TODO: Eventually will we support formatted output?
    }

    @Override
    public void showTitle(TitleS2CPacket.Action field, Component text) {
        this.sendMessage(text);
    }

    @Override
    public void showBossBar(@NonNull BossBar bar) {}

    @Override
    public void hideBossBar(@NonNull BossBar bar) {}

    @Override
    public void playSound(@NonNull Sound sound) {}

    @Override
    public void stopSound(@NonNull SoundStop stop) {}

    @Override
    public void showTitle(@NonNull final Title title) {}

    @Override
    public void clearTitle() { }

    @Override
    public void resetTitle() {}
}
