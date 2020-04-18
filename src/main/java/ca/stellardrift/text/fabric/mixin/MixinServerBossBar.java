package ca.stellardrift.text.fabric.mixin;

import ca.stellardrift.text.fabric.GameEnums;
import ca.stellardrift.text.fabric.MinecraftTextSerializer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ServerBossBar.class)
public abstract class MixinServerBossBar extends net.minecraft.entity.boss.BossBar implements BossBar {

    @Shadow public abstract void setName(Text text);

    @Shadow public abstract void setPercent(float f);

    public MixinServerBossBar(Text text, net.minecraft.entity.boss.BossBar.Color color, Style style) {
        super(UUID.randomUUID(), text, color, style);
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
    public boolean darkenScreen() {
        return getDarkenSky();
    }

    @Override
    public @NonNull BossBar darkenScreen(boolean darkenScreen) {
        setDarkenSky(darkenScreen);
        return this;
    }

    @Override
    public boolean playBossMusic() {
        return this.dragonMusic;
    }

    @Override
    public @NonNull BossBar playBossMusic(boolean playBossMusic) {
        this.setDragonMusic(playBossMusic);
        return this;
    }

    @Override
    public boolean createWorldFog() {
        return getThickenFog();
    }

    @Override
    public @NonNull BossBar createWorldFog(boolean createWorldFog) {
        setThickenFog(createWorldFog);
        return this;
    }
}
