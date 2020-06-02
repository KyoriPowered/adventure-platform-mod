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

import ca.stellardrift.text.fabric.mixin.AccessorSoundCategory;
import java.util.Locale;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public class GameEnums {

    public static final MappedRegistry<BossBar.Color, net.kyori.adventure.bossbar.BossBar.Color> BOSS_BAR_COLOR
            = MappedRegistry.named(BossBar.Color.class, BossBar.Color::byName,
            net.kyori.adventure.bossbar.BossBar.Color.class, net.kyori.adventure.bossbar.BossBar.Color.NAMES);

    public static final MappedRegistry<BossBar.Style, Overlay> BOSS_BAR_OVERLAY
            = MappedRegistry.named(BossBar.Style.class, BossBar.Style::byName,
            Overlay.class, Overlay.NAMES);

    public static final MappedRegistry<SoundCategory, Sound.Source> SOUND_SOURCE
            = MappedRegistry.named(SoundCategory.class, key -> AccessorSoundCategory.getNameMap().get(key),
            Sound.Source.class, Sound.Source.NAMES);

    private GameEnums() {
    }
}
