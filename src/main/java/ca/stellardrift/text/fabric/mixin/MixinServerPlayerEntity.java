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

import ca.stellardrift.text.fabric.ComponentPlayer;
import ca.stellardrift.text.fabric.GameEnums;
import ca.stellardrift.text.fabric.TextAdapter;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.minecraft.client.network.packet.SetTradeOffersS2CPacket;
import net.minecraft.client.network.packet.StopSoundS2CPacket;
import net.minecraft.client.network.packet.TitleS2CPacket;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements ComponentPlayer {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    @Shadow public abstract void playSound(SoundEvent soundEvent, SoundCategory soundCategory, float f, float g);

    @Shadow public abstract void sendAbilitiesUpdate();

    public MixinServerPlayerEntity(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    /**
     * Send a message to this receiver as a component
     *
     * @param text The text to send
     */
    @Override
    public void message(Component text) {
        message(text, MessageType.SYSTEM);
    }

    @Override
    public void message(Component text, MessageType type) {
        if (type == MessageType.GAME_INFO) {
            sendTitle(TitleS2CPacket.Action.ACTIONBAR, text);
        } else {
            networkHandler.sendPacket(TextAdapter.createChatPacket(text, type));
        }
    }

    @Override
    public void sendTitle(TitleS2CPacket.Action field, Component text) {
        networkHandler.sendPacket(TextAdapter.createTitlePacket(field, text));
    }

    @Override
    public void showBossBar(@NonNull BossBar bar) {
        ((ServerBossBar) bar).addPlayer((ServerPlayerEntity) (Object) this);
    }

    @Override
    public void hideBossBar(@NonNull BossBar bar) {
        ((ServerBossBar) bar).removePlayer((ServerPlayerEntity) (Object) this);
    }

    @Override
    public void playSound(@NonNull Sound sound) {
        SoundEvent event = Registry.SOUND_EVENT.get(TextAdapter.toIdentifier(sound.name()));
        if (event == null) {
            throw new IllegalArgumentException("Unknown sound event " + sound.name());
        }
        this.playSound(event, GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), sound.volume(), sound.pitch());
    }

    @Override
    public void stopSound(@NonNull SoundStop stop) {
        SoundCategory cat = stop.source() == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(stop.source());
        this.networkHandler.sendPacket(new StopSoundS2CPacket(TextAdapter.toIdentifier(stop.sound()), cat));
    }
}
