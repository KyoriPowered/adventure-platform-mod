package ca.stellardrift.text.fabric;

import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import java.util.Locale;

public class GameEnums {
    public static final MappedEnum<Formatting, TextColor> TEXT_COLOR
            = new MappedEnum<>(Formatting.class, Formatting::byName, Formatting::getName,
            TextColor.class, TextColor::toString, name -> TextColor.NAMES.value(name).orElse(null));

    public static final MappedEnum<HoverEvent.Action, net.kyori.adventure.text.event.HoverEvent.Action> HOVER_EVENT
            = new MappedEnum<>(HoverEvent.Action.class, HoverEvent.Action::byName, HoverEvent.Action::getName,
            net.kyori.adventure.text.event.HoverEvent.Action.class, net.kyori.adventure.text.event.HoverEvent.Action::toString, name -> net.kyori.adventure.text.event.HoverEvent.Action.NAMES.value(name).orElse(null));

    public static final MappedEnum<ClickEvent.Action, net.kyori.adventure.text.event.ClickEvent.Action> CLICK_EVENT
            = new MappedEnum<>(ClickEvent.Action.class, ClickEvent.Action::byName, ClickEvent.Action::getName,
            net.kyori.adventure.text.event.ClickEvent.Action.class, net.kyori.adventure.text.event.ClickEvent.Action::toString, name -> net.kyori.adventure.text.event.ClickEvent.Action.NAMES.value(name).orElse(null));

    public static final MappedEnum<BossBar.Color, net.kyori.adventure.bossbar.BossBar.Color> BOSS_BAR_COLOR
            = new MappedEnum<>(BossBar.Color.class, BossBar.Color::byName, BossBar.Color::getName,
            net.kyori.adventure.bossbar.BossBar.Color.class, it -> it.name().toLowerCase(Locale.ROOT), name -> net.kyori.adventure.bossbar.BossBar.Color.NAMES.value(name).orElse(null));

    public static final MappedEnum<BossBar.Style, Overlay> BOSS_BAR_OVERLAY
            = new MappedEnum<>(BossBar.Style.class, BossBar.Style::byName, BossBar.Style::getName,
            Overlay.class, it -> it.name().toLowerCase(Locale.ROOT), name -> Overlay.NAMES.value(name).orElse(null));

    public static final MappedEnum<SoundCategory, Sound.Source> SOUND_SOURCE
            = new MappedEnum<>(SoundCategory.class, null, SoundCategory::getName, // TODO: SoundCategory.byName?
            Sound.Source.class, it -> it.name().toLowerCase(Locale.ROOT), name -> Sound.Source.NAMES.value(name).orElse(null));

    private GameEnums() {
    }
}
