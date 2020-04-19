package ca.stellardrift.text.fabric;

import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

public class GameEnums {
    public static final MappedEnum<Formatting, TextColor> TEXT_COLOR
            = MappedEnum.named(Formatting.class, Formatting::byName, Formatting::getName,
            TextColor.class, TextColor.NAMES);

    public static final MappedEnum<HoverEvent.Action, net.kyori.adventure.text.event.HoverEvent.Action> HOVER_EVENT
            = MappedEnum.named(HoverEvent.Action.class, HoverEvent.Action::byName, HoverEvent.Action::getName,
            net.kyori.adventure.text.event.HoverEvent.Action.class, net.kyori.adventure.text.event.HoverEvent.Action.NAMES);

    public static final MappedEnum<ClickEvent.Action, net.kyori.adventure.text.event.ClickEvent.Action> CLICK_EVENT
            = MappedEnum.named(ClickEvent.Action.class, ClickEvent.Action::byName, ClickEvent.Action::getName,
            net.kyori.adventure.text.event.ClickEvent.Action.class, net.kyori.adventure.text.event.ClickEvent.Action.NAMES);

    public static final MappedEnum<BossBar.Color, net.kyori.adventure.bossbar.BossBar.Color> BOSS_BAR_COLOR
            = MappedEnum.named(BossBar.Color.class, BossBar.Color::byName, BossBar.Color::getName,
            net.kyori.adventure.bossbar.BossBar.Color.class, net.kyori.adventure.bossbar.BossBar.Color.NAMES);

    public static final MappedEnum<BossBar.Style, Overlay> BOSS_BAR_OVERLAY
            = MappedEnum.named(BossBar.Style.class, BossBar.Style::byName, BossBar.Style::getName,
            Overlay.class, Overlay.NAMES);

    public static final MappedEnum<SoundCategory, Sound.Source> SOUND_SOURCE
            = MappedEnum.named(SoundCategory.class, SoundCategory::valueOf, SoundCategory::getName,
            Sound.Source.class, Sound.Source.NAMES);

    private GameEnums() {
    }
}
