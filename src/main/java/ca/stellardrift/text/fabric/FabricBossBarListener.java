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
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.util.Objects.requireNonNull;


public class FabricBossBarListener implements BossBar.Listener {
    public static final FabricBossBarListener INSTANCE = new FabricBossBarListener();
    private final Map<BossBar, ServerBossBar> bars = new IdentityHashMap<>();

    @Override
    public void bossBarChanged(@NonNull final BossBar bar, @NonNull final Change change) {
        final ServerBossBar mc = this.bars.get(bar);
        if (mc == null) {
            throw new IllegalArgumentException("Unknown boss bar instance " + bar);
        }
        if (change == Change.NAME) {
            mc.setName(TextAdapter.adapt(bar.name()));
        } else if (change == Change.PERCENT) {
            mc.setPercent(bar.percent());
        } else if (change == Change.COLOR) {
            mc.setColor(GameEnums.BOSS_BAR_COLOR.toMinecraft(bar.color()));
        } else if (change == Change.OVERLAY) {
            mc.setOverlay(GameEnums.BOSS_BAR_OVERLAY.toMinecraft(bar.overlay()));
        } else if (change == Change.FLAGS) {
            updateFlags(mc, bar.flags());
        } else {
            throw new IllegalArgumentException("Unknown change " + change);
        }
    }

    private static void updateFlags(ServerBossBar bar, Set<BossBar.Flag> flags) {
        bar.setThickenFog(flags.contains(BossBar.Flag.CREATE_WORLD_FOG));
        bar.setDarkenSky(flags.contains(BossBar.Flag.DARKEN_SCREEN));
        bar.setDragonMusic(flags.contains(BossBar.Flag.PLAY_BOSS_MUSIC));
    }

    private ServerBossBar minecraft(BossBar bar) {
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
        minecraft(requireNonNull(bar, "bar")).addPlayer(requireNonNull(player, "player"));
    }

    public void subscribeAll(final Collection<ServerPlayerEntity> players, final BossBar bar) {
        ((BulkServerBossBar) minecraft(requireNonNull(bar, "bar"))).addAll(players);
    }

    public void unsubscribe(final ServerPlayerEntity player, final BossBar bar) {
        this.bars.computeIfPresent(bar, (key, old) -> {
            old.removePlayer(player);
            if (old.getPlayers().isEmpty()) {
                bar.removeListener(this);
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
                bar.removeListener(this);
                return null;
            } else {
                return old;
            }
        });
    }
}
