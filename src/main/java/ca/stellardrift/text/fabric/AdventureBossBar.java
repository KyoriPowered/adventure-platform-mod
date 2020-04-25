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

import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AdventureBossBar extends ServerBossBar implements BossBar {
    private Set<Flag> flags = new HashSet<>();

    public AdventureBossBar(Text text, BossBar.Color color, BossBar.Overlay style) {
        super(text, GameEnums.BOSS_BAR_COLOR.toMinecraft(color), GameEnums.BOSS_BAR_OVERLAY.toMinecraft(style));
    }

    @Override
    public @NonNull Component name() {
        return MinecraftTextSerializer.INSTANCE.deserialize(getName());
    }

    @Override
    public @NonNull BossBar name(@NonNull Component name) {
        setName(MinecraftTextSerializer.INSTANCE.serialize(name));
        return this;
    }

    @Override
    public float percent() {
        return getPercent();
    }

    @Override
    public @NonNull BossBar percent(float percent) {
        setPercent(percent);
        return this;
    }

    @Override
    public BossBar.@NonNull Color color() {
        return GameEnums.BOSS_BAR_COLOR.toAdventure(getColor());
    }

    @Override
    public @NonNull BossBar color(BossBar.@NonNull Color color) {
        setColor(GameEnums.BOSS_BAR_COLOR.toMinecraft(color));
        return this;
    }

    @Override
    public @NonNull Overlay overlay() {
        return GameEnums.BOSS_BAR_OVERLAY.toAdventure(this.style);
    }

    @Override
    public @NonNull BossBar overlay(@NonNull Overlay overlay) {
        setOverlay(GameEnums.BOSS_BAR_OVERLAY.toMinecraft(overlay));
        return this;
    }

    @Override
    public @NonNull Set<Flag> flags() {
        return ImmutableSet.copyOf(flags);
    }

    @Override
    public @NonNull BossBar flags(@NonNull Set<Flag> flags) {
        this.flags = new HashSet<>(flags);
        return sendFlagUpdate();
    }

    @Override
    public @NonNull BossBar addFlags(@NonNull Flag @NonNull... flags) {
        return setFlags(true, flags);
    }

    @Override
    public @NonNull BossBar removeFlags(@NonNull Flag @NonNull... flags) {
        return setFlags(false, flags);
    }
    
    private BossBar setFlags(boolean value, Flag... flags) {
        boolean changed = false;
        
        for (Flag flag : flags) {
            changed |= value ? this.flags.add(flag) : this.flags.remove(flag);
        }
        
        if (changed) {
            sendFlagUpdate();
        }
        return this;
    }
    
    private BossBar sendFlagUpdate() {
        BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.UPDATE_PROPERTIES, this);
        for (ServerPlayerEntity ply : getMutablePlayers()) {
            ply.networkHandler.sendPacket(pkt);
        }
        return this;
    }

    @Override
    public net.minecraft.entity.boss.BossBar setDarkenSky(boolean bl) {
        setFlags(bl, Flag.DARKEN_SCREEN);
        return this;
    }

    @Override
    public net.minecraft.entity.boss.BossBar setDragonMusic(boolean bl) {
        setFlags(bl, Flag.PLAY_BOSS_MUSIC);
        return this;
    }

    @Override
    public net.minecraft.entity.boss.BossBar setThickenFog(boolean bl) {
        setFlags(bl, Flag.CREATE_WORLD_FOG);
        return this;
    }

    @Override
    public boolean getDarkenSky() {
        return flags.contains(Flag.DARKEN_SCREEN);
    }

    @Override
    public boolean hasDragonMusic() {
        return flags.contains(Flag.PLAY_BOSS_MUSIC);
    }

    @Override
    public boolean getThickenFog() {
        return flags.contains(Flag.CREATE_WORLD_FOG);
    }

    private Set<ServerPlayerEntity> getMutablePlayers() {
        return ((ServerBossBarAccess) this).getPlayers();
    }

    void addAll(Collection<ServerPlayerEntity> players) {
        if (!this.isVisible()) {
            getMutablePlayers().addAll(players);
            return;
        }

        BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.ADD, this);
        for (ServerPlayerEntity ply : players) {
            ply.networkHandler.sendPacket(pkt, handler -> getMutablePlayers().add(ply));
        }
    }

    void removeAll(Collection<ServerPlayerEntity> players) {
        if (!this.isVisible()) {
            getMutablePlayers().removeAll(players);
            return;
        }

        BossBarS2CPacket pkt = new BossBarS2CPacket(BossBarS2CPacket.Type.REMOVE, this);
        for (ServerPlayerEntity ply : players) {
            if (getMutablePlayers().remove(ply)) {
                ply.networkHandler.sendPacket(pkt);
            }
        }

    }
}
