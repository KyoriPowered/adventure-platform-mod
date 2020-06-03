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

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Objects.requireNonNull;


public class FabricBossBarListener implements BossBar.Listener {
    public static final FabricBossBarListener INSTANCE = new FabricBossBarListener();
    private final Map<BossBar, ServerBossBar> bars = new IdentityHashMap<>();

    @Override
    public void bossBarNameChanged(@NonNull final BossBar bar, @NonNull final Component oldName, @NonNull final Component newName) {
        if(!oldName.equals(newName)) {
            minecraft(bar).setName(TextAdapter.adapt(newName));
        }
    }

    @Override
    public void bossBarPercentChanged(@NonNull final BossBar bar, final float oldPercent, final float newPercent) {
        if(oldPercent != newPercent) {
            minecraft(bar).setPercent(newPercent);
        }
    }

    @Override
    public void bossBarColorChanged(@NonNull final BossBar bar, final BossBar.@NonNull Color oldColor, final BossBar.@NonNull Color newColor) {
        if(oldColor != newColor) {
            minecraft(bar).setColor(GameEnums.BOSS_BAR_COLOR.toMinecraft(newColor));
        }
    }

    @Override
    public void bossBarOverlayChanged(@NonNull final BossBar bar, final BossBar.@NonNull Overlay oldOverlay, final BossBar.@NonNull Overlay newOverlay) {
        if(oldOverlay != newOverlay) {
            minecraft(bar).setOverlay(GameEnums.BOSS_BAR_OVERLAY.toMinecraft(newOverlay));
        }
    }

    @Override
    public void bossBarFlagsChanged(@NonNull final BossBar bar, @NonNull final Set<BossBar.Flag> oldFlags, @NonNull final Set<BossBar.Flag> newFlags) {
        if(!oldFlags.equals(newFlags)) {
            updateFlags(minecraft(bar), newFlags);
        }
    }

    private static void updateFlags(ServerBossBar bar, Set<BossBar.Flag> flags) {
        bar.setThickenFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
        bar.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
        bar.setDragonMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    }

    private ServerBossBar minecraft(BossBar bar) {
        final @Nullable ServerBossBar mc = this.bars.get(bar);
        if (mc == null) {
            throw new IllegalArgumentException("Unknown boss bar instance " + bar);
        }
        return mc;
    }

    private ServerBossBar minecraftCreating(BossBar bar) {
        return this.bars.computeIfAbsent(bar, key -> {
            final ServerBossBar ret = new ServerBossBar(TextAdapter.adapt(key.name()),
              GameEnums.BOSS_BAR_COLOR.toMinecraft(key.color()), GameEnums.BOSS_BAR_OVERLAY.toMinecraft(key.overlay()));
            ret.setPercent(key.percent());
            updateFlags(ret, key.flags());
            key.addListener(this);
            return ret;
        });
    }

    public void subscribe(final ServerPlayerEntity player, final BossBar bar) {
        minecraftCreating(requireNonNull(bar, "bar")).addPlayer(requireNonNull(player, "player"));
    }

    public void subscribeAll(final Collection<ServerPlayerEntity> players, final BossBar bar) {
        ((BulkServerBossBar) minecraftCreating(requireNonNull(bar, "bar"))).addAll(players);
    }

    public void unsubscribe(final ServerPlayerEntity player, final BossBar bar) {
        this.bars.computeIfPresent(bar, (key, old) -> {
            old.removePlayer(player);
            if (old.getPlayers().isEmpty()) {
                key.removeListener(this);
                return null;
            } else {
                return old;
            }
        });
    }

    public void unsubscribeAll(final Collection<ServerPlayerEntity> players, final BossBar bar) {
        this.bars.computeIfPresent(bar, (key, old) -> {
            ((BulkServerBossBar) old).removeAll(players);
            if (old.getPlayers().isEmpty()) {
                key.removeListener(this);
                return null;
            } else {
                return old;
            }
        });
    }

    /**
     * Replace a player entity without sending any packets.
     *
     * <p>This should be triggered when the entity representing a player changes
     * (such as during a respawn)
     *
     * @param old old player
     * @param newPlayer new one
     */
    public void replacePlayer(final ServerPlayerEntity old, ServerPlayerEntity newPlayer) {
        for (final ServerBossBar bar : this.bars.values()) {
            ((BulkServerBossBar) bar).replaceSubscriber(old, newPlayer);
        }
    }

    /**
     * Remove the player from all associated boss bars.
     *
     * @param player The player to remove
     */
    public void unsubscribeFromAll(final ServerPlayerEntity player) {
        for (Iterator<Map.Entry<BossBar, ServerBossBar>> it = this.bars.entrySet().iterator(); it.hasNext();) {
            final ServerBossBar bar = it.next().getValue();
            if (bar.getPlayers().contains(player)) {
                bar.removePlayer(player);
                if (bar.getPlayers().isEmpty()) {
                    it.remove();
                }
            }
        }
    }
}
