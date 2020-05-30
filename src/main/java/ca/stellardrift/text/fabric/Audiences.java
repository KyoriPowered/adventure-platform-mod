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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Audiences {
    private Audiences() {
        // no
    }

    public static Audience console() {
        final @Nullable MinecraftServer server = TextAdapter.server();
        return server == null ? Audience.empty() : (FabricAudience) server;
    }

    public static FabricAudience of(ServerPlayerEntity player) {
        return (FabricAudience) player;
    }

    public static Audience of(Collection<ServerPlayerEntity> players) {
        if (players.size() == 1) {
            return of(players.iterator().next());
        }
        return new MultiAudience(ImmutableSet.copyOf(players), ImmutableSet.of(), ImmutableSet.of());
    }
    
    public static Audience of(Iterable<? extends CommandOutput> sources) {
        final ImmutableSet.Builder<ServerPlayerEntity> players = ImmutableSet.builder();
        final ImmutableSet.Builder<Audience> audiences = ImmutableSet.builder();
        final ImmutableSet.Builder<CommandOutput> others = ImmutableSet.builder();
        boolean first = true;
        for (Iterator<? extends CommandOutput> it = sources.iterator(); it.hasNext();) {
            CommandOutput out = it.next();
            if (first && !it.hasNext() && out instanceof Audience) {
                return (Audience) out;
            }
            first = false;
            if (out instanceof ServerPlayerEntity) {
                players.add((ServerPlayerEntity) out);
            } else if (out instanceof Audience) {
                audiences.add((Audience) out);
            } else {
                others.add(out);
            }
        }
        return new MultiAudience(players.build(), audiences.build(), others.build());
    }


    /**
     * Return an Audience that sends to all operators with at least the level {@link
     * MinecraftServer#getOpPermissionLevel()}, including the server console and a potential RCON
     * client
     *
     * @return An audience that targets all operators on the server
     */
    public static Audience operators() {
        final @Nullable MinecraftServer server = TextAdapter.server();
        return operators(server == null ? 4 : server.getOpPermissionLevel());
    }

    /**
     * An audience targeting all operators with at least the provided level
     *
     * @param level The operator level
     * @return A new audience
     */
    public static Audience operators(int level) {
        return new OnlinePlayersAudience(ply -> ply.hasPermissionLevel(level), ImmutableSet.of(console()), ImmutableSet.of());
    }

    public static Audience withPermission(Identifier permission) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static final class OnlinePlayersAudience implements FabricAudience {
        private final Predicate<ServerPlayerEntity> playerFilter;
        private final Set<Audience> others;
        private final Set<CommandOutput> unknownOthers;

        OnlinePlayersAudience(Predicate<ServerPlayerEntity> playerFilter, Set<Audience> others, Set<CommandOutput> unknownOthers) {
            this.playerFilter = playerFilter;
            this.others = others;
            this.unknownOthers = unknownOthers;
        }

        private static Iterable<ServerPlayerEntity> getOnlinePlayers() {
            @Nullable MinecraftServer server = TextAdapter.server();
            return server == null ? ImmutableList.of() : server.getPlayerManager().getPlayerList();
        }

        @Override
        public void showBossBar(@NonNull BossBar bar) {

        }

        @Override
        public void hideBossBar(@NonNull BossBar bar) {

        }

        @Override
        public void playSound(@NonNull Sound sound) {

        }

        @Override
        public void stopSound(@NonNull SoundStop stop) {

        }

        @Override
        public void showTitle(@NonNull final Title title) {

        }

        @Override
        public void clearTitle() {

        }

        @Override
        public void resetTitle() {

        }

        @Override
        public void sendMessage(MessageType type, Component text, UUID source) {
            if (type == MessageType.GAME_INFO) {
                showTitle(TitleS2CPacket.Action.ACTIONBAR, text);
                return;
            }

            Iterator<ServerPlayerEntity> it = getOnlinePlayers().iterator();
            if (it.hasNext()) {
                GameMessageS2CPacket pkt = new GameMessageS2CPacket(TextAdapter.adapt(text), type, source);
                while (it.hasNext()) {
                    ServerPlayerEntity player = it.next();
                    if (playerFilter.test(player)) {
                        player.networkHandler.sendPacket(pkt);
                    }
                }
            }
            others.forEach(aud -> {
                if (aud instanceof FabricAudience) {
                    ((FabricAudience) aud).sendMessage(MessageType.SYSTEM, text);
                } else {
                    aud.sendMessage(text);
                }
            });
        }

        @Override
        public void showTitle(TitleS2CPacket.Action field, Component text) {
            Iterator<ServerPlayerEntity> it = getOnlinePlayers().iterator();
            if (it.hasNext()) {
                TitleS2CPacket pkt = new TitleS2CPacket(field, TextAdapter.adapt(text));
                while (it.hasNext()) {
                    ServerPlayerEntity player = it.next();
                    if (playerFilter.test(player)) {
                        player.networkHandler.sendPacket(pkt);
                    }
                }
            }
            if (field == TitleS2CPacket.Action.ACTIONBAR) {
                others.forEach(aud -> aud.sendActionBar(text));
            }
        }
    }

    static final class MultiAudience implements FabricAudience {
        private final Set<ServerPlayerEntity> players;
        private final Set<Audience> audiences;
        private final Set<CommandOutput> others;

        MultiAudience(Set<ServerPlayerEntity> players, Set<Audience> audiences, Set<CommandOutput> others) {
            this.players = ImmutableSet.copyOf(players);
            this.audiences = ImmutableSet.copyOf(audiences);
            this.others = ImmutableSet.copyOf(others);
        }

        @Override
        public void showBossBar(@NonNull BossBar bar) {
            if (!players.isEmpty()) {
                ((AdventureBossBar) bar).addAll(this.players);
            }
            if (!audiences.isEmpty()) {
                audiences.forEach(aud -> aud.showBossBar(bar));
            }
        }

        @Override
        public void hideBossBar(@NonNull BossBar bar) {
            if (!players.isEmpty()) {
                ((AdventureBossBar) bar).removeAll(this.players);
            }
            if (!audiences.isEmpty()) {
                audiences.forEach(it -> it.hideBossBar(bar));
            }
        }

        @Override
        public void playSound(@NonNull Sound sound) {
            if (!players.isEmpty()) {
                // Sounds are played at each player's location, so we can't re-use the packet
                players.forEach(it -> ((Audience) it).playSound(sound));
            }
            if (!audiences.isEmpty()) {
                audiences.forEach(it -> it.playSound(sound));
            }
        }

        @Override
        public void stopSound(@NonNull SoundStop stop) {
            if (!players.isEmpty()) {
                Sound.Source src = stop.source();
                StopSoundS2CPacket pkt = new StopSoundS2CPacket(TextAdapter.adapt(stop.sound()),
                        src == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(src));
                players.forEach(it -> it.networkHandler.sendPacket(pkt));
            }

            if (!audiences.isEmpty()) {
                audiences.forEach(it -> it.stopSound(stop));
            }
        }

        @Override
        public void showTitle(@NonNull final Title title) {

        }

        @Override
        public void clearTitle() {

        }

        @Override
        public void resetTitle() {

        }

        @Override
        public void sendMessage(MessageType type, Component text, UUID source) {
            if (type == MessageType.GAME_INFO) {
                showTitle(TitleS2CPacket.Action.ACTIONBAR, text);
                return;
            }
            @MonotonicNonNull Text mcText = null;

            if (!players.isEmpty()) {
                mcText = TextAdapter.adapt(text);
                GameMessageS2CPacket pkt = new GameMessageS2CPacket(TextAdapter.adapt(text), type, source);
                for (ServerPlayerEntity ply : players) {
                    ply.networkHandler.sendPacket(pkt);
                }
            }
            if (!audiences.isEmpty()) {
                audiences.forEach(it -> {
                    if (it instanceof FabricAudience) {
                        ((FabricAudience) it).sendMessage(type, text, source);
                    } else {
                        it.sendMessage(text);
                    }
                });
            }
            if (!others.isEmpty()) {
                if (mcText == null) {
                    mcText = TextAdapter.adapt(text);
                }
                final Text finalMcText = mcText;
                others.forEach(it -> it.sendSystemMessage(finalMcText, source));
            }
        }

        @Override
        public void showTitle(TitleS2CPacket.Action field, Component text) {
            if (!players.isEmpty()) {
                TitleS2CPacket pkt = new TitleS2CPacket(field, TextAdapter.adapt(text));
                for (ServerPlayerEntity ply : players) {
                    ply.networkHandler.sendPacket(pkt);
                }
            }
            if (!audiences.isEmpty()) {
                audiences.forEach(it -> {
                    if (it instanceof FabricAudience) {
                        ((FabricAudience) it).showTitle(field, text);
                    } else if (field == TitleS2CPacket.Action.ACTIONBAR) {
                        it.sendActionBar(text);
                    }
                });
            }
        }
    }
}
